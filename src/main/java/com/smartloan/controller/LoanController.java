package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanDTO> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.createLoan(request, user));
    }

    @PostMapping("/quick")
    public ResponseEntity<LoanDTO> quickLend(
            @Valid @RequestBody QuickLendRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.quickLend(request, user));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<LoanDTO>> getMyLoans(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.getMyLoans(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanWithBalanceDTO> getLoanById(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.getLoanById(id, user));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<LoanDTO> acceptLoan(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.acceptLoan(id, user));
    }

    @PatchMapping("/{id}/decline")
    public ResponseEntity<LoanDTO> declineLoan(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.declineLoan(id, user));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<LoanDTO> cancelLoan(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.cancelLoan(id, user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LoanDTO> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.updateStatus(id, request.getStatus(), user));
    }

    @PostMapping("/{id}/alert-borrower")
    public ResponseEntity<Void> alertBorrower(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        loanService.alertBorrower(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/send-due-reminder")
    public ResponseEntity<Void> sendDueReminder(
            @PathVariable String id,
            @RequestBody SendReminderRequest request,
            @AuthenticationPrincipal User user) {
        loanService.sendDueReminder(id, request, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/send-overdue-alert")
    public ResponseEntity<Void> sendOverdueAlert(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        loanService.sendOverdueAlert(id, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<RepaymentScheduleDTO>> getOverdue(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.getOverdueSchedules(user));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<LoanDTO> reportLoan(
            @PathVariable String id,
            @RequestBody ReportLoanRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.reportLoan(id, request.getReason(), user));
    }
}
