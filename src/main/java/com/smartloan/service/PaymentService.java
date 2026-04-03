package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    // Platform fee configuration
    private static final double FEE_THRESHOLD = 500.0;
    private static final double FEE_RATE = 0.015; // 1.5%

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PlatformFeeRepository platformFeeRepository;

    @Transactional
    public PaymentDTO logPayment(String loanId, LogPaymentRequest request, User user) {
        Loan loan = loanRepository.findByIdWithUsers(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender or borrower can log payment
        if (!loan.getLenderId().equals(user.getId()) && !loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        LocalDate paymentDate = request.getPaymentDate() != null
                ? LocalDate.parse(request.getPaymentDate())
                : LocalDate.now();

        Payment payment = Payment.builder()
                .loanId(loanId)
                .paidBy(user.getId())
                .scheduleId(request.getScheduleId())
                .amount(request.getAmount())
                .paymentDate(paymentDate)
                .note(request.getNote())
                .build();

        payment = paymentRepository.save(payment);

        // Mark schedule as paid if linked
        if (request.getScheduleId() != null) {
            RepaymentSchedule schedule = scheduleRepository.findById(request.getScheduleId()).orElse(null);
            if (schedule != null) {
                schedule.setIsPaid(true);
                schedule.setPaidAt(LocalDateTime.now());
                scheduleRepository.save(schedule);
            }
        }

        // Note: Manually logging an offline payment does NOT automatically
        // complete the loan. Only valid wallet transactions (makePayment, autoDebit)
        // are allowed to transition the loan to COMPLETED status to prevent exploitation.

        return toPaymentDTO(payment);
    }

    public List<PaymentDTO> getPayments(String loanId, User user) {
        Loan loan = loanRepository.findByIdWithUsers(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender or borrower can view payments
        if (!loan.getLenderId().equals(user.getId()) && !loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(this::toPaymentDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDTO deletePayment(String loanId, String paymentId, User user) {
        Loan loan = loanRepository.findByIdWithUsers(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender can delete payments
        if (!loan.getLenderId().equals(user.getId())) {
            throw new RuntimeException("Only the lender can delete payments");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getLoanId().equals(loanId)) {
            throw new RuntimeException("Payment does not belong to this loan");
        }

        // Unmark schedule if linked
        if (payment.getScheduleId() != null) {
            RepaymentSchedule schedule = scheduleRepository.findById(payment.getScheduleId()).orElse(null);
            if (schedule != null) {
                schedule.setIsPaid(false);
                schedule.setPaidAt(null);
                scheduleRepository.save(schedule);
            }
        }

        paymentRepository.delete(payment);

        // Update loan status if necessary
        if (loan.getStatus() == LoanStatus.COMPLETED) {
            loan.setStatus(LoanStatus.ACTIVE);
            loanRepository.save(loan);
        }

        return toPaymentDTO(payment);
    }

    @Transactional
    public PaymentDTO makePayment(String loanId, MakePaymentRequest request, User user) {
        Loan loan = loanRepository.findByIdWithUsers(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only borrower can make payment
        if (!loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Only the borrower can make payments");
        }

        // Validate amount
        if (request.getAmount() <= 0) {
            throw new RuntimeException("Payment amount must be greater than 0");
        }

        // Calculate platform fee for transactions above threshold
        double platformFee = 0.0;
        if (request.getAmount() > FEE_THRESHOLD) {
            platformFee = Math.round(request.getAmount() * FEE_RATE * 100.0) / 100.0;
            log.info("Platform fee of ${} applied to payment of ${} on loan {}", platformFee, request.getAmount(), loanId);
        }

        // Transfer full amount from borrower to lender
        walletService.payLoan(user.getId(), loan.getLenderId(), loanId, request.getAmount());

        // Deduct fee from lender's wallet
        if (platformFee > 0) {
            walletService.deductFromWallet(loan.getLenderId(), platformFee, "Platform fee for loan payment", loanId);
        }

        // Create payment record (full amount paid by borrower)
        LocalDate paymentDate = LocalDate.now();
        Payment payment = Payment.builder()
                .loanId(loanId)
                .paidBy(user.getId())
                .scheduleId(request.getScheduleId())
                .amount(request.getAmount())
                .paymentDate(paymentDate)
                .note(request.getNote())
                .type(PaymentType.MANUAL)
                .build();

        payment = paymentRepository.save(payment);

        // Record platform fee if applicable
        if (platformFee > 0) {
            PlatformFee fee = PlatformFee.builder()
                    .loanId(loanId)
                    .paymentId(payment.getId())
                    .transactionAmount(request.getAmount())
                    .feeRate(FEE_RATE)
                    .feeAmount(platformFee)
                    .payerId(loan.getLenderId())
                    .recipientId(null)
                    .build();
            platformFeeRepository.save(fee);
        }

        // Mark schedule as paid if linked and belongs to this loan
        if (request.getScheduleId() != null) {
            RepaymentSchedule schedule = scheduleRepository.findById(request.getScheduleId()).orElse(null);
            if (schedule != null && schedule.getLoanId().equals(loanId)) {
                schedule.setIsPaid(true);
                schedule.setPaidAt(LocalDateTime.now());
                scheduleRepository.save(schedule);
            }
        }

        // Check if loan is fully paid
        Double totalPaid = paymentRepository.getTotalPaidForLoan(loanId);
        if (totalPaid != null && totalPaid >= loan.getTotalAmount()) {
            loan.setStatus(LoanStatus.COMPLETED);
            loanRepository.save(loan);
        }

        // Send notification to lender
        String feeNotice = platformFee > 0
                ? String.format(" (Platform fee: $%.2f charged to your wallet)", platformFee)
                : "";
        notificationService.createNotification(
                loan.getLenderId(),
                "Payment Received",
                user.getName() + " paid $" + request.getAmount() + " on your loan." + feeNotice,
                NotificationType.PAYMENT_RECEIVED,
                loanId
        );

        return toPaymentDTO(payment);
    }

    @Transactional
    public PaymentDTO initiateAutoDebit(String loanId, MakePaymentRequest request, User user) {
        Loan loan = loanRepository.findByIdWithUsers(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender can initiate auto-debit
        if (!loan.getLenderId().equals(user.getId())) {
            throw new RuntimeException("Only the lender can initiate auto-debit");
        }

        // Validate amount
        if (request.getAmount() <= 0) {
            throw new RuntimeException("Payment amount must be greater than 0");
        }

        // Get borrower
        User borrower = userRepository.findById(loan.getBorrowerId())
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        // Calculate platform fee for transactions above threshold
        double platformFee = 0.0;
        if (request.getAmount() > FEE_THRESHOLD) {
            platformFee = Math.round(request.getAmount() * FEE_RATE * 100.0) / 100.0;
            log.info("Platform fee of ${} applied to auto-debit of ${} on loan {}", platformFee, request.getAmount(), loanId);
        }

        // Transfer full amount from borrower to lender
        walletService.payLoan(borrower.getId(), user.getId(), loanId, request.getAmount());

        // Deduct fee from lender's wallet
        if (platformFee > 0) {
            walletService.deductFromWallet(user.getId(), platformFee, "Platform fee for auto-debit payment", loanId);
        }

        // Create payment record
        LocalDate paymentDate = LocalDate.now();
        Payment payment = Payment.builder()
                .loanId(loanId)
                .paidBy(borrower.getId())
                .scheduleId(request.getScheduleId())
                .amount(request.getAmount())
                .paymentDate(paymentDate)
                .note(request.getNote() != null ? "Auto-Debit: " + request.getNote() : "Auto-Debit")
                .type(PaymentType.AUTO_DEBIT)
                .build();

        payment = paymentRepository.save(payment);

        // Record platform fee if applicable
        if (platformFee > 0) {
            PlatformFee fee = PlatformFee.builder()
                    .loanId(loanId)
                    .paymentId(payment.getId())
                    .transactionAmount(request.getAmount())
                    .feeRate(FEE_RATE)
                    .feeAmount(platformFee)
                    .payerId(user.getId())
                    .recipientId(null)
                    .build();
            platformFeeRepository.save(fee);
        }

        // Mark schedule as paid if linked and belongs to this loan
        if (request.getScheduleId() != null) {
            RepaymentSchedule schedule = scheduleRepository.findById(request.getScheduleId()).orElse(null);
            if (schedule != null && schedule.getLoanId().equals(loanId)) {
                schedule.setIsPaid(true);
                schedule.setPaidAt(LocalDateTime.now());
                scheduleRepository.save(schedule);
            }
        }

        // Check if loan is fully paid
        Double totalPaid = paymentRepository.getTotalPaidForLoan(loanId);
        if (totalPaid != null && totalPaid >= loan.getTotalAmount()) {
            loan.setStatus(LoanStatus.COMPLETED);
            loanRepository.save(loan);
        }

        // Send notification to borrower
        String feeNotice = platformFee > 0
                ? String.format(" (Platform fee: $%.2f charged to lender's wallet)", platformFee)
                : "";
        notificationService.createNotification(
                borrower.getId(),
                "Auto-Debit Payment",
                user.getName() + " initiated an auto-debit of $" + request.getAmount() + " on your loan." + feeNotice,
                NotificationType.PAYMENT_RECEIVED,
                loanId
        );

        return toPaymentDTO(payment);
    }


    private PaymentDTO toPaymentDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .loanId(payment.getLoanId())
                .paidBy(payment.getPaidBy())
                .scheduleId(payment.getScheduleId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate().toString())
                .note(payment.getNote())
                .type(payment.getType())
                .createdAt(payment.getCreatedAt().toString())
                .build();
    }
}
