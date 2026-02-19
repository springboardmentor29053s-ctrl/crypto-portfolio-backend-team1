package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.HoldingRequest;
import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    public HoldingController(HoldingRepository holdingRepository,
                             UserRepository userRepository) {
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public Holding addHolding(@RequestBody HoldingRequest request,
                              Authentication authentication) {
        System.out.println("Controller reached");
        System.out.println("User from JWT: " + authentication.getName());
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Holding holding = new Holding();

        holding.setAssetSymbol(request.getAssetSymbol());
        holding.setQuantity(request.getQuantity());
        holding.setAvgCost(request.getAvgCost());
        holding.setWalletType(request.getWalletType());
        holding.setUser(user);

        return holdingRepository.save(holding);
    }
}
