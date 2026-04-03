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

    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.lender LEFT JOIN FETCH l.borrower WHERE l.lenderId = :userId OR l.borrowerId = :userId")
    List<Loan> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.lender LEFT JOIN FETCH l.borrower WHERE l.id = :id")
    java.util.Optional<Loan> findByIdWithUsers(@Param("id") String id);

    List<Loan> findByStatus(LoanStatus status);

    long countByStatus(LoanStatus status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.lenderId = :userId")
    long countByLenderId(@Param("userId") String userId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.borrowerId = :userId")
    long countByBorrowerId(@Param("userId") String userId);

    @Query("SELECT SUM(l.principal) FROM Loan l")
    Double getTotalVolume();

    // Optimized query for admin - get all user stats in one query
    @Query("SELECT u.id, " +
           "COUNT(CASE WHEN l.lenderId = u.id THEN 1 END), " +
           "COUNT(CASE WHEN l.borrowerId = u.id THEN 1 END) " +
           "FROM User u LEFT JOIN Loan l ON l.lenderId = u.id OR l.borrowerId = u.id " +
           "GROUP BY u.id")
    List<Object[]> getUserLoanStats();

    // Admin: all loans with user details
    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.lender LEFT JOIN FETCH l.borrower ORDER BY l.createdAt DESC")
    List<Loan> findAllWithUsers();

    // Admin: count flagged loans
    long countByFlaggedTrue();

    // Admin: user detail - loans as lender
    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.lender LEFT JOIN FETCH l.borrower WHERE l.lenderId = :userId ORDER BY l.createdAt DESC")
    List<Loan> findByLenderIdWithUsers(@Param("userId") String userId);

    // Admin: user detail - loans as borrower
    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.lender LEFT JOIN FETCH l.borrower WHERE l.borrowerId = :userId ORDER BY l.createdAt DESC")
    List<Loan> findByBorrowerIdWithUsers(@Param("userId") String userId);
}
