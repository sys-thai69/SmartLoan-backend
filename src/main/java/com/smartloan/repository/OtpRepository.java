package com.smartloan.repository;

import com.smartloan.entity.Otp;
import com.smartloan.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {
    Optional<Otp> findFirstByTargetAndTypeAndUsedFalseOrderByCreatedAtDesc(String target, OtpType type);
    void deleteByTargetAndType(String target, OtpType type);
}
