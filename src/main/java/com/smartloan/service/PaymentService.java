package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    @Transactional
    public PaymentDTO logPayment(String loanId, LogPaymentRequest request, User user) {
        Loan loan = loanRepository.findById(loanId)
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

        // Check if loan is fully paid
        Double totalPaid = paymentRepository.getTotalPaidForLoan(loanId);
        if (totalPaid != null && totalPaid >= loan.getTotalAmount()) {
            loan.setStatus(LoanStatus.COMPLETED);
            loanRepository.save(loan);
        }

        return toPaymentDTO(payment);
    }

    public List<PaymentDTO> getPayments(String loanId, User user) {
        Loan loan = loanRepository.findById(loanId)
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
    public void deletePayment(String loanId, String paymentId, User user) {
        Loan loan = loanRepository.findById(loanId)
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
