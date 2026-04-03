package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.*;
import com.crypto.portfoliotracker.repository.*;
import com.crypto.portfoliotracker.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PortfolioController {

    @Autowired
    private PortfolioServiceImplementation portfolioService;

    @Autowired
    private UserService userService;

    @Autowired
    private RiskDetectionService riskDetectionService;

    /**
     * Get user's complete portfolio
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserPortfolio(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Holding> holdings = portfolioService.getUserHoldings(user);
            List<Trade> trades = portfolioService.getUserTrades(user);
            
            Map<String, Object> portfolio = new HashMap<>();
            portfolio.put("holdings", holdings);
            portfolio.put("trades", trades);
            portfolio.put("totalValue", calculateTotalValue(holdings));
            portfolio.put("totalInvested", calculateTotalInvested(trades));
            portfolio.put("lastUpdated", LocalDateTime.now());
            
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get portfolio holdings
     */
    @GetMapping("/holdings")
    public ResponseEntity<List<Holding>> getHoldings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Holding> holdings = portfolioService.getUserHoldings(user);
            return ResponseEntity.ok(holdings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get portfolio trades
     */
    @GetMapping("/trades")
    public ResponseEntity<List<Trade>> getTrades(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Trade> trades = portfolioService.getUserTrades(user);
            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add new trade to portfolio
     */
    @PostMapping("/trades")
    public ResponseEntity<Trade> addTrade(@RequestBody Trade trade, 
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            trade.setUser(user);
            trade.setExecutedAt(LocalDateTime.now());
            
            Trade savedTrade = portfolioService.saveTrade(trade);
            
            // Analyze trade risk
            Map<String, Object> riskAnalysis = riskDetectionService.analyzeTradeRisk(trade, user);
            
            return ResponseEntity.ok(savedTrade);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing trade
     */
    @PutMapping("/trades/{id}")
    public ResponseEntity<Trade> updateTrade(@PathVariable Long id, 
                                           @RequestBody Trade tradeUpdate,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            Trade existingTrade = portfolioService.getTradeById(id);
            
            if (existingTrade == null || !existingTrade.getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            // Update allowed fields
            if (tradeUpdate.getQuantity() != null) {
                existingTrade.setQuantity(tradeUpdate.getQuantity());
            }
            if (tradeUpdate.getPrice() != null) {
                existingTrade.setPrice(tradeUpdate.getPrice());
            }
            if (tradeUpdate.getSide() != null) {
                existingTrade.setSide(tradeUpdate.getSide());
            }
            
            Trade updatedTrade = portfolioService.saveTrade(existingTrade);
            return ResponseEntity.ok(updatedTrade);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete trade
     */
    @DeleteMapping("/trades/{id}")
    public ResponseEntity<Void> deleteTrade(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            Trade trade = portfolioService.getTradeById(id);
            
            if (trade == null || !trade.getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            portfolioService.deleteTrade(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get portfolio summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Holding> holdings = portfolioService.getUserHoldings(user);
            List<Trade> trades = portfolioService.getUserTrades(user);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalHoldings", holdings.size());
            summary.put("totalTrades", trades.size());
            summary.put("totalValue", calculateTotalValue(holdings));
            summary.put("totalInvested", calculateTotalInvested(trades));
            summary.put("totalProfitLoss", calculateTotalProfitLoss(holdings, trades));
            summary.put("topHoldings", getTopHoldings(holdings, 5));
            summary.put("lastUpdated", LocalDateTime.now());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Calculate total portfolio value
     */
    private BigDecimal calculateTotalValue(List<Holding> holdings) {
        return holdings.stream()
                .map(holding -> holding.getCurrentPrice().multiply(holding.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total invested amount
     */
    private BigDecimal calculateTotalInvested(List<Trade> trades) {
        return trades.stream()
                .filter(trade -> trade.getSide() == Trade.TradeSide.BUY)
                .map(trade -> trade.getPrice().multiply(trade.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total profit/loss
     */
    private BigDecimal calculateTotalProfitLoss(List<Holding> holdings, List<Trade> trades) {
        BigDecimal currentValue = calculateTotalValue(holdings);
        BigDecimal totalInvested = calculateTotalInvested(trades);
        return currentValue.subtract(totalInvested);
    }

    /**
     * Get top holdings by value
     */
    private List<Map<String, Object>> getTopHoldings(List<Holding> holdings, int limit) {
        return holdings.stream()
                .sorted((h1, h2) -> h2.getCurrentPrice().multiply(h2.getQuantity())
                        .compareTo(h1.getCurrentPrice().multiply(h1.getQuantity())) > 0 ? 1 : -1)
                .limit(limit)
                .map(holding -> {
                    Map<String, Object> holdingMap = new HashMap<>();
                    holdingMap.put("symbol", holding.getAssetSymbol());
                    holdingMap.put("quantity", holding.getQuantity());
                    holdingMap.put("currentPrice", holding.getCurrentPrice());
                    holdingMap.put("value", holding.getCurrentPrice().multiply(holding.getQuantity()));
                    holdingMap.put("profitLoss", calculateHoldingProfitLoss(holding));
                    return holdingMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate profit/loss for a holding
     */
    private BigDecimal calculateHoldingProfitLoss(Holding holding) {
        // This would require average buy price calculation
        // Simplified implementation
        return BigDecimal.ZERO;
    }
}
