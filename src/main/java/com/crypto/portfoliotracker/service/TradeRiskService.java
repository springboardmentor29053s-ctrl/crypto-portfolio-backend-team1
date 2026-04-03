package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class TradeRiskService {

    /**
     * Analyze trade risk
     */
    public Map<String, Object> analyzeTradeRisk(Trade trade, User user) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // Basic risk factors
            BigDecimal tradeValue = trade.getPrice().multiply(trade.getQuantity());
            String assetSymbol = trade.getAssetSymbol();
            
            // Risk scoring
            int riskScore = calculateTradeRiskScore(trade, tradeValue);
            
            analysis.put("tradeId", trade.getId());
            analysis.put("assetSymbol", assetSymbol);
            analysis.put("tradeValue", tradeValue);
            analysis.put("riskScore", riskScore);
            analysis.put("riskLevel", getRiskLevel(riskScore));
            analysis.put("recommendations", getTradeRecommendations(riskScore, trade.getSide()));
            analysis.put("timestamp", new Date());
            
        } catch (Exception e) {
            analysis.put("error", "Failed to analyze trade risk");
            analysis.put("message", e.getMessage());
        }
        
        return analysis;
    }

    /**
     * Calculate trade risk score
     */
    private int calculateTradeRiskScore(Trade trade, BigDecimal tradeValue) {
        int score = 0;
        
        // Trade size risk
        if (tradeValue.compareTo(new BigDecimal("10000")) > 0) {
            score += 30; // Large trade
        } else if (tradeValue.compareTo(new BigDecimal("1000")) > 0) {
            score += 15; // Medium trade
        }
        
        // Asset type risk
        String assetSymbol = trade.getAssetSymbol().toUpperCase();
        if (isHighVolatilityAsset(assetSymbol)) {
            score += 25;
        } else if (isMediumVolatilityAsset(assetSymbol)) {
            score += 15;
        }
        
        // Trade side risk
        if (trade.getSide() == Trade.TradeSide.SELL) {
            score += 10; // Selling has slightly different risk profile
        }
        
        return Math.min(score, 100);
    }

    /**
     * Check if asset is high volatility
     */
    private boolean isHighVolatilityAsset(String symbol) {
        List<String> highVolatilityAssets = Arrays.asList(
            "BTC", "ETH", "SOL", "AVAX", "DOT", "SHIB", "DOGE"
        );
        return highVolatilityAssets.contains(symbol);
    }

    /**
     * Check if asset is medium volatility
     */
    private boolean isMediumVolatilityAsset(String symbol) {
        List<String> mediumVolatilityAssets = Arrays.asList(
            "BNB", "ADA", "LINK", "UNI", "MATIC", "ATOM", "LTC"
        );
        return mediumVolatilityAssets.contains(symbol);
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
     * Get trade recommendations
     */
    private List<String> getTradeRecommendations(int score, Trade.TradeSide side) {
        List<String> recommendations = new ArrayList<>();
        
        if (score >= 70) {
            recommendations.add("Consider using limit orders");
            recommendations.add("Set tight stop-loss");
            recommendations.add("Monitor price closely");
        } else if (score >= 40) {
            recommendations.add("Consider partial position sizing");
            recommendations.add("Set wider stop-loss");
        } else {
            recommendations.add("Consider market orders");
            recommendations.add("Long-term holding strategy");
        }
        
        if (side == Trade.TradeSide.BUY) {
            recommendations.add("Dollar-cost averaging recommended");
        } else {
            recommendations.add("Take profit at resistance levels");
        }
        
        return recommendations;
    }

    /**
     * Get trade risk summary for user
     */
    public Map<String, Object> getTradeRiskSummary(User user) {
        Map<String, Object> summary = new HashMap<>();
        
        // Placeholder implementation - would query actual trade data
        summary.put("totalTrades", 0);
        summary.put("averageRiskScore", 25);
        summary.put("highRiskTrades", 0);
        summary.put("mediumRiskTrades", 0);
        summary.put("lowRiskTrades", 0);
        summary.put("timestamp", new Date());
        
        return summary;
    }

    /**
     * Get risk recommendations for user
     */
    public Map<String, Object> getRiskRecommendations(User user) {
        Map<String, Object> recommendations = new HashMap<>();
        
        recommendations.put("portfolio", Arrays.asList(
            "Diversify across different asset classes",
            "Consider stablecoin allocation",
            "Set stop-loss orders",
            "Regular portfolio rebalancing"
        ));
        
        recommendations.put("trading", Arrays.asList(
            "Use limit orders",
            "Monitor market volatility",
            "Take profit at key levels"
        ));
        
        recommendations.put("timestamp", new Date());
        
        return recommendations;
    }
}
