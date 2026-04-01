package com.smartloan.dto;

import com.smartloan.entity.LoanFrequency;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateLoanRequest {
    @NotBlank(message = "Borrower email or phone is required")
    private String borrowerEmail;

    @NotNull(message = "Principal amount is required")
    @Positive(message = "Principal must be positive")
    private Double principal;

    @Min(value = 0, message = "Interest rate cannot be negative")
    private Double interestRate = 0.0;

    @NotNull(message = "Number of installments is required")
    @Min(value = 1, message = "At least 1 installment required")
    private Integer installments;

    @NotNull(message = "Frequency is required")
    private LoanFrequency frequency;

    @NotBlank(message = "Start date is required")
    private String startDate;

    private Boolean autoDebit = false;

    private String templateId;
}
