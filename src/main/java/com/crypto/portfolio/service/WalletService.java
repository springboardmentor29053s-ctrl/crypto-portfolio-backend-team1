package com.crypto.portfolio.service;



import com.crypto.portfolio.dto.WalletResponse;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.model.Wallet;
import com.crypto.portfolio.repository.UserRepository;
import com.crypto.portfolio.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Fetch wallet balance for logged-in user
     */
    public WalletResponse getWalletBalance() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return new WalletResponse(
                wallet.getBalance(),
                wallet.getCurrency()
        );
    }

    /**
     * Deduct wallet balance during BUY
     */
    public void deductBalance(User user, Double amount) {

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);

        walletRepository.save(wallet);
    }

    /**
     * Add wallet balance during SELL
     */
    public void addBalance(User user, Double amount) {

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setBalance(wallet.getBalance() + amount);

        walletRepository.save(wallet);
    }
}
