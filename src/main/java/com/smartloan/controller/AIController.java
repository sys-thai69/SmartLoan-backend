package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/parse-loan")
    public ResponseEntity<ParseLoanResponse> parseLoan(
            @Valid @RequestBody ParseLoanRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiService.parseLoan(request));
    }
}
