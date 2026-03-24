package com.smartloan.dto;

import com.smartloan.entity.LoanFrequency;
import com.smartloan.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private String id;
    private String lenderId;
    private String borrowerId;
    private UserDTO lender;
    private UserDTO borrower;
    private Double principal;
    private Double interestRate;
    private Double totalAmount;
    private Integer installments;
    private LoanFrequency frequency;
    private String startDate;
    private LoanStatus status;
    private Boolean autoDebit;
    private Boolean isQuickLend;
    private String templateId;
    private String createdAt;
    private List<RepaymentScheduleDTO> schedule;
    private List<PaymentDTO> payments;
}
