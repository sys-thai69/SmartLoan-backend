package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<WalletDTO> getMyWallet(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(walletService.getMyWallet(user));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletDTO> topUp(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(walletService.topUp(request, user));
    }

    @PostMapping("/transfer")
    public ResponseEntity<WalletDTO> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(walletService.transfer(request, user));
    }
}
