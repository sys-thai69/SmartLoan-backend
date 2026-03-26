package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.OtpType;
import com.smartloan.entity.User;
import com.smartloan.entity.Wallet;
import com.smartloan.repository.OtpRepository;
import com.smartloan.repository.UserRepository;
import com.smartloan.repository.WalletRepository;
import com.smartloan.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            OtpRepository otpRepository,
            @Lazy PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean phoneExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Check if phone already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        // Verify email OTP
        boolean emailVerified = verifyOtp(request.getEmail(), OtpType.EMAIL, request.getEmailOtp());
        if (!emailVerified) {
            throw new RuntimeException("Invalid or expired email verification code");
        }

        // Verify SMS OTP
        boolean phoneVerified = verifyOtp(request.getPhoneNumber(), OtpType.SMS, request.getSmsOtp());
        if (!phoneVerified) {
            throw new RuntimeException("Invalid or expired SMS verification code");
        }

        // Create user with verified email and phone
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(true)
                .phoneVerified(true)
                .build();

        user = userRepository.save(user);

        // Create wallet for user
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .build();
        walletRepository.save(wallet);

        // Generate token
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .user(toUserDTO(user))
                .token(token)
                .build();
    }

    private boolean verifyOtp(String target, OtpType type, String code) {
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

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .user(toUserDTO(user))
                .token(token)
                .build();
    }

    public UserDTO getCurrentUser(User user) {
        return toUserDTO(user);
    }

    public UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .trustScore(user.getTrustScore())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}
