package com.smartloan.dto;

import com.smartloan.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDTO {
    private String id;
    private String fromUser;
    private String toUser;
    private Double amount;
    private TransactionType type;
    private String loanId;
    private String note;
    private String createdAt;
}
