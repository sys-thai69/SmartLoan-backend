package com.smartloan.dto;

import com.smartloan.entity.AutoDebitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoDebitDTO {
    private String id;
    private String loanId;
    private Double amount;
    private LocalDate nextDebitDate;
    private AutoDebitStatus status;
    private Integer failureCount;
    private LocalDateTime lastAttempt;
    private String lastFailureReason;
    private LocalDateTime createdAt;
}
