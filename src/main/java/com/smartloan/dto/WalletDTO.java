package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private String id;
    private String userId;
    private Double balance;
    private String currency;
    private String updatedAt;
    private List<WalletTransactionDTO> transactions;
}
