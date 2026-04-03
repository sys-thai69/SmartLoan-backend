package com.smartloan.service;

import com.smartloan.entity.EmailVerification;
import com.smartloan.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationRepository repository;
    private final EmailService emailService;
    private static final int MAX_ATTEMPTS = 5;

    public void sendVerificationCode(String email) {
        // Delete old verification if exists
        repository.findByEmail(email).ifPresent(repository::delete);

        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Save verification record
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .build();
        repository.save(verification);

        // Send email
        emailService.sendVerificationCode(email, code);
    }

    public boolean verifyCode(String email, String code) {
        EmailVerification verification = repository.findByEmailAndCode(email, code)
                .orElseThrow(() -> new RuntimeException("Invalid verification code"));

        // Check if expired
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code has expired");
        }

        // Check attempts
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new RuntimeException("Too many verification attempts. Please request a new code.");
        }

        // Mark as verified
        verification.setVerified(true);
        repository.save(verification);

        return true;
    }
}
