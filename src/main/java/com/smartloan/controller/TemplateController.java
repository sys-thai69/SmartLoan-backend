package com.smartloan.controller;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<LoanTemplateDTO>> getAllTemplates(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(templateService.getAllTemplates(user));
    }

    @PostMapping
    public ResponseEntity<LoanTemplateDTO> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(templateService.createTemplate(request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        templateService.deleteTemplate(id, user);
        return ResponseEntity.ok().build();
    }
}
