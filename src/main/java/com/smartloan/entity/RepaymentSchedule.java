package com.smartloan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loanId", insertable = false, updatable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNo;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private Double amountDue;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPaid = false;

    private LocalDateTime paidAt;
}
