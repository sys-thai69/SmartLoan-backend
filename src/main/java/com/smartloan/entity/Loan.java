package com.smartloan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String lenderId;

    @Column(nullable = false)
    private String borrowerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lenderId", insertable = false, updatable = false)
    private User lender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrowerId", insertable = false, updatable = false)
    private User borrower;

    @Column(nullable = false)
    private Double principal;

    @Column(nullable = false)
    @Builder.Default
    private Double interestRate = 0.0;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    @Builder.Default
    private Integer installments = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanFrequency frequency = LoanFrequency.MONTHLY;

    @Column(nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING_ACCEPTANCE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean autoDebit = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isQuickLend = false;

    private String templateId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean flagged = false;

    private String flagReason;

    private String flaggedBy;

    private LocalDateTime flaggedAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RepaymentSchedule> schedule = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}
