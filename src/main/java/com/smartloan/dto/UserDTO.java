package com.smartloan.dto;

import com.smartloan.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePicture;
    private UserRole role;
    private Double trustScore;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String createdAt;
}
