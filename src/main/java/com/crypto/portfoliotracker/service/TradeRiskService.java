package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.config.RiskThresholdConfig;
import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.TradeRiskLog;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.TradeRepository;
import com.crypto.portfoliotracker.repository.RiskAlertRepository;
import com.crypto.portfoliotracker.repository.TradeRiskLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing trade risks and creating alerts
 */
@Service
@Transactional
public class TradeRiskService {

    @Autowired
    private RiskThresholdConfig riskThresholdConfig;

    @Autowired
    private RiskDetectionService riskDetectionService;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private TradeRiskLogRepository tradeRiskLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Risk thresholds are now configurable via RiskThresholdConfig

    /**
     * Analyze trade for risk factors and create alerts if necessary
     */
    @Transactional
    public Map<String, Object> analyzeTradeRisk(Trade trade, User user) {
        Map<String, Object> riskAnalysis = new HashMap<>();
        boolean hasRiskAlert = false;
        String riskLevel = "LOW";
        String riskDetails = "";

        // Calculate trade value
        BigDecimal tradeValue = trade.getQuantity().multiply(trade.getPrice());

        // 1. Check trade value thresholds
        if (tradeValue.compareTo(riskThresholdConfig.getHighValueThreshold()) > 0) {
            riskLevel = "HIGH";
            hasRiskAlert = true;
            riskDetails = String.format("High-value trade detected: $%s for %s", 
                tradeValue.toString(), trade.getAssetSymbol());
            createTradeRiskAlert(user, trade, RiskAlert.AlertType.NEWS, riskDetails, "HIGH");
        } else if (tradeValue.compareTo(riskThresholdConfig.getMediumValueThreshold()) > 0) {
            if (riskLevel.equals("LOW")) riskLevel = "MEDIUM";
            hasRiskAlert = true;
            riskDetails = String.format("Medium-value trade: $%s for %s", 
                tradeValue.toString(), trade.getAssetSymbol());
            createTradeRiskAlert(user, trade, RiskAlert.AlertType.NEWS, riskDetails, "MEDIUM");
        }

        // 2. Check quantity thresholds
        if (trade.getQuantity().compareTo(riskThresholdConfig.getHighQuantityThreshold()) > 0) {
            riskLevel = "HIGH";
            hasRiskAlert = true;
            riskDetails = String.format("Large quantity trade: %s %s tokens", 
                trade.getQuantity().toString(), trade.getAssetSymbol());
            createTradeRiskAlert(user, trade, RiskAlert.AlertType.LIQUIDITY_RISK, riskDetails, "HIGH");
        } else if (trade.getQuantity().compareTo(riskThresholdConfig.getMediumQuantityThreshold()) > 0) {
            if (riskLevel.equals("LOW")) riskLevel = "MEDIUM";
            hasRiskAlert = true;
            riskDetails = String.format("Medium quantity trade: %s %s tokens", 
                trade.getQuantity().toString(), trade.getAssetSymbol());
            createTradeRiskAlert(user, trade, RiskAlert.AlertType.LIQUIDITY_RISK, riskDetails, "MEDIUM");
        }

        // 3. Check for scam token risk
        Map<String, Object> coinRiskAssessment = riskDetectionService.getCoinRiskAssessment(trade.getAssetSymbol());
        String coinRiskLevel = (String) coinRiskAssessment.get("riskLevel");
        
        if ("CRITICAL".equals(coinRiskLevel)) {
            riskLevel = "CRITICAL";
            hasRiskAlert = true;
            riskDetails = String.format("CRITICAL: %s is flagged as potential scam token! Trade value: $%s", 
                trade.getAssetSymbol(), tradeValue.toString());
            createTradeRiskAlert(user, trade, RiskAlert.AlertType.RUGPULL_WARNING, riskDetails, "CRITICAL");
        } else if ("HIGH".equals(coinRiskLevel)) {
            if (!"CRITICAL".equals(riskLevel)) riskLevel = "HIGH";
            hasRiskAlert = true;
            riskDetails = String.format("High risk token detected: %s. %s", 
                trade.getAssetSymbol(), coinRiskAssessment.get("alert"));
            createTradeRiskAlert(user, trade, RiskAlert.AlertType.CONTRACT_RISK, riskDetails, "HIGH");
        }

        // 4. Check user's recent trading patterns
        List<Trade> recentTrades = tradeRepository.findTop10ByUserOrderByExecutedAtDesc(user);
        if (recentTrades.size() > 5) {
            // Check for rapid trading pattern
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(riskThresholdConfig.getRapidTradeWindowMinutes());
            long recentTradesCount = recentTrades.stream()
                .filter(t -> t.getExecutedAt().isAfter(windowStart))
                .count();
            
            if (recentTradesCount > riskThresholdConfig.getRapidTradeCountThreshold()) {
                hasRiskAlert = true;
                riskDetails = String.format("Rapid trading detected: %d trades in %d minutes for %s", 
                    recentTradesCount, riskThresholdConfig.getRapidTradeWindowMinutes(), trade.getAssetSymbol());
                createTradeRiskAlert(user, trade, RiskAlert.AlertType.PRICE_VOLATILITY, riskDetails, "MEDIUM");
            }
        }

        // 5. Log the trade with risk assessment
        logTradeWithRisk(trade, user, riskLevel, hasRiskAlert, coinRiskAssessment);

        // Prepare response
        riskAnalysis.put("tradeId", trade.getId());
        riskAnalysis.put("riskLevel", riskLevel);
        riskAnalysis.put("hasRiskAlert", hasRiskAlert);
        riskAnalysis.put("tradeValue", tradeValue);
        riskAnalysis.put("coinRiskAssessment", coinRiskAssessment);
        riskAnalysis.put("timestamp", LocalDateTime.now());

        return riskAnalysis;
    }

