package com.smartloan.repository;

import com.smartloan.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, String> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNo(String loanId);

    // Batch fetch schedules for multiple loans
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loanId IN :loanIds ORDER BY rs.loanId, rs.installmentNo")
    List<RepaymentSchedule> findByLoanIdInOrderByInstallmentNo(List<String> loanIds);

    @Query("SELECT rs FROM RepaymentSchedule rs JOIN rs.loan l WHERE (l.lenderId = :userId OR l.borrowerId = :userId) AND rs.isPaid = false AND rs.dueDate < :today")
    List<RepaymentSchedule> findOverdueByUserId(String userId, LocalDate today);
}
