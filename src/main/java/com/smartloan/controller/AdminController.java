package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.service.AdminService;
import com.smartloan.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final LoanService loanService;

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserWithStatsDTO>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/loans/{id}/flag")
    public ResponseEntity<LoanDTO> flagLoan(@PathVariable String id) {
        return ResponseEntity.ok(adminService.flagLoan(id, loanService));
    }
}
