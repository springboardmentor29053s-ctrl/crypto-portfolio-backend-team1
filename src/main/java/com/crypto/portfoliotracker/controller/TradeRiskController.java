package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.service.TradeRiskService;
import com.crypto.portfoliotracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for trade risk analysis and notifications
 */
@RestController
@RequestMapping("/api/trade-risk")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://10.14.189.34:3000"})
public class TradeRiskController {

    @Autowired
    private TradeRiskService tradeRiskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }

    /**
     * Analyze a trade for risk factors (called automatically when trades are made)
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeTradeRisk(@RequestBody Map<String, Object> tradeData, 
                                            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            
            // Create a mock trade object for analysis
            Trade trade = new Trade();
            trade.setAssetSymbol((String) tradeData.get("assetSymbol"));
            trade.setQuantity(new java.math.BigDecimal(tradeData.get("quantity").toString()));
            trade.setPrice(new java.math.BigDecimal(tradeData.get("price").toString()));
            trade.setSide(Trade.TradeSide.valueOf((String) tradeData.get("side")));
            trade.setExecutedAt(java.time.LocalDateTime.now());
            
            Map<String, Object> riskAnalysis = tradeRiskService.analyzeTradeRisk(trade, user);
            
            return ResponseEntity.ok(riskAnalysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get daily risk summary for the user
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<?> getDailyRiskSummary(Authentication authentication) {
        try {
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            Map<String, Object> summary = tradeRiskService.getDailyRiskSummary(user);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get risk trends for the past 7 days
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getRiskTrends(Authentication authentication) {
        try {
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            Map<String, Object> trends = tradeRiskService.getRiskTrends(user);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get risk configuration thresholds
     */
    @GetMapping("/thresholds")
    public ResponseEntity<?> getRiskThresholds() {
        try {
            Map<String, Object> thresholds = new HashMap<>();
            thresholds.put("highValueThreshold", "10000");
            thresholds.put("mediumValueThreshold", "5000");
            thresholds.put("highQuantityThreshold", "1000");
            thresholds.put("mediumQuantityThreshold", "500");
            thresholds.put("rapidTradeThreshold", "3");
            thresholds.put("rapidTradeWindowMinutes", "5");
            return ResponseEntity.ok(thresholds);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get trade risk statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getTradeRiskStatistics(Authentication authentication) {
        try {
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            
            // Combine daily summary and trends for comprehensive statistics
            Map<String, Object> dailySummary = tradeRiskService.getDailyRiskSummary(user);
            Map<String, Object> trends = tradeRiskService.getRiskTrends(user);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("today", dailySummary);
            statistics.put("weekly", trends);
            
            Map<String, Object> riskThresholds = new HashMap<>();
            riskThresholds.put("highValue", "$10,000");
            riskThresholds.put("mediumValue", "$5,000");
            riskThresholds.put("highQuantity", "1,000 tokens");
            riskThresholds.put("mediumQuantity", "500 tokens");
            statistics.put("riskThresholds", riskThresholds);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
