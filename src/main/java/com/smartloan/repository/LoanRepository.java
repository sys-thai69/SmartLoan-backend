package com.smartloan.repository;

import com.smartloan.entity.Loan;
import com.smartloan.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    List<Loan> findByLenderIdOrBorrowerId(String lenderId, String borrowerId);

    @Query("SELECT l FROM Loan l WHERE l.lenderId = :userId OR l.borrowerId = :userId")
    List<Loan> findAllByUserId(@Param("userId") String userId);

    List<Loan> findByStatus(LoanStatus status);

    long countByStatus(LoanStatus status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.lenderId = :userId")
    long countByLenderId(@Param("userId") String userId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.borrowerId = :userId")
    long countByBorrowerId(@Param("userId") String userId);

    @Query("SELECT SUM(l.principal) FROM Loan l")
    Double getTotalVolume();
}
