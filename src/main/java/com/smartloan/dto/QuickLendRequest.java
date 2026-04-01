package com.smartloan.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class QuickLendRequest {
    @NotBlank(message = "Borrower email or phone is required")
    private String borrowerEmail;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String templateId;
}
