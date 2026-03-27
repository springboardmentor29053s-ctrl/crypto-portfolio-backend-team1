package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);
    Optional<Wallet> findByUserId(Long userId);
}