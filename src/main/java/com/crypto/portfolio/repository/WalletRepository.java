package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.User;
import com.crypto.portfolio.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);
}