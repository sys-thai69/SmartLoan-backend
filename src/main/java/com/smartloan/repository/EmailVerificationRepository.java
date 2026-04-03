package com.smartloan.repository;

import com.smartloan.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
    Optional<EmailVerification> findByEmailAndCode(String email, String code);
    Optional<EmailVerification> findByEmail(String email);
}
