package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.*;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.repository.WalletRepository;
import com.crypto.cryptoPortfolio.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Get or create wallet for user
     */
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    wallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    /**
     * Get wallet balance
     */
    public BigDecimal getBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = getOrCreateWallet(user);
        return wallet.getBalance();
    }

    /**
     * Deposit funds
     */
    @Transactional
    public WalletTransaction deposit(String email, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than 0");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = getOrCreateWallet(user);

        // Update balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Record transaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }

    /**
     * Withdraw funds
     */
    @Transactional
    public WalletTransaction withdraw(String email, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than 0");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = getOrCreateWallet(user);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Update balance
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // Record transaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }

    /**
     * Get transaction history
     */
    public List<WalletTransaction> getTransactions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return transactionRepository.findByUserOrderByTimestampDesc(user);
    }
}