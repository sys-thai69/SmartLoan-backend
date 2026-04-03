package com.smartloan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@smartloan.com}")
    private String fromEmail;

    @Value("${mail.dev-mode:true}")
    private boolean devMode;

    public void sendVerificationCode(String email, String code) {
        try {
            // In development mode, just log the code
            if (devMode) {
                log.info("=== DEV MODE: VERIFICATION CODE ===");
                log.info("Email: {}", email);
                log.info("Code: {}", code);
                log.info("====================================");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("SmartLoan Email Verification Code");
            message.setText(
                "Your SmartLoan email verification code is:\n\n" +
                code + "\n\n" +
                "This code will expire in 10 minutes.\n" +
                "Do not share this code with anyone.\n\n" +
                "If you didn't request this, please ignore this email."
            );
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }
}
