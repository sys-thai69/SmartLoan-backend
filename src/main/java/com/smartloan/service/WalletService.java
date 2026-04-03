package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.*;
import com.smartloan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletDTO getMyWallet(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        List<WalletTransaction> transactions = transactionRepository
                .findByToUserOrFromUserOrderByCreatedAtDesc(user.getId(), user.getId());

        return toWalletDTO(wallet, transactions);
    }

    @Transactional
    public WalletDTO topUp(TopUpRequest request, User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setBalance(wallet.getBalance() + request.getAmount());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .toUser(user.getId())
                .amount(request.getAmount())
                .type(TransactionType.TOPUP)
                .build();
        transactionRepository.save(transaction);

        List<WalletTransaction> transactions = transactionRepository
                .findByToUserOrFromUserOrderByCreatedAtDesc(user.getId(), user.getId());

        return toWalletDTO(wallet, transactions);
    }

    @Transactional
    public WalletDTO transfer(TransferRequest request, User user) {
        Wallet senderWallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (senderWallet.getBalance() < request.getAmount()) {
            throw new RuntimeException("Insufficient balance");
        }

        // Guard: cannot transfer to yourself
        if (user.getId().equals(request.getToUserId())) {
            throw new RuntimeException("Cannot transfer money to yourself");
        }

        User recipient = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        Wallet recipientWallet = walletRepository.findByUserId(recipient.getId())
                .orElseThrow(() -> new RuntimeException("Recipient wallet not found"));

        // Update balances
        senderWallet.setBalance(senderWallet.getBalance() - request.getAmount());
        senderWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(senderWallet);

        recipientWallet.setBalance(recipientWallet.getBalance() + request.getAmount());
        recipientWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(recipientWallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .fromUser(user.getId())
                .toUser(request.getToUserId())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .note(request.getNote())
                .build();
        transactionRepository.save(transaction);

        List<WalletTransaction> transactions = transactionRepository
                .findByToUserOrFromUserOrderByCreatedAtDesc(user.getId(), user.getId());

        return toWalletDTO(senderWallet, transactions);
    }

    @Transactional
    public void payLoan(String borrowerId, String lenderId, String loanId, Double amount) {
        Wallet borrowerWallet = walletRepository.findByUserId(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower wallet not found"));

        if (borrowerWallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance to make payment");
        }

        Wallet lenderWallet = walletRepository.findByUserId(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender wallet not found"));

        // Update balances
        borrowerWallet.setBalance(borrowerWallet.getBalance() - amount);
        borrowerWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(borrowerWallet);

        lenderWallet.setBalance(lenderWallet.getBalance() + amount);
        lenderWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(lenderWallet);

        // Create transaction record with loan reference
        WalletTransaction transaction = WalletTransaction.builder()
                .fromUser(borrowerId)
                .toUser(lenderId)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .loanId(loanId)
                .note("Loan payment")
                .build();
        transactionRepository.save(transaction);
    }

    private WalletDTO toWalletDTO(Wallet wallet, List<WalletTransaction> transactions) {
        return WalletDTO.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .updatedAt(wallet.getUpdatedAt().toString())
                .transactions(transactions.stream().map(this::toTransactionDTO).collect(Collectors.toList()))
                .build();
    }

    private com.smartloan.dto.WalletTransactionDTO toTransactionDTO(WalletTransaction tx) {
        return com.smartloan.dto.WalletTransactionDTO.builder()
                .id(tx.getId())
                .fromUser(tx.getFromUser())
                .toUser(tx.getToUser())
                .amount(tx.getAmount())
                .type(tx.getType())
                .loanId(tx.getLoanId())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt().toString())
                .build();
    }

    @Transactional
    public void deductFromWallet(String userId, Double amount, String note, String loanId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance. Required: $" + amount + ", Available: $" + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record - RESERVED for pending loans
        WalletTransaction transaction = WalletTransaction.builder()
                .fromUser(userId)
                .toUser(null)
                .amount(amount)
                .type(TransactionType.RESERVED)
                .loanId(loanId)
                .note(note)
                .build();
        transactionRepository.save(transaction);
    }

    @Transactional
    public void creditToWallet(String userId, Double amount, String note, String loanId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Determine transaction type based on note
        TransactionType type = note.contains("refunded") ? TransactionType.REFUND : TransactionType.TOPUP;

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .toUser(userId)
                .fromUser(null)
                .amount(amount)
                .type(type)
                .loanId(loanId)
                .note(note)
                .build();
        transactionRepository.save(transaction);
    }

    @Transactional
    public void acceptLoanTransfer(String lenderId, String borrowerId, String loanId, Double amount, String note) {
        Wallet borrowerWallet = walletRepository.findByUserId(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower wallet not found"));

        // Credit the borrower with the principal
        borrowerWallet.setBalance(borrowerWallet.getBalance() + amount);
        borrowerWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(borrowerWallet);

        // Create transaction record showing transfer from lender to borrower
        WalletTransaction transaction = WalletTransaction.builder()
                .fromUser(lenderId)
                .toUser(borrowerId)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .loanId(loanId)
                .note(note)
                .build();
        transactionRepository.save(transaction);
    }
}
