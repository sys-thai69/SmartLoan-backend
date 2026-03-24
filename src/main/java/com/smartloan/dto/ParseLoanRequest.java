package com.smartloan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseLoanRequest {
    @NotBlank(message = "Text is required")
    private String text;
}
