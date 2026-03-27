package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserOrderByTimestampDesc(User user);
    List<WalletTransaction> findByUserIdOrderByTimestampDesc(Long userId);
}