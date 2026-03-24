package com.smartloan.repository;

import com.smartloan.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {

    List<WalletTransaction> findByToUserOrFromUserOrderByCreatedAtDesc(String toUser, String fromUser);
}
