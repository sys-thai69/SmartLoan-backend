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
public class LoanWithBalanceDTO {
    private LoanDTO loan;
    private Double totalPaid;
    private Double remaining;
    private Double percentPaid;

    // Flatten the loan fields for frontend compatibility
    public String getId() { return loan != null ? loan.getId() : null; }
    public String getLenderId() { return loan != null ? loan.getLenderId() : null; }
    public String getBorrowerId() { return loan != null ? loan.getBorrowerId() : null; }
    public UserDTO getLender() { return loan != null ? loan.getLender() : null; }
    public UserDTO getBorrower() { return loan != null ? loan.getBorrower() : null; }
    public Double getPrincipal() { return loan != null ? loan.getPrincipal() : null; }
    public Double getInterestRate() { return loan != null ? loan.getInterestRate() : null; }
    public Double getTotalAmount() { return loan != null ? loan.getTotalAmount() : null; }
    public Integer getInstallments() { return loan != null ? loan.getInstallments() : null; }
    public LoanFrequency getFrequency() { return loan != null ? loan.getFrequency() : null; }
    public String getStartDate() { return loan != null ? loan.getStartDate() : null; }
    public LoanStatus getStatus() { return loan != null ? loan.getStatus() : null; }
    public Boolean getAutoDebit() { return loan != null ? loan.getAutoDebit() : null; }
    public Boolean getIsQuickLend() { return loan != null ? loan.getIsQuickLend() : null; }
    public String getTemplateId() { return loan != null ? loan.getTemplateId() : null; }
    public String getCreatedAt() { return loan != null ? loan.getCreatedAt() : null; }
    public List<RepaymentScheduleDTO> getSchedule() { return loan != null ? loan.getSchedule() : null; }
    public List<PaymentDTO> getPayments() { return loan != null ? loan.getPayments() : null; }
}
