package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.service.AdminService;
import com.smartloan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LoanService loanService;

    @Value("${admin.bootstrap.key:}")
    private String bootstrapKey;

    // ── Admin-only endpoints ──────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserWithStatsDTO>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailDTO> getUserDetail(@PathVariable String id) {
        return ResponseEntity.ok(adminService.getUserDetail(id, loanService));
    }

    @GetMapping("/loans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanDTO>> getAllLoans() {
        return ResponseEntity.ok(adminService.getAllLoans(loanService));
    }

    @PatchMapping("/loans/{id}/flag")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanDTO> flagLoan(@PathVariable String id) {
        return ResponseEntity.ok(adminService.flagLoan(id, loanService));
    }

    @PatchMapping("/loans/{id}/unflag")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanDTO> unflagLoan(@PathVariable String id) {
        return ResponseEntity.ok(adminService.unflagLoan(id, loanService));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserWithStatsDTO> setUserRole(
            @PathVariable String id,
            @Valid @RequestBody SetRoleRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(adminService.setUserRole(id, request.getRole(), currentUser.getId()));
    }

    // ── Bootstrap (one-time, requires secret key) ─────────

    @PostMapping("/bootstrap")
    public ResponseEntity<Map<String, String>> bootstrap(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {

        String providedKey = body.getOrDefault("key", "");

        // Validate bootstrap key
        if (bootstrapKey == null || bootstrapKey.isEmpty()) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Admin bootstrap key is not configured on the server."));
        }

        if (!bootstrapKey.equals(providedKey)) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Invalid bootstrap key."));
        }

        boolean promoted = adminService.bootstrapAdmin(currentUser.getId());
        if (promoted) {
            return ResponseEntity.ok(Map.of("message", "You are now the platform admin."));
        } else {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "Admin already exists. Bootstrap is disabled."));
        }
    }
}
