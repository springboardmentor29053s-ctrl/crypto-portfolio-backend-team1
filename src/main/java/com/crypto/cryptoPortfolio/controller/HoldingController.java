package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.HoldingRequest;
import com.crypto.cryptoPortfolio.dto.HoldingResponse;
import com.crypto.cryptoPortfolio.entity.Exchange;
import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.ExchangeRepository;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final ExchangeRepository exchangeRepository;

    public HoldingController(HoldingRepository holdingRepository,
                             UserRepository userRepository,
                             ExchangeRepository exchangeRepository) {
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
        this.exchangeRepository = exchangeRepository;
    }

    // ✅ ADD MANUAL HOLDING
    @PostMapping("/manual")
    public Holding addManualHolding(@RequestBody HoldingRequest request,
                                    Principal principal) {

        if (request.getAssetSymbol() == null ||
                request.getAssetSymbol().trim().isEmpty()) {
            throw new RuntimeException("Asset symbol cannot be empty");
        }

        if (request.getQuantity() == null ||
                request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        if (request.getAvgCost() == null ||
                request.getAvgCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Average cost must be greater than 0");
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findById(request.getExchangeId())
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        Holding holding = new Holding();
        holding.setUser(user);
        holding.setExchange(exchange);
        holding.setAssetSymbol(request.getAssetSymbol().trim().toUpperCase());
        holding.setQuantity(request.getQuantity());
        holding.setAvgCost(request.getAvgCost());
        holding.setWalletType("MANUAL");
        holding.setIsManual(true); // ✅ FIXED: Mark as manual
        holding.setUpdatedAt(LocalDateTime.now());

        return holdingRepository.save(holding);
    }

    // ✅ UPDATE HOLDING
    @PutMapping("/{id}")
    public HoldingResponse updateHolding(@PathVariable Long id,
                                         @RequestBody HoldingRequest request,
                                         Authentication authentication) {

        String email = authentication.getName();

        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holding not found"));

        if (!holding.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        holding.setQuantity(request.getQuantity());
        holding.setAvgCost(request.getAvgCost());
        holding.setUpdatedAt(LocalDateTime.now());

        Holding saved = holdingRepository.save(holding);

        return new HoldingResponse(
                saved.getId(),
                saved.getAssetSymbol(),
                saved.getQuantity(),
                saved.getAvgCost()
        );
    }

    // ✅ DELETE HOLDING
    @DeleteMapping("/{id}")
    public String deleteHolding(@PathVariable Long id,
                                Principal principal) {

        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holding not found"));

        if (!holding.getUser().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Unauthorized");
        }

        holdingRepository.delete(holding);

        return "Holding deleted successfully";
    }
}