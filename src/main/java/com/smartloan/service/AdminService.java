package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final PlatformFeeRepository platformFeeRepository;

    @Transactional
    public UserWithStatsDTO setUserRole(String userId, UserRole newRole, String requesterId) {
        if (userId.equals(requesterId) && newRole != UserRole.ADMIN) {
            throw new RuntimeException("You cannot demote yourself. Ask another admin to change your role.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(newRole);
        user = userRepository.save(user);

        return UserWithStatsDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .trustScore(user.getTrustScore())
                .createdAt(user.getCreatedAt().toString())
                .loanCount(0L)
                .borrowCount(0L)
                .build();
    }

    /**
     * Bootstrap: makes the requesting user an admin IF no admin yet exists.
     * Safe to call repeatedly — no-op once the first admin is set.
     */
    @Transactional
    public boolean bootstrapAdmin(String userId) {
        boolean adminExists = userRepository.existsByRole(UserRole.ADMIN);
        if (adminExists) {
            return false; // Already has an admin — refuse
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        return true;
    }

    public PlatformStatsDTO getStats() {
        long totalUsers = userRepository.count();
        long totalLoans = loanRepository.count();
        long activeLoans = loanRepository.countByStatus(LoanStatus.ACTIVE);
        long overdueLoans = loanRepository.countByStatus(LoanStatus.OVERDUE);
        long completedLoans = loanRepository.countByStatus(LoanStatus.COMPLETED);
        long flaggedLoans = loanRepository.countByFlaggedTrue();
        Double totalVolume = loanRepository.getTotalVolume();
        Double platformRevenue = platformFeeRepository.getTotalRevenue();
        long totalFeeTransactions = platformFeeRepository.getTotalFeeCount();

        return PlatformStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalLoans(totalLoans)
                .activeLoans(activeLoans)
                .overdueLoans(overdueLoans)
                .completedLoans(completedLoans)
                .flaggedLoans(flaggedLoans)
                .totalVolume(totalVolume != null ? totalVolume : 0.0)
                .platformRevenue(platformRevenue != null ? platformRevenue : 0.0)
                .totalFeeTransactions(totalFeeTransactions)
                .build();
    }

    public List<UserWithStatsDTO> getAllUsers() {
        // Get all users
        List<User> users = userRepository.findAll();

        // Get loan stats in a single query (userId -> [loanCount, borrowCount])
        Map<String, long[]> userStats = loanRepository.getUserLoanStats().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> new long[] {
                            row[1] != null ? ((Number) row[1]).longValue() : 0L,
                            row[2] != null ? ((Number) row[2]).longValue() : 0L
                        }
                ));

        return users.stream()
                .map(user -> {
                    long[] stats = userStats.getOrDefault(user.getId(), new long[] {0L, 0L});
                    return UserWithStatsDTO.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .trustScore(user.getTrustScore())
                            .createdAt(user.getCreatedAt().toString())
                            .loanCount(stats[0])
                            .borrowCount(stats[1])
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getAllLoans(LoanService loanService) {
        return loanRepository.findAllWithUsers().stream()
                .map(loanService::toLoanDTO)
                .collect(Collectors.toList());
    }

    public AdminUserDetailDTO getUserDetail(String userId, LoanService loanService) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build user stats
        long lentCount = loanRepository.countByLenderId(userId);
        long borrowCount = loanRepository.countByBorrowerId(userId);

        UserWithStatsDTO userDTO = UserWithStatsDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .trustScore(user.getTrustScore())
                .createdAt(user.getCreatedAt().toString())
                .loanCount(lentCount)
                .borrowCount(borrowCount)
                .build();

        List<LoanDTO> asLender = loanRepository.findByLenderIdWithUsers(userId).stream()
                .map(loanService::toLoanDTO)
                .collect(Collectors.toList());

        List<LoanDTO> asBorrower = loanRepository.findByBorrowerIdWithUsers(userId).stream()
                .map(loanService::toLoanDTO)
                .collect(Collectors.toList());

        return AdminUserDetailDTO.builder()
                .user(userDTO)
                .loansAsLender(asLender)
                .loansAsBorrower(asBorrower)
                .build();
    }

    @Transactional
    public LoanDTO flagLoan(String id, LoanService loanService) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Set the flag fields (admin flagging is distinct from user-reported flagging)
        loan.setFlagged(true);
        if (loan.getFlagReason() == null || loan.getFlagReason().isBlank()) {
            loan.setFlagReason("Flagged by admin for review");
        }
        loan.setFlaggedBy("ADMIN");
        loan.setFlaggedAt(java.time.LocalDateTime.now());
        loan = loanRepository.save(loan);

        return loanService.toLoanDTO(loan);
    }

    @Transactional
    public LoanDTO unflagLoan(String id, LoanService loanService) {
        Loan loan = loanRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        loan.setFlagged(false);
        loan.setFlagReason(null);
        loan.setFlaggedBy(null);
        loan.setFlaggedAt(null);
        loan = loanRepository.save(loan);

        return loanService.toLoanDTO(loan);
    }
}

