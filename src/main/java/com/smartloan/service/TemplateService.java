package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final LoanTemplateRepository templateRepository;

    public List<LoanTemplateDTO> getAllTemplates(User user) {
        return templateRepository.findByUserId(user.getId())
                .stream()
                .map(this::toTemplateDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanTemplateDTO createTemplate(CreateTemplateRequest request, User user) {
        LoanTemplate template = LoanTemplate.builder()
                .userId(user.getId())
                .templateName(request.getTemplateName())
                .amount(request.getAmount())
                .interestRate(request.getInterestRate() != null ? request.getInterestRate() : 0.0)
                .frequency(request.getFrequency())
                .installments(request.getInstallments())
                .autoDebit(request.getAutoDebit() != null && request.getAutoDebit())
                .build();

        template = templateRepository.save(template);
        return toTemplateDTO(template);
    }

    @Transactional
    public void deleteTemplate(String id, User user) {
        LoanTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (!template.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        templateRepository.delete(template);
    }

    private LoanTemplateDTO toTemplateDTO(LoanTemplate template) {
        return LoanTemplateDTO.builder()
                .id(template.getId())
                .userId(template.getUserId())
                .templateName(template.getTemplateName())
                .amount(template.getAmount())
                .interestRate(template.getInterestRate())
                .frequency(template.getFrequency())
                .installments(template.getInstallments())
                .autoDebit(template.getAutoDebit())
                .createdAt(template.getCreatedAt().toString())
                .build();
    }
}
