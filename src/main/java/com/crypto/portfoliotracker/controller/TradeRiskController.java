package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.service.TradeRiskService;
import com.crypto.portfoliotracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trade-risk")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TradeRiskController {

    @Autowired
    private TradeRiskService tradeRiskService;

    @Autowired
    private UserService userService;

    /**
     * Analyze a trade for risk factors (called automatically when trades are made)
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeTradeRisk(@RequestBody Map<String, Object> tradeData, 
                                            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            
            // Create a trade object for analysis
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
     * Get trade risk summary for user
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTradeRiskSummary(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            
            Map<String, Object> summary = tradeRiskService.getTradeRiskSummary(user);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get risk recommendations for user
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRiskRecommendations(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            
            Map<String, Object> recommendations = tradeRiskService.getRiskRecommendations(user);
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current user ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.getUserByEmail(userDetails.getUsername());
            return user.getId();
        }
        return null;
    }
}
