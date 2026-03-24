package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    public PlatformStatsDTO getStats() {
        long totalUsers = userRepository.count();
        long totalLoans = loanRepository.count();
        long activeLoans = loanRepository.countByStatus(LoanStatus.ACTIVE);
        long overdueLoans = loanRepository.countByStatus(LoanStatus.OVERDUE);
        long completedLoans = loanRepository.countByStatus(LoanStatus.COMPLETED);
        Double totalVolume = loanRepository.getTotalVolume();

        return PlatformStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalLoans(totalLoans)
                .activeLoans(activeLoans)
                .overdueLoans(overdueLoans)
                .completedLoans(completedLoans)
                .totalVolume(totalVolume != null ? totalVolume : 0.0)
                .build();
    }

    public List<UserWithStatsDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    long loanCount = loanRepository.countByLenderId(user.getId());
                    long borrowCount = loanRepository.countByBorrowerId(user.getId());

                    return UserWithStatsDTO.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .trustScore(user.getTrustScore())
                            .createdAt(user.getCreatedAt().toString())
                            .loanCount(loanCount)
                            .borrowCount(borrowCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanDTO flagLoan(String id, LoanService loanService) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Set status to overdue (flagged)
        loan.setStatus(LoanStatus.OVERDUE);
        loan = loanRepository.save(loan);

        return loanService.toLoanDTO(loan);
    }
}
