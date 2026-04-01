package com.smartloan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$|^$", message = "Invalid phone number")
    private String phoneNumber;

    private String profilePicture;
}
