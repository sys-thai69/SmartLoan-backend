package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans/{loanId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDTO> logPayment(
            @PathVariable String loanId,
            @Valid @RequestBody LogPaymentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.logPayment(loanId, request, user));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentDTO> makePayment(
            @PathVariable String loanId,
            @Valid @RequestBody MakePaymentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.makePayment(loanId, request, user));
    }

    @PostMapping("/auto-debit")
    public ResponseEntity<PaymentDTO> initiateAutoDebit(
            @PathVariable String loanId,
            @Valid @RequestBody MakePaymentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.initiateAutoDebit(loanId, request, user));
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getPayments(
            @PathVariable String loanId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.getPayments(loanId, user));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(
            @PathVariable String loanId,
            @PathVariable String paymentId,
            @AuthenticationPrincipal User user) {
        paymentService.deletePayment(loanId, paymentId, user);
        return ResponseEntity.ok().build();
    }
}
