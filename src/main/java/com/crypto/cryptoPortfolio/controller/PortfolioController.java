package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.PortfolioResponse;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.PortfolioService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    public PortfolioController(PortfolioService portfolioService,
                               UserRepository userRepository) {
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PortfolioResponse> getPortfolio(Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException("Authentication is NULL");
        }

        System.out.println("Authenticated user: " + authentication.getName());

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        return portfolioService.getPortfolio(user);

    }
}
