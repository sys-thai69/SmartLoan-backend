package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleDTO {
    private String id;
    private String loanId;
    private Integer installmentNo;
    private String dueDate;
    private Double amountDue;
    private Boolean isPaid;
    private String paidAt;
}
