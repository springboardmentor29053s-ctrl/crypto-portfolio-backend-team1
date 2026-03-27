package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.TradeRequest;
import com.crypto.cryptoPortfolio.dto.TradeResponse;
import com.crypto.cryptoPortfolio.entity.Trade;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final UserRepository userRepository;

    /**
     * Place a BUY or SELL order
     * POST /api/trade/order
     */
    @PostMapping("/order")
    public ResponseEntity<TradeResponse> placeOrder(@RequestBody TradeRequest request) {

        // Get authentication from SecurityContext
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        // Call service (returns TradeResponse directly)
        TradeResponse response = tradeService.placeOrder(request, email);

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's trade history
     * GET /api/trade/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<TradeResponse>> getTradeHistory() {

        // Get authentication from SecurityContext
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get trades from service
        List<Trade> trades = tradeService.getTradeHistory(user);

        // Map to TradeResponse DTOs
        List<TradeResponse> response = trades.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to convert Trade entity to TradeResponse DTO
     */
    private TradeResponse mapToResponse(Trade trade) {
        TradeResponse response = new TradeResponse();
        response.setId(trade.getId());
        response.setAssetSymbol(trade.getAssetSymbol());
        response.setSide(trade.getSide().name());
        response.setQuantity(trade.getQuantity());
        response.setPrice(trade.getPrice());
        response.setFee(trade.getFee());
        response.setExchangeName(trade.getExchange().getName());
        response.setExecutedAt(trade.getExecutedAt());

        // Handle null realizedProfit (for BUY trades)
        response.setRealizedProfit(
                trade.getRealizedProfit() != null
                        ? trade.getRealizedProfit()
                        : BigDecimal.ZERO
        );

        return response;
    }
}