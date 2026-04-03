package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    // Minimum trust score required to receive loans
    private static final double MIN_TRUST_SCORE = 50.0;

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;
    private final LoanTemplateRepository templateRepository;
    private final AuthService authService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final UserService userService;

    @Transactional
    public LoanDTO createLoan(CreateLoanRequest request, User lender) {
        User borrower = userService.findUserByFlexibleInput(request.getBorrowerEmail());

        if (borrower.getId().equals(lender.getId())) {
            throw new RuntimeException("Cannot lend to yourself");
        }

        // Trust score enforcement — reject borrowers with low trust
        if (borrower.getTrustScore() < MIN_TRUST_SCORE) {
            throw new RuntimeException(
                    "Cannot create loan: " + borrower.getName() + " has a trust score of " +
                    borrower.getTrustScore().intValue() + " (minimum required: " + (int) MIN_TRUST_SCORE + "). " +
                    "They need to improve their trust score by repaying existing loans on time.");
        }

        double interestRate = request.getInterestRate() != null ? request.getInterestRate() : 0.0;
        double totalAmount = request.getPrincipal() * (1 + interestRate / 100);

        // Deduct principal from lender's wallet immediately (only the principal, not total with interest)
        walletService.deductFromWallet(lender.getId(), request.getPrincipal(), "Loan issued - Principal reserved", null);

        Loan loan = Loan.builder()
                .lenderId(lender.getId())
                .borrowerId(borrower.getId())
                .principal(request.getPrincipal())
                .interestRate(interestRate)
                .totalAmount(totalAmount)
                .installments(request.getInstallments())
                .frequency(request.getFrequency())
                .startDate(LocalDate.parse(request.getStartDate()))
                .autoDebit(request.getAutoDebit() != null && request.getAutoDebit())
                .templateId(request.getTemplateId())
                .build();

        loan = loanRepository.save(loan);

        // Create repayment schedule
        createRepaymentSchedule(loan);

        // Notify borrower about new loan request
        notificationService.createNotification(
                borrower.getId(),
                "New Loan Request",
                "You have received a new loan request from " + lender.getName() + " for $" + request.getPrincipal(),
                NotificationType.LOAN_REQUEST,
                loan.getId()
        );

        return toLoanDTO(loanRepository.findByIdWithUsers(loan.getId()).orElse(loan));
    }

    @Transactional
    public LoanDTO quickLend(QuickLendRequest request, User lender) {
        User borrower = userService.findUserByFlexibleInput(request.getBorrowerEmail());

        if (borrower.getId().equals(lender.getId())) {
            throw new RuntimeException("Cannot lend to yourself");
        }

        // Trust score enforcement — reject borrowers with low trust
        if (borrower.getTrustScore() < MIN_TRUST_SCORE) {
            throw new RuntimeException(
                    "Cannot create loan: " + borrower.getName() + " has a trust score of " +
                    borrower.getTrustScore().intValue() + " (minimum required: " + (int) MIN_TRUST_SCORE + "). " +
                    "They need to improve their trust score by repaying existing loans on time.");
        }

        // Default values or from template
        double interestRate = 0.0;
        int installments = 1;
        LoanFrequency frequency = LoanFrequency.MONTHLY;
        boolean autoDebit = false;

        if (request.getTemplateId() != null) {
            LoanTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found"));
            interestRate = template.getInterestRate();
            installments = template.getInstallments();
            frequency = template.getFrequency();
            autoDebit = template.getAutoDebit();
        }

        double totalAmount = request.getAmount() * (1 + interestRate / 100);

        // Deduct amount from lender's wallet immediately
        walletService.deductFromWallet(lender.getId(), request.getAmount(), "Quick loan issued - Principal reserved", null);

        Loan loan = Loan.builder()
                .lenderId(lender.getId())
                .borrowerId(borrower.getId())
                .principal(request.getAmount())
                .interestRate(interestRate)
                .totalAmount(totalAmount)
                .installments(installments)
                .frequency(frequency)
                .startDate(LocalDate.now())
                .autoDebit(autoDebit)
                .isQuickLend(true)
                .templateId(request.getTemplateId())
                .build();

        loan = loanRepository.save(loan);

        // Create repayment schedule
        createRepaymentSchedule(loan);

        // Notify borrower about new quick loan
        notificationService.createNotification(
                borrower.getId(),
                "New Quick Loan",
                "You have received a quick loan from " + lender.getName() + " for $" + request.getAmount(),
                NotificationType.LOAN_REQUEST,
                loan.getId()
        );

        return toLoanDTO(loanRepository.findByIdWithUsers(loan.getId()).orElse(loan));
    }

    private void createRepaymentSchedule(Loan loan) {
        double amountPerInstallment = loan.getTotalAmount() / loan.getInstallments();
        LocalDate dueDate = loan.getStartDate();

        List<RepaymentSchedule> schedules = new ArrayList<>();
        for (int i = 1; i <= loan.getInstallments(); i++) {
            if (loan.getFrequency() == LoanFrequency.WEEKLY) {
                dueDate = dueDate.plusWeeks(1);
            } else {
                dueDate = dueDate.plusMonths(1);
            }

            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loanId(loan.getId())
                    .installmentNo(i)
                    .dueDate(dueDate)
                    .amountDue(amountPerInstallment)
                    .build();

            schedules.add(schedule);
        }

        scheduleRepository.saveAll(schedules);
    }

    public List<LoanDTO> getMyLoans(User user) {
        List<Loan> loans = loanRepository.findAllByUserId(user.getId());
        if (loans.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> loanIds = loans.stream().map(Loan::getId).collect(Collectors.toList());

        // Batch fetch schedules and payments (only 2 queries instead of 2N)
        Map<String, List<RepaymentSchedule>> schedulesByLoanId = scheduleRepository
                .findByLoanIdInOrderByInstallmentNo(loanIds)
                .stream()
                .collect(Collectors.groupingBy(RepaymentSchedule::getLoanId));

        Map<String, List<Payment>> paymentsByLoanId = paymentRepository
                .findByLoanIdInOrderByPaymentDateDesc(loanIds)
                .stream()
                .collect(Collectors.groupingBy(Payment::getLoanId));

        return loans.stream()
                .map(loan -> toLoanDTOWithPreloadedData(loan,
                        schedulesByLoanId.getOrDefault(loan.getId(), Collections.emptyList()),
                        paymentsByLoanId.getOrDefault(loan.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private LoanDTO toLoanDTOWithPreloadedData(Loan loan, List<RepaymentSchedule> schedules, List<Payment> payments) {
        return LoanDTO.builder()
                .id(loan.getId())
                .lenderId(loan.getLenderId())
                .borrowerId(loan.getBorrowerId())
                .lender(loan.getLender() != null ? authService.toUserDTO(loan.getLender()) : null)
                .borrower(loan.getBorrower() != null ? authService.toUserDTO(loan.getBorrower()) : null)
                .principal(loan.getPrincipal())
                .interestRate(loan.getInterestRate())
                .totalAmount(loan.getTotalAmount())
                .installments(loan.getInstallments())
                .frequency(loan.getFrequency())
                .startDate(loan.getStartDate().toString())
                .status(loan.getStatus())
                .autoDebit(loan.getAutoDebit())
                .isQuickLend(loan.getIsQuickLend())
                .templateId(loan.getTemplateId())
                .createdAt(loan.getCreatedAt().toString())
                .flagged(loan.getFlagged())
                .flagReason(loan.getFlagReason())
                .flaggedBy(loan.getFlaggedBy())
                .flaggedAt(loan.getFlaggedAt() != null ? loan.getFlaggedAt().toString() : null)
                .schedule(schedules.stream().map(this::toScheduleDTO).collect(Collectors.toList()))
                .payments(payments.stream().map(this::toPaymentDTO).collect(Collectors.toList()))
                .build();
    }

    public LoanWithBalanceDTO getLoanById(String id, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Check access
        if (!loan.getLenderId().equals(user.getId()) && !loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        Double totalPaid = paymentRepository.getTotalPaidForLoan(id);
        if (totalPaid == null) totalPaid = 0.0;

        double remaining = loan.getTotalAmount() - totalPaid;
        double percentPaid = (totalPaid / loan.getTotalAmount()) * 100;

        LoanDTO loanDTO = toLoanDTO(loan);

        return LoanWithBalanceDTO.builder()
                .loan(loanDTO)
                .totalPaid(totalPaid)
                .remaining(remaining)
                .percentPaid(percentPaid)
                .build();
    }

    @Transactional
    public LoanDTO acceptLoan(String id, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Only the borrower can accept this loan");
        }

        if (loan.getStatus() != LoanStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Loan is not pending acceptance");
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan = loanRepository.save(loan);

        // Transfer principal from lender to borrower (TRANSFER type transaction)
        walletService.acceptLoanTransfer(loan.getLenderId(), user.getId(), loan.getId(), loan.getPrincipal(), "Loan received - Principal transferred");

        // Send notification to lender
        notificationService.createNotification(
                loan.getLenderId(),
                "Loan Accepted",
                "Your loan of $" + loan.getPrincipal() + " to " + user.getName() + " has been accepted.",
                NotificationType.LOAN_ACCEPTED,
                loan.getId()
        );

        return toLoanDTO(loan);
    }

    @Transactional
    public LoanDTO declineLoan(String id, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Only the borrower can decline this loan");
        }

        if (loan.getStatus() != LoanStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Loan is not pending acceptance");
        }

        loan.setStatus(LoanStatus.DECLINED);
        loan = loanRepository.save(loan);

        // Refund the principal back to lender's wallet
        walletService.creditToWallet(loan.getLenderId(), loan.getPrincipal(), "Loan declined - Principal refunded", loan.getId());

        // Send notification to lender
        notificationService.createNotification(
                loan.getLenderId(),
                "Loan Declined",
                "Your loan of $" + loan.getPrincipal() + " to " + user.getName() + " has been declined. Principal has been refunded.",
                NotificationType.LOAN_DECLINED,
                loan.getId()
        );

        return toLoanDTO(loan);
    }

    @Transactional
    public LoanDTO cancelLoan(String id, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getLenderId().equals(user.getId())) {
            throw new RuntimeException("Only the lender can cancel this loan");
        }

        if (loan.getStatus() != LoanStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Loan can only be cancelled while pending acceptance");
        }

        loan.setStatus(LoanStatus.CANCELLED);
        loan = loanRepository.save(loan);

        // Refund the principal back to lender's wallet
        walletService.creditToWallet(loan.getLenderId(), loan.getPrincipal(), "Loan cancelled - Principal refunded", loan.getId());

        // Notify borrower the loan was cancelled
        notificationService.createNotification(
                loan.getBorrowerId(),
                "Loan Request Cancelled",
                "The lending request from " + user.getName() + " has been cancelled. No action needed.",
                NotificationType.LOAN_DECLINED,
                loan.getId()
        );

        return toLoanDTO(loan);
    }

    @Transactional
    public LoanDTO updateStatus(String id, String newStatus, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender or borrower can update status
        if (!loan.getLenderId().equals(user.getId()) && !loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        LoanStatus targetStatus;
        try {
            targetStatus = LoanStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid loan status: " + newStatus);
        }

        // Validate allowed status transitions
        LoanStatus currentStatus = loan.getStatus();
        boolean validTransition = switch (currentStatus) {
            case ACTIVE -> targetStatus == LoanStatus.OVERDUE || targetStatus == LoanStatus.COMPLETED;
            case OVERDUE -> targetStatus == LoanStatus.ACTIVE || targetStatus == LoanStatus.COMPLETED;
            default -> false;
        };

        if (!validTransition) {
            throw new RuntimeException("Cannot transition from " + currentStatus + " to " + targetStatus);
        }

        loan.setStatus(targetStatus);
        return toLoanDTO(loanRepository.save(loan));
    }

    public List<RepaymentScheduleDTO> getOverdueSchedules(User user) {
        return scheduleRepository.findOverdueByUserId(user.getId(), LocalDate.now())
                .stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void alertBorrower(String id, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender can alert borrower
        if (!loan.getLenderId().equals(user.getId())) {
            throw new RuntimeException("Only the lender can alert the borrower about this loan");
        }

        // Send notification to borrower
        notificationService.createNotification(
                loan.getBorrowerId(),
                "Overdue Payment Alert",
                "Your payment on loan from " + user.getName() + " is overdue. Please make payment immediately.",
                NotificationType.OVERDUE_ALERT,
                loan.getId()
        );
    }

    @Transactional
    public void sendDueReminder(String id, SendReminderRequest request, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender can send reminder
        if (!loan.getLenderId().equals(user.getId())) {
            throw new RuntimeException("Only the lender can send reminders");
        }

        if (request.getScheduleId() == null) {
            throw new RuntimeException("A schedule ID is required to send a due reminder");
        }

        RepaymentSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // Verify this schedule belongs to this loan
        if (!schedule.getLoanId().equals(id)) {
            throw new RuntimeException("Schedule does not belong to this loan");
        }

        notificationService.sendDueReminderNotification(
                loan.getBorrowerId(),
                user.getName(),
                loan.getId(),
                schedule.getAmountDue(),
                schedule.getDueDate().toString()
        );
    }

    @Transactional
    public LoanDTO reportLoan(String id, String reason, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender or borrower can report
        if (!loan.getLenderId().equals(user.getId()) && !loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Only the lender or borrower can report this loan");
        }

        loan.setFlagged(true);
        loan.setFlagReason(reason);
        loan.setFlaggedBy(user.getId());
        loan.setFlaggedAt(LocalDateTime.now());
        loan = loanRepository.save(loan);

        return toLoanDTO(loan);
    }

    @Transactional
    public void sendOverdueAlert(String id, User user) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender can send alert
        if (!loan.getLenderId().equals(user.getId())) {
            throw new RuntimeException("Only the lender can send overdue alerts");
        }

        // Find ALL unpaid overdue schedules and send a consolidated alert
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNo(id);
        boolean anyAlertSent = false;
        for (RepaymentSchedule schedule : schedules) {
            if (!schedule.getIsPaid() && schedule.getDueDate().isBefore(LocalDate.now())) {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(schedule.getDueDate(), LocalDate.now());

                notificationService.sendOverdueNotification(
                        loan.getBorrowerId(),
                        user.getName(),
                        loan.getId(),
                        schedule.getAmountDue(),
                        (int) daysOverdue
                );
                anyAlertSent = true;
            }
        }

        // Notify lender only once with a summary
        if (anyAlertSent) {
            notificationService.sendLenderOverdueNotification(
                    user.getId(),
                    loan.getBorrower().getName(),
                    loan.getId(),
                    schedules.stream()
                        .filter(s -> !s.getIsPaid() && s.getDueDate().isBefore(LocalDate.now()))
                        .mapToDouble(RepaymentSchedule::getAmountDue).sum(),
                    0 // summary notification; days not meaningful for multi-schedule
            );
        }
    }

    public LoanDTO toLoanDTO(Loan loan) {
        List<RepaymentScheduleDTO> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNo(loan.getId())
                .stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());

        List<PaymentDTO> payments = paymentRepository.findByLoanIdOrderByPaymentDateDesc(loan.getId())
                .stream()
                .map(this::toPaymentDTO)
                .collect(Collectors.toList());

        return LoanDTO.builder()
                .id(loan.getId())
                .lenderId(loan.getLenderId())
                .borrowerId(loan.getBorrowerId())
                .lender(loan.getLender() != null ? authService.toUserDTO(loan.getLender()) : null)
                .borrower(loan.getBorrower() != null ? authService.toUserDTO(loan.getBorrower()) : null)
                .principal(loan.getPrincipal())
                .interestRate(loan.getInterestRate())
                .totalAmount(loan.getTotalAmount())
                .installments(loan.getInstallments())
                .frequency(loan.getFrequency())
                .startDate(loan.getStartDate().toString())
                .status(loan.getStatus())
                .autoDebit(loan.getAutoDebit())
                .isQuickLend(loan.getIsQuickLend())
                .templateId(loan.getTemplateId())
                .createdAt(loan.getCreatedAt().toString())
                .flagged(loan.getFlagged())
                .flagReason(loan.getFlagReason())
                .flaggedBy(loan.getFlaggedBy())
                .flaggedAt(loan.getFlaggedAt() != null ? loan.getFlaggedAt().toString() : null)
                .schedule(schedules)
                .payments(payments)
                .build();
    }

    private RepaymentScheduleDTO toScheduleDTO(RepaymentSchedule schedule) {
        return RepaymentScheduleDTO.builder()
                .id(schedule.getId())
                .loanId(schedule.getLoanId())
                .installmentNo(schedule.getInstallmentNo())
                .dueDate(schedule.getDueDate().toString())
                .amountDue(schedule.getAmountDue())
                .isPaid(schedule.getIsPaid())
                .paidAt(schedule.getPaidAt() != null ? schedule.getPaidAt().toString() : null)
                .build();
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
