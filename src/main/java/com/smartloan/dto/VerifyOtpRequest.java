package com.smartloan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Target (email or phone) is required")
    private String target;

    @NotBlank(message = "OTP type is required")
    private String type; // EMAIL or SMS

    @NotBlank(message = "OTP code is required")
    private String code;
}
