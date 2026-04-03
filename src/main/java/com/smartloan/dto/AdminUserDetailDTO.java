package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDTO {
    private UserWithStatsDTO user;
    private List<LoanDTO> loansAsLender;
    private List<LoanDTO> loansAsBorrower;
}
