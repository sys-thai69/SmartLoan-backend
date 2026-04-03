package com.smartloan.repository;

import com.smartloan.entity.PlatformFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformFeeRepository extends JpaRepository<PlatformFee, String> {

    List<PlatformFee> findByLoanId(String loanId);

    List<PlatformFee> findByPaymentId(String paymentId);

    @Query("SELECT COALESCE(SUM(f.feeAmount), 0) FROM PlatformFee f")
    Double getTotalRevenue();

    @Query("SELECT COUNT(f) FROM PlatformFee f")
    long getTotalFeeCount();
}
