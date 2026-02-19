package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.DashboardLiveResponse;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.DashboardService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    public DashboardController(DashboardService dashboardService,
                               UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/live")
    public DashboardLiveResponse getLiveDashboard(Authentication authentication) {

        // Extract email from JWT
        String email = authentication.getName();

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch dashboard for logged-in user only
        return dashboardService.getLiveDashboard(user.getId());

    }

}
