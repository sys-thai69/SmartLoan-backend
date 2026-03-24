package com.smartloan.dto;

import com.smartloan.entity.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private String id;
    private String loanId;
    private String paidBy;
    private String scheduleId;
    private Double amount;
    private String paymentDate;
    private String note;
    private PaymentType type;
    private String createdAt;
}