    /**
     * Create a risk alert for a trade
     */
    private void createTradeRiskAlert(User user, Trade trade, RiskAlert.AlertType alertType, 
                                    String details, String severity) {
        String alertDetails = String.format("TRADE RISK ALERT - %s: %s\nTrade Details: %s %s at $%s (Value: $%s)", 
            severity, details, trade.getSide(), trade.getQuantity(), trade.getPrice(), 
            trade.getQuantity().multiply(trade.getPrice()));
        
        riskDetectionService.createRiskAlert(user, trade.getAssetSymbol(), alertType, alertDetails, severity);
    }

    /**
     * Log trade with risk assessment
     */
    private void logTradeWithRisk(Trade trade, User user, String riskLevel, boolean hasRiskAlert, Map<String, Object> coinRiskAssessment) {
        try {
            // Convert coin risk assessment to JSON string
            String coinRiskJson = objectMapper.writeValueAsString(coinRiskAssessment);
            
            // Create trade risk log entry
            TradeRiskLog riskLog = new TradeRiskLog(
                user, 
                trade, 
                riskLevel, 
                trade.getQuantity().multiply(trade.getPrice()), 
                hasRiskAlert, 
                String.format("Risk analysis for %s trade: %s", trade.getAssetSymbol(), riskLevel),
                coinRiskJson
            );
            
            tradeRiskLogRepository.save(riskLog);
            
            // Console logging for immediate visibility
            String logMessage = String.format("TRADE RISK LOG - User: %s, Asset: %s, Side: %s, " +
                "Quantity: %s, Price: $%s, Value: $%s, Risk Level: %s, Alert: %s, Time: %s",
                user.getEmail(), trade.getAssetSymbol(), trade.getSide(), trade.getQuantity(), 
                trade.getPrice(), trade.getQuantity().multiply(trade.getPrice()), riskLevel, 
                hasRiskAlert ? "YES" : "NO", LocalDateTime.now());
            
            System.out.println(logMessage);
        } catch (Exception e) {
            System.err.println("Error logging trade risk: " + e.getMessage());
            // Continue without failing the trade
        }
    }

    /**
     * Get daily risk summary for user
     */
    public Map<String, Object> getDailyRiskSummary(User user) {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<RiskAlert> todayAlerts = riskAlertRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), today);
        
        List<Trade> todayTrades = tradeRepository.findByUserAndExecutedAtAfterOrderByExecutedAtDesc(user, today);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("date", today.toLocalDate());
        summary.put("totalTrades", todayTrades.size());
        summary.put("totalAlerts", todayAlerts.size());
        summary.put("totalTradeValue", todayTrades.stream()
            .map(t -> t.getQuantity().multiply(t.getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        Map<String, Long> alertsByType = new HashMap<>();
        for (RiskAlert.AlertType type : RiskAlert.AlertType.values()) {
            long count = todayAlerts.stream().filter(a -> a.getAlertType() == type).count();
            alertsByType.put(type.name(), count);
        }
        summary.put("alertsByType", alertsByType);
        
        return summary;
    }

    /**
     * Get risk trends for the past 7 days
     */
    public Map<String, Object> getRiskTrends(User user) {
        Map<String, Object> trends = new HashMap<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        
        List<RiskAlert> weeklyAlerts = riskAlertRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), sevenDaysAgo);
        List<Trade> weeklyTrades = tradeRepository.findByUserAndExecutedAtAfterOrderByExecutedAtDesc(user, sevenDaysAgo);
        
        trends.put("weeklyTrades", weeklyTrades.size());
        trends.put("weeklyAlerts", weeklyAlerts.size());
        trends.put("riskRatio", weeklyTrades.size() > 0 ? (double) weeklyAlerts.size() / weeklyTrades.size() : 0.0);
        
        // Daily breakdown
        Map<String, Integer> dailyAlerts = new HashMap<>();
        Map<String, Integer> dailyTrades = new HashMap<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDateTime day = LocalDateTime.now().minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime nextDay = day.plusDays(1);
            
            int dayAlerts = (int) weeklyAlerts.stream()
                .filter(a -> a.getCreatedAt().isAfter(day) && a.getCreatedAt().isBefore(nextDay))
                .count();
            int dayTrades = (int) weeklyTrades.stream()
                .filter(t -> t.getExecutedAt().isAfter(day) && t.getExecutedAt().isBefore(nextDay))
                .count();
            
            String dayKey = day.toLocalDate().toString();
            dailyAlerts.put(dayKey, dayAlerts);
            dailyTrades.put(dayKey, dayTrades);
        }
        
        trends.put("dailyAlerts", dailyAlerts);
        trends.put("dailyTrades", dailyTrades);
        
        return trends;
    }
}
