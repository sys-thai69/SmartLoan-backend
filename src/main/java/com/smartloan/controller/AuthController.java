package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.OtpType;
import com.smartloan.entity.User;
import com.smartloan.service.AuthService;
import com.smartloan.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/send-email-otp")
    public ResponseEntity<OtpResponse> sendEmailOtp(@Valid @RequestBody SendEmailOtpRequest request) {
        // Check if email already exists
        if (authService.emailExists(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                OtpResponse.builder()
                    .success(false)
                    .message("Email already registered")
                    .build()
            );
        }
        otpService.sendEmailOtp(request.getEmail());
        return ResponseEntity.ok(
            OtpResponse.builder()
                .success(true)
                .message("Verification code sent to your email")
                .build()
        );
    }

    @PostMapping("/send-sms-otp")
    public ResponseEntity<OtpResponse> sendSmsOtp(@Valid @RequestBody SendSmsOtpRequest request) {
        // Check if phone already exists
        if (authService.phoneExists(request.getPhoneNumber())) {
            return ResponseEntity.badRequest().body(
                OtpResponse.builder()
                    .success(false)
                    .message("Phone number already registered")
                    .build()
            );
        }
        otpService.sendSmsOtp(request.getPhoneNumber());
        return ResponseEntity.ok(
            OtpResponse.builder()
                .success(true)
                .message("Verification code sent to your phone")
                .build()
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpType type = OtpType.valueOf(request.getType().toUpperCase());
        boolean valid = otpService.verifyOtp(request.getTarget(), type, request.getCode());
        if (valid) {
            return ResponseEntity.ok(
                OtpResponse.builder()
                    .success(true)
                    .message("OTP verified successfully")
                    .build()
            );
        } else {
            return ResponseEntity.badRequest().body(
                OtpResponse.builder()
                    .success(false)
                    .message("Invalid or expired OTP")
                    .build()
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getCurrentUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless, so logout is handled client-side
        return ResponseEntity.ok().build();
    }
}
