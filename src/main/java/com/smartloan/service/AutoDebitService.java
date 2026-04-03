package com.smartloan.service;

import com.smartloan.dto.AutoDebitDTO;
import com.smartloan.entity.*;
import com.smartloan.repository.AutoDebitScheduleRepository;
import com.smartloan.repository.LoanRepository;
import com.smartloan.repository.RepaymentScheduleRepository;
import com.smartloan.repository.WalletRepository;
import com.smartloan.repository.PlatformFeeRepository;
import com.smartloan.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoDebitService {
    private static final double FEE_THRESHOLD = 500.0;
    private static final double FEE_RATE = 0.015; // 1.5%

    private final AutoDebitScheduleRepository autoDebitRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;
    private final PlatformFeeRepository platformFeeRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void enableAutoDebit(String loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (autoDebitRepository.findByLoanId(loanId).isPresent()) {
            throw new RuntimeException("Auto-debit already enabled for this loan");
        }

        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNo(loanId);
        RepaymentSchedule firstSchedule = schedules.stream()
            .filter(s -> !s.getIsPaid())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No pending payments for this loan"));

        AutoDebitSchedule autoDebit = AutoDebitSchedule.builder()
            .loanId(loanId)
            .scheduleId(firstSchedule.getId())
            .amount(firstSchedule.getAmountDue())
            .nextDebitDate(firstSchedule.getDueDate())
            .status(AutoDebitStatus.ACTIVE)
            .build();

        autoDebitRepository.save(autoDebit);
        loan.setAutoDebit(true);
        loanRepository.save(loan);

        notificationService.createNotification(
            loan.getBorrowerId(),
            "Auto-Debit Enabled",
            "Auto-debit enabled. Payments will be deducted automatically on due dates.",
            NotificationType.ALERT,
            loanId
        );
    }

    @Transactional
    public void disableAutoDebit(String loanId) {
        AutoDebitSchedule schedule = autoDebitRepository.findByLoanId(loanId)
            .orElseThrow(() -> new RuntimeException("Auto-debit not found for this loan"));

        schedule.setStatus(AutoDebitStatus.PAUSED);
        autoDebitRepository.save(schedule);

        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setAutoDebit(false);
        loanRepository.save(loan);

        notificationService.createNotification(
            loan.getBorrowerId(),
            "Auto-Debit Disabled",
            "Auto-debit has been disabled for your loan.",
            NotificationType.WARNING,
            loanId
        );
    }

    // Run daily at 00:01:00 — 6-field cron: seconds minutes hours day-of-month month day-of-week
    @Scheduled(cron = "0 1 0 * * *")
    public void processAutoDebits() {
        log.info("Starting auto-debit processing");
        List<AutoDebitSchedule> dueDebits = autoDebitRepository.findDueAutoDebits(AutoDebitStatus.ACTIVE);

        for (AutoDebitSchedule debit : dueDebits) {
            try {
                executeAutoDebit(debit);
            } catch (Exception e) {
                log.error("Failed to process auto-debit {}: {}", debit.getId(), e.getMessage());
                handleAutoDebitFailure(debit, e.getMessage());
            }
        }
    }

    private void executeAutoDebit(AutoDebitSchedule debit) {
        Loan loan = debit.getLoan();
        RepaymentSchedule schedule = repaymentScheduleRepository.findById(debit.getScheduleId())
            .orElseThrow(() -> new RuntimeException("Schedule not found"));

        Wallet borrowerWallet = walletRepository.findByUserId(loan.getBorrowerId())
            .orElseThrow(() -> new RuntimeException("Borrower wallet not found"));

        if (borrowerWallet.getBalance() < debit.getAmount()) {
            throw new RuntimeException("Insufficient funds");
        }

        // Calculate platform fee for transactions above threshold
        double platformFee = 0.0;
        if (debit.getAmount() > FEE_THRESHOLD) {
            platformFee = Math.round(debit.getAmount() * FEE_RATE * 100.0) / 100.0;
            log.info("Platform fee of ${} applied to auto-debit of ${} on loan {}", platformFee, debit.getAmount(), loan.getId());
        }

        // Transfer full amount from borrower to lender
        walletService.payLoan(loan.getBorrowerId(), loan.getLenderId(), loan.getId(), debit.getAmount());

        // Deduct fee from lender's wallet
        if (platformFee > 0) {
            walletService.deductFromWallet(loan.getLenderId(), platformFee, "Platform fee for auto-debit payment", loan.getId());
        }

        // Create payment record
        LocalDate paymentDate = LocalDate.now();
        Payment payment = Payment.builder()
                .loanId(loan.getId())
                .paidBy(loan.getBorrowerId())
                .scheduleId(debit.getScheduleId())
                .amount(debit.getAmount())
                .paymentDate(paymentDate)
                .note("Auto-Debit")
                .type(PaymentType.AUTO_DEBIT)
                .build();
        payment = paymentRepository.save(payment);

        // Record platform fee if applicable
        if (platformFee > 0) {
            PlatformFee fee = PlatformFee.builder()
                    .loanId(loan.getId())
                    .paymentId(payment.getId())
                    .transactionAmount(debit.getAmount())
                    .feeRate(FEE_RATE)
                    .feeAmount(platformFee)
                    .payerId(loan.getLenderId())
                    .recipientId(null)
                    .build();
            platformFeeRepository.save(fee);
        }

        schedule.setIsPaid(true);
        schedule.setPaidAt(LocalDateTime.now());
        repaymentScheduleRepository.save(schedule);

        List<RepaymentSchedule> allSchedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNo(loan.getId());
        RepaymentSchedule nextSchedule = allSchedules.stream()
            .filter(s -> !s.getIsPaid())
            .findFirst()
            .orElse(null);

        if (nextSchedule != null) {
            debit.setNextDebitDate(nextSchedule.getDueDate());
            debit.setScheduleId(nextSchedule.getId());
            debit.setAmount(nextSchedule.getAmountDue());
            debit.setFailureCount(0);
        } else {
            debit.setStatus(AutoDebitStatus.COMPLETED);
            loan.setAutoDebit(false);
            loanRepository.save(loan);
        }

        debit.setLastAttempt(LocalDateTime.now());
        autoDebitRepository.save(debit);

        // Send notification to lender about payment received
        String feeNotice = platformFee > 0
                ? String.format(" (Platform fee: $%.2f charged to your wallet)", platformFee)
                : "";
        notificationService.createNotification(
            loan.getLenderId(),
            "Auto-Debit Payment Received",
            "Auto-debit payment of $" + debit.getAmount() + " received on your loan." + feeNotice,
            NotificationType.PAYMENT_RECEIVED,
            loan.getId()
        );

        // Send notification to borrower about payment completed
        notificationService.createNotification(
            loan.getBorrowerId(),
            "Auto-Debit Payment Completed",
            "Auto-debit payment of $" + debit.getAmount() + " completed",
            NotificationType.PAYMENT_RECEIVED,
            loan.getId()
        );
    }

    private void handleAutoDebitFailure(AutoDebitSchedule debit, String reason) {
        debit.setFailureCount(debit.getFailureCount() + 1);
        debit.setLastFailureReason(reason);
        debit.setLastAttempt(LocalDateTime.now());

        if (debit.getFailureCount() >= 3) {
            debit.setStatus(AutoDebitStatus.FAILED);
            Loan loan = debit.getLoan();
            loan.setAutoDebit(false);
            loanRepository.save(loan);
        }

        autoDebitRepository.save(debit);
    }

    public AutoDebitDTO getAutoDebitStatus(String loanId) {
        return autoDebitRepository.findByLoanId(loanId)
            .map(this::mapToDTO)
            .orElse(null);
    }

    private AutoDebitDTO mapToDTO(AutoDebitSchedule debit) {
        return AutoDebitDTO.builder()
            .id(debit.getId())
            .loanId(debit.getLoanId())
            .amount(debit.getAmount())
            .nextDebitDate(debit.getNextDebitDate())
            .status(debit.getStatus())
            .failureCount(debit.getFailureCount())
            .lastAttempt(debit.getLastAttempt())
            .lastFailureReason(debit.getLastFailureReason())
            .createdAt(debit.getCreatedAt())
            .build();
    }
}
