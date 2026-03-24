package com.smartloan.dto;

import com.smartloan.entity.LoanFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanTemplateDTO {
    private String id;
    private String userId;
    private String templateName;
    private Double interestRate;
    private LoanFrequency frequency;
    private Integer installments;
    private Boolean autoDebit;
    private String createdAt;
}
