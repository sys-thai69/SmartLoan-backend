package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseLoanResponse {
    private String borrowerEmail;
    private Double amount;
    private String duration;
    private Double interestRate;
    private Boolean parsed;
    private String borrowerName;
    private Integer installments;
    private String frequency;
}
