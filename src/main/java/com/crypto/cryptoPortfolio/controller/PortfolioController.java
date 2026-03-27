package com.crypto.cryptoPortfolio.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import com.crypto.cryptoPortfolio.dto.PerformanceResponse;
import com.crypto.cryptoPortfolio.dto.ProfitLossResponse;
import com.crypto.cryptoPortfolio.entity.Exchange;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.ExchangeRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.crypto.cryptoPortfolio.dto.HoldingResponse;
import com.crypto.cryptoPortfolio.service.PortfolioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;

    @GetMapping("/holdings")
    public List<HoldingResponse> getHoldings(
            @RequestParam Integer exchangeId,
            Principal principal) {

        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        return portfolioService.getHoldingsFIFO(
                principal.getName(),
                exchange
        );
    }

    @GetMapping("/performance")
    public List<PerformanceResponse> getPerformance(
            @RequestParam int days,
            Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow();

        return portfolioService.getPerformance(
                user.getId(),
                days
        );
    }

    // ✅ NEW: Manual snapshot trigger for testing
    @PostMapping("/snapshot")
    public ResponseEntity<String> captureSnapshot(Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        portfolioService.captureSnapshotForUser(user);

        return ResponseEntity.ok("Snapshot captured successfully");
    }

    // ✅ NEW: Capture multiple snapshots for testing (simulates past days)
    @PostMapping("/snapshot/simulate")
    public ResponseEntity<String> simulateSnapshots(
            @RequestParam(defaultValue = "7") int days,
            Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        portfolioService.simulateHistoricalSnapshots(user, days);

        return ResponseEntity.ok("Simulated " + days + " days of snapshots");
    }

    @GetMapping("/pnl")
    public ProfitLossResponse getProfitAndLoss(Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return portfolioService.getProfitAndLoss(user.getId());
    }

    @GetMapping("/value")
    public BigDecimal getTotalValue(Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow();

        return portfolioService.calculateTotalPortfolioValue(user.getId());
    }
}