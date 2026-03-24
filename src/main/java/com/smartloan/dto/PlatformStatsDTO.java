package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsDTO {
    private Long totalUsers;
    private Long totalLoans;
    private Long activeLoans;
    private Long overdueLoans;
    private Double totalVolume;
    private Long completedLoans;
}
