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
}
