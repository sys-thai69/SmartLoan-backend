package com.smartloan.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.smartloan.entity.User;
import com.smartloan.entity.UserRole;
import com.smartloan.entity.Wallet;
import com.smartloan.repository.UserRepository;
import com.smartloan.repository.WalletRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
// @Component - Disabled for local JWT auth testing. Use JwtAuthenticationFilter instead.
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String idToken = authHeader.substring(7);

            // Verify the Firebase ID token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String phone = decodedToken.getClaims().get("phone_number") != null
                    ? decodedToken.getClaims().get("phone_number").toString()
                    : null;

            // Find or create user in our database
            User user = findOrCreateUser(firebaseUid, email, name, phone, decodedToken.isEmailVerified());

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (FirebaseAuthException e) {
            log.debug("Invalid Firebase token: {}", e.getMessage());
            // Invalid token - continue without authentication
        } catch (Exception e) {
            log.error("Error processing Firebase token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private User findOrCreateUser(String firebaseUid, String email, String name, String phone, boolean emailVerified) {
        // First try to find by Firebase UID
        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);

        if (user != null) {
            // Update email verification status if changed
            if (emailVerified && !user.getEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
            }
            return user;
        }

        // Try to find by email (for existing users migrating to Firebase)
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                // Link existing user to Firebase
                user.setFirebaseUid(firebaseUid);
                user.setEmailVerified(emailVerified);
                userRepository.save(user);
                log.info("Linked existing user {} to Firebase UID {}", email, firebaseUid);
                return user;
            }
        }

        // Create new user
        user = User.builder()
                .firebaseUid(firebaseUid)
                .email(email != null ? email : firebaseUid + "@phone.user")
                .name(name != null ? name : "SmartLoan User")
                .phoneNumber(phone)
                .emailVerified(emailVerified)
                .phoneVerified(phone != null)
                .role(UserRole.USER)
                .trustScore(100.0)
                .build();

        user = userRepository.save(user);

        // Create wallet for new user
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .build();
        walletRepository.save(wallet);

        log.info("Created new user from Firebase: {}", user.getEmail());

        return user;
    }
}
