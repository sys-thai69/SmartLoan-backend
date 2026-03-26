package com.smartloan.repository;

import com.smartloan.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByLoanIdOrderByPaymentDateDesc(String loanId);

    // Batch fetch payments for multiple loans
    @Query("SELECT p FROM Payment p WHERE p.loanId IN :loanIds ORDER BY p.loanId, p.paymentDate DESC")
    List<Payment> findByLoanIdInOrderByPaymentDateDesc(List<String> loanIds);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.loanId = :loanId")
    Double getTotalPaidForLoan(String loanId);
}
