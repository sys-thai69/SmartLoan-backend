package com.smartloan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_debit_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoDebitSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String loanId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loanId", insertable = false, updatable = false)
    private Loan loan;

    @Column(nullable = false)
    private String scheduleId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDate nextDebitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AutoDebitStatus status = AutoDebitStatus.ACTIVE;

    @Builder.Default
    private Integer failureCount = 0;

    private LocalDateTime lastAttempt;

    @Column(columnDefinition = "TEXT")
    private String lastFailureReason;

    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
