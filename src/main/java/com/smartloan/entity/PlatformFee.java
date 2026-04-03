package com.smartloan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String loanId;

    @Column(nullable = false)
    private String paymentId;

    // The original transaction amount
    @Column(nullable = false)
    private Double transactionAmount;

    // Fee rate applied (e.g., 0.015 for 1.5%)
    @Column(nullable = false)
    private Double feeRate;

    // Actual fee amount collected
    @Column(nullable = false)
    private Double feeAmount;

    // Who is charged the fee (lender), recipient is platform (null for internal revenue)
    private String payerId;
    private String recipientId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
