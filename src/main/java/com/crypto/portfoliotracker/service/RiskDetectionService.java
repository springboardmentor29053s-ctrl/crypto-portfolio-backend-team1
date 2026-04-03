package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.ScamToken;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.repository.ScamTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RiskDetectionService {

    @Autowired
    private ScamTokenRepository scamTokenRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Check if a token is a known scam token
     */
    public Map<String, Object> checkCoinRisk(String symbol) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check against known scam tokens
            List<ScamToken> scamTokens = scamTokenRepository.findByIsActiveTrue();
            
            boolean isScam = scamTokens.stream()
                    .anyMatch(token -> token.getTokenSymbol().equalsIgnoreCase(symbol));
            
            result.put("symbol", symbol);
            result.put("isScam", isScam);
            result.put("riskLevel", isScam ? "HIGH" : "LOW");
            result.put("timestamp", new Date());
            
            if (isScam) {
                ScamToken scamToken = scamTokens.stream()
                        .filter(token -> token.getTokenSymbol().equalsIgnoreCase(symbol))
                        .findFirst()
                        .orElse(null);
                
                if (scamToken != null) {
                    result.put("reason", scamToken.getReason());
                    result.put("source", scamToken.getSource());
                    result.put("confidenceScore", scamToken.getConfidenceScore());
                }
            }
            
        } catch (Exception e) {
            result.put("error", "Failed to check coin risk");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * Analyze portfolio risk
     */
    public Map<String, Object> analyzePortfolioRisk(Map<String, Object> portfolioData, User user) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // Basic risk analysis
            double totalValue = ((Number) portfolioData.getOrDefault("totalValue", 0)).doubleValue();
            int coinCount = ((List<?>) portfolioData.getOrDefault("coins", Collections.emptyList())).size();
            
            // Risk scoring
            int riskScore = calculateBasicRiskScore(totalValue, coinCount);
            
            analysis.put("riskScore", riskScore);
            analysis.put("riskLevel", getRiskLevel(riskScore));
            analysis.put("totalValue", totalValue);
            analysis.put("coinCount", coinCount);
            analysis.put("recommendations", getRecommendations(riskScore));
            analysis.put("timestamp", new Date());
            
        } catch (Exception e) {
            analysis.put("error", "Failed to analyze portfolio risk");
            analysis.put("message", e.getMessage());
        }
        
        return analysis;
    }

    /**
     * Calculate basic risk score
     */
    private int calculateBasicRiskScore(double totalValue, int coinCount) {
        int score = 0;
        
        // Value concentration risk
        if (coinCount == 1) {
            score += 30; // High concentration
        } else if (coinCount <= 3) {
            score += 15; // Medium concentration
        }
        
        // Portfolio size risk
        if (totalValue < 1000) {
            score += 20; // Small portfolio
        } else if (totalValue < 10000) {
            score += 10; // Medium portfolio
        }
        
        return Math.min(score, 100);
    }

    /**
     * Get risk level based on score
     */
    private String getRiskLevel(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get recommendations based on risk score
     */
    private List<String> getRecommendations(int score) {
        List<String> recommendations = new ArrayList<>();
        
        if (score >= 70) {
            recommendations.add("Consider diversifying your portfolio");
            recommendations.add("Set stop-loss orders");
            recommendations.add("Monitor market volatility closely");
        } else if (score >= 40) {
            recommendations.add("Consider adding stablecoins");
            recommendations.add("Regular portfolio rebalancing");
        } else {
            recommendations.add("Continue dollar-cost averaging");
            recommendations.add("Consider long-term holding strategy");
        }
        
        return recommendations;
    }

    // Placeholder methods for interface compatibility
    public List<RiskAlert> getRiskAlertsForUser(User user) {
        return new ArrayList<>();
    }

    public Map<String, Object> analyzeTradeRisk(Trade trade, User user) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("riskScore", 25);
        analysis.put("riskLevel", "LOW");
        analysis.put("recommendations", Arrays.asList("Monitor price", "Set stop-loss"));
        return analysis;
    }

    public Map<String, Object> calculateRiskScore(User user) {
        return Map.of("score", 25, "level", "LOW");
    }

    public Map<String, Object> getMarketRiskIndicators() {
        return Map.of("marketSentiment", "NEUTRAL", "volatility", "MEDIUM");
    }

    public Map<String, Object> getTradeRiskSummary(User user) {
        return Map.of("totalTrades", 0, "riskScore", 25);
    }

    public Map<String, Object> getRiskRecommendations(User user) {
        return Map.of("recommendations", List.of("Diversify portfolio", "Set stop-loss"));
    }
}
