package com.smartloan.dto;

import com.smartloan.entity.LoanFrequency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be at least 0")
    private Double amount;

    @Min(value = 0, message = "Interest rate cannot be negative")
    private Double interestRate = 0.0;

    @NotNull(message = "Frequency is required")
    private LoanFrequency frequency;

    @NotNull(message = "Number of installments is required")
    @Min(value = 1, message = "At least 1 installment required")
    private Integer installments;

    private Boolean autoDebit = false;
}
