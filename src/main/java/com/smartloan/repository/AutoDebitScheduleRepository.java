package com.smartloan.repository;

import com.smartloan.entity.AutoDebitSchedule;
import com.smartloan.entity.AutoDebitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AutoDebitScheduleRepository extends JpaRepository<AutoDebitSchedule, String> {
    Optional<AutoDebitSchedule> findByLoanId(String loanId);

    List<AutoDebitSchedule> findByStatusAndNextDebitDateLessThanEqual(AutoDebitStatus status, LocalDate date);

    @Query("SELECT a FROM AutoDebitSchedule a WHERE a.status = :status AND a.nextDebitDate <= CURRENT_DATE")
    List<AutoDebitSchedule> findDueAutoDebits(@Param("status") AutoDebitStatus status);

    List<AutoDebitSchedule> findByLoanIdOrderByCreatedAtDesc(String loanId);
}
