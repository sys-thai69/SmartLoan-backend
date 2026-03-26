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

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;
    private final LoanTemplateRepository templateRepository;
    private final AuthService authService;

    @Transactional
    public LoanDTO createLoan(CreateLoanRequest request, User lender) {
        User borrower = userRepository.findByEmail(request.getBorrowerEmail())
                .orElseThrow(() -> new RuntimeException("Borrower not found with email: " + request.getBorrowerEmail()));

        if (borrower.getId().equals(lender.getId())) {
            throw new RuntimeException("Cannot lend to yourself");
        }

        double interestRate = request.getInterestRate() != null ? request.getInterestRate() : 0.0;
        double totalAmount = request.getPrincipal() * (1 + interestRate / 100);

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

        return toLoanDTO(loanRepository.findById(loan.getId()).orElse(loan));
    }

    @Transactional
    public LoanDTO quickLend(QuickLendRequest request, User lender) {
        User borrower = userRepository.findByEmail(request.getBorrowerEmail())
                .orElseThrow(() -> new RuntimeException("Borrower not found with email: " + request.getBorrowerEmail()));

        if (borrower.getId().equals(lender.getId())) {
            throw new RuntimeException("Cannot lend to yourself");
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

        return toLoanDTO(loanRepository.findById(loan.getId()).orElse(loan));
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
                .schedule(schedules.stream().map(this::toScheduleDTO).collect(Collectors.toList()))
                .payments(payments.stream().map(this::toPaymentDTO).collect(Collectors.toList()))
                .build();
    }

    public LoanWithBalanceDTO getLoanById(String id, User user) {
        Loan loan = loanRepository.findById(id)
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
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Only the borrower can accept this loan");
        }

        if (loan.getStatus() != LoanStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Loan is not pending acceptance");
        }

        loan.setStatus(LoanStatus.ACTIVE);
        return toLoanDTO(loanRepository.save(loan));
    }

    @Transactional
    public LoanDTO declineLoan(String id, User user) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Only the borrower can decline this loan");
        }

        if (loan.getStatus() != LoanStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Loan is not pending acceptance");
        }

        loan.setStatus(LoanStatus.DECLINED);
        return toLoanDTO(loanRepository.save(loan));
    }

    @Transactional
    public LoanDTO updateStatus(String id, String newStatus, User user) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Only lender or borrower can update status
        if (!loan.getLenderId().equals(user.getId()) && !loan.getBorrowerId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        loan.setStatus(LoanStatus.valueOf(newStatus.toUpperCase()));
        return toLoanDTO(loanRepository.save(loan));
    }

    public List<RepaymentScheduleDTO> getOverdueSchedules(User user) {
        return scheduleRepository.findOverdueByUserId(user.getId(), LocalDate.now())
                .stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());
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
