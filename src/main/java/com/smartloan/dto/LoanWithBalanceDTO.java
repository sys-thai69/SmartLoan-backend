package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
