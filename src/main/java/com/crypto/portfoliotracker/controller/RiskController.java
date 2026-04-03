package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.service.RiskDetectionService;
import com.crypto.portfoliotracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

@RestController
@RequestMapping("/api/risk")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class RiskController {

    @Autowired
    private RiskDetectionService riskDetectionService;

    @Autowired
    private UserService userService;

    /**
     * Get all risk alerts for user
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<RiskAlert>> getRiskAlerts(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<RiskAlert> alerts = riskDetectionService.getRiskAlertsForUser(user);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Analyze portfolio risk
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzePortfolioRisk(
            @RequestBody Map<String, Object> portfolioData,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            
            Map<String, Object> riskAnalysis = riskDetectionService.analyzePortfolioRisk(
                portfolioData, user);
            
            List<Map<String, Object>> riskAlerts = new ArrayList<>();
            Map<String, Object> recommendations = new HashMap<>();
            recommendations.put("portfolio", Arrays.asList(
                "Diversify across different asset classes",
                "Consider stablecoin allocation", 
                "Set stop-loss orders",
                "Regular portfolio rebalancing"
            ));
            riskAlerts.add(recommendations);
            riskAnalysis.put("riskAlerts", riskAlerts);
            
            return ResponseEntity.ok(riskAnalysis);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to analyze portfolio risk");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Check specific coin risk
     */
    @PostMapping("/check-coin")
    public ResponseEntity<Map<String, Object>> checkCoinRisk(
            @RequestBody Map<String, Object> coinData) {
        try {
            String symbol = (String) coinData.get("symbol");
            Map<String, Object> riskCheck = riskDetectionService.checkCoinRisk(symbol);
            
            return ResponseEntity.ok(riskCheck);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to check coin risk");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get risk score for portfolio
     */
    @GetMapping("/score")
    public ResponseEntity<Map<String, Object>> getRiskScore(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            Map<String, Object> riskScore = riskDetectionService.calculateRiskScore(user);
            
            return ResponseEntity.ok(riskScore);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to calculate risk score");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get market risk indicators
     */
    @GetMapping("/market-indicators")
    public ResponseEntity<Map<String, Object>> getMarketRiskIndicators() {
        try {
            Map<String, Object> indicators = riskDetectionService.getMarketRiskIndicators();
            return ResponseEntity.ok(indicators);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch market risk indicators");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
