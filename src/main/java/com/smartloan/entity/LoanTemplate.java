package com.smartloan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String templateName;

    @Column(nullable = false)
    @Builder.Default
    private Double interestRate = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanFrequency frequency = LoanFrequency.MONTHLY;

    @Column(nullable = false)
    @Builder.Default
    private Integer installments = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean autoDebit = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
