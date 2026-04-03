package com.smartloan.dto;

import com.smartloan.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}
