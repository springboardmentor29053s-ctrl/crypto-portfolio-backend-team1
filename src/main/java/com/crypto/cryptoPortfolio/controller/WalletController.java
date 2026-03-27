package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.entity.WalletTransaction;
import com.crypto.cryptoPortfolio.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Get wallet balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(Principal principal) {
        BigDecimal balance = walletService.getBalance(principal.getName());

        Map<String, BigDecimal> response = new HashMap<>();
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    /**
     * Deposit funds
     */
    @PostMapping("/deposit")
    public ResponseEntity<WalletTransaction> deposit(
            @RequestBody Map<String, BigDecimal> request,
            Principal principal) {

        BigDecimal amount = request.get("amount");
        WalletTransaction transaction = walletService.deposit(principal.getName(), amount);

        return ResponseEntity.ok(transaction);
    }

    /**
     * Withdraw funds
     */
    @PostMapping("/withdraw")
    public ResponseEntity<WalletTransaction> withdraw(
            @RequestBody Map<String, BigDecimal> request,
            Principal principal) {

        BigDecimal amount = request.get("amount");
        WalletTransaction transaction = walletService.withdraw(principal.getName(), amount);

        return ResponseEntity.ok(transaction);
    }

    /**
     * Get transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactions(Principal principal) {
        List<WalletTransaction> transactions = walletService.getTransactions(principal.getName());
        return ResponseEntity.ok(transactions);
    }
}