package com.smartloan.service;

import com.smartloan.entity.Otp;
import com.smartloan.entity.OtpType;
import com.smartloan.repository.OtpRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;

    @Value("${spring.mail.username:}")
    private String emailFrom;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    public void initTwilio() {
        if (twilioAccountSid != null && !twilioAccountSid.isEmpty()
            && twilioAuthToken != null && !twilioAuthToken.isEmpty()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio credentials not configured - SMS will not work");
        }
    }

    private String generateOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Transactional
    public void sendEmailOtp(String email) {
        String code = generateOtpCode();

        // Save OTP to database
        Otp otp = Otp.builder()
                .target(email)
                .type(OtpType.EMAIL)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpRepository.save(otp);

        // Send email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("SmartLoan - Email Verification Code");
            message.setText(
                "Your SmartLoan verification code is: " + code + "\n\n" +
                "This code will expire in " + OTP_EXPIRY_MINUTES + " minutes.\n\n" +
                "If you didn't request this code, please ignore this email."
            );
            mailSender.send(message);
            log.info("Email OTP sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email OTP to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    @Transactional
    public void sendSmsOtp(String phoneNumber) {
        if (twilioAccountSid == null || twilioAccountSid.isEmpty()) {
            throw new RuntimeException("SMS service is not configured");
        }

        String code = generateOtpCode();

        // Save OTP to database
        Otp otp = Otp.builder()
                .target(phoneNumber)
                .type(OtpType.SMS)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpRepository.save(otp);

        // Send SMS via Twilio
        try {
            Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                "Your SmartLoan verification code is: " + code + ". Valid for " + OTP_EXPIRY_MINUTES + " minutes."
            ).create();
            log.info("SMS OTP sent to: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS OTP to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS. Please check your phone number and try again.");
        }
    }

    @Transactional
    public boolean verifyOtp(String target, OtpType type, String code) {
        return otpRepository.findFirstByTargetAndTypeAndUsedFalseOrderByCreatedAtDesc(target, type)
                .map(otp -> {
                    if (!otp.isValid()) {
                        return false;
                    }
                    if (otp.getCode().equals(code)) {
                        otp.setUsed(true);
                        otpRepository.save(otp);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}
