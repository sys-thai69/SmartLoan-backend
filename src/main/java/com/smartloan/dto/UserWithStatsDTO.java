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
public class UserWithStatsDTO {
    private String id;
    private String name;
    private String email;
    private UserRole role;
    private Double trustScore;
    private String createdAt;
    private Long loanCount;
    private Long borrowCount;
}
