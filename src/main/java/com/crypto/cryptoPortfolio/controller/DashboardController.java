package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.DashboardLiveResponse;
import com.crypto.cryptoPortfolio.dto.DashboardSummaryResponse;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.DashboardService;

import com.crypto.cryptoPortfolio.service.PortfolioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final PortfolioService portfolioService;

    public DashboardController(DashboardService dashboardService,
                               UserRepository userRepository, PortfolioService portfolioService) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
        this.portfolioService=portfolioService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Get summary FIRST (computes live prices)
        DashboardSummaryResponse summary = dashboardService.getDashboard(user.getId());

        // ✅ THEN capture snapshot (now has real value)
        portfolioService.captureSnapshotForUser(user);

        return summary;
    }
}
