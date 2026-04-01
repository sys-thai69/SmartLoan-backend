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
    private String borrowerName;
    private String phoneNumber;
    private Double amount;
    private String duration;
    private Double interestRate;
    private Boolean parsed;
    private Integer installments;
    private String frequency;
}
