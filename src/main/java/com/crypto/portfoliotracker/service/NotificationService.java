package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.RiskAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class NotificationService {
    
    @Autowired
    private RiskAlertRepository riskAlertRepository;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    /**
     * Send notification to user via email
     */
    public void sendNotification(User user, RiskAlert alert) {
        // Send email notification
        String subject = String.format("Risk Alert: %s - %s", alert.getAssetSymbol(), alert.getAlertType());
        String message = String.format(
            "A new risk alert has been detected for %s:\n\n" +
            "Type: %s\n" +
            "Details: %s\n" +
            "Time: %s",
            alert.getAssetSymbol(),
            alert.getAlertType().toString(),
            alert.getDetails(),
            alert.getCreatedAt()
        );
        
        if (emailService != null) {
            emailService.sendEmail(user.getEmail(), subject, message);
        }
        
        // Send email notification if user has email notifications enabled
        CompletableFuture.runAsync(() -> {
            try {
                if (emailService != null) {
                    emailService.sendRiskAlertEmail(user, alert);
                }
            } catch (Exception e) {
                System.err.println("Failed to send email notification: " + e.getMessage());
            }
        });
    }
    
    /**
     * Send price threshold notification
     */
    public void sendPriceThresholdNotification(User user, String assetSymbol, 
                                            double currentPrice, double threshold, 
                                            boolean isAboveThreshold) {
        
        String direction = isAboveThreshold ? "above" : "below";
        String message = String.format("%s price has gone %s threshold of $%.2f. Current price: $%.2f", 
            assetSymbol, direction, threshold, currentPrice);
        
        RiskAlert alert = RiskAlert.builder()
            .user(user)
            .assetSymbol(assetSymbol)
            .alertType(RiskAlert.AlertType.PRICE_VOLATILITY)
            .details(message)
            .seen(false)
            .build();
        
        riskAlertRepository.save(alert);
        sendNotification(user, alert);
    }
    
    /**
     * Send liquidity risk notification
     */
    public void sendLiquidityRiskNotification(User user, String assetSymbol, 
                                           double liquidityChange, String reason) {
        
        String message = String.format("Liquidity risk detected for %s. Change: %.2f%%. Reason: %s", 
            assetSymbol, liquidityChange, reason);
        
        RiskAlert alert = RiskAlert.builder()
            .user(user)
            .assetSymbol(assetSymbol)
            .alertType(RiskAlert.AlertType.LIQUIDITY_RISK)
            .details(message)
            .seen(false)
            .build();
        
        riskAlertRepository.save(alert);
        sendNotification(user, alert);
    }
    
    /**
     * Send holder concentration notification
     */
    public void sendHolderConcentrationNotification(User user, String assetSymbol, 
                                                  int holderCount, double topHolderPercentage) {
        
        String message = String.format("Holder concentration risk for %s. Total holders: %d, Top 10 holders control %.2f%%", 
            assetSymbol, holderCount, topHolderPercentage);
        
        RiskAlert alert = RiskAlert.builder()
            .user(user)
            .assetSymbol(assetSymbol)
            .alertType(RiskAlert.AlertType.HOLDER_CONCENTRATION)
            .details(message)
            .seen(false)
            .build();
        
        riskAlertRepository.save(alert);
        sendNotification(user, alert);
    }
    
    /**
     * Send contract risk notification
     */
    public void sendContractRiskNotification(User user, String assetSymbol, 
                                          String contractAddress, String riskFactors) {
        
        String message = String.format("Contract risk detected for %s (%s). Risk factors: %s", 
            assetSymbol, contractAddress, riskFactors);
        
        RiskAlert alert = RiskAlert.builder()
            .user(user)
            .assetSymbol(assetSymbol)
            .alertType(RiskAlert.AlertType.CONTRACT_RISK)
            .details(message)
            .seen(false)
            .build();
        
        riskAlertRepository.save(alert);
        sendNotification(user, alert);
    }
    
    /**
     * Send rug pull warning
     */
    public void sendRugPullWarning(User user, String assetSymbol, String evidence) {
        
        String message = String.format("RUG PULL WARNING for %s! Evidence: %s", 
            assetSymbol, evidence);
        
        RiskAlert alert = RiskAlert.builder()
            .user(user)
            .assetSymbol(assetSymbol)
            .alertType(RiskAlert.AlertType.RUGPULL_WARNING)
            .details(message)
            .seen(false)
            .build();
        
        riskAlertRepository.save(alert);
        sendNotification(user, alert);
    }
    
    /**
     * Batch send notifications for multiple users
     */
    public void sendBulkNotifications(List<User> users, RiskAlert.AlertType alertType, 
                                    String message, String severity) {
        
        for (User user : users) {
            RiskAlert alert = RiskAlert.builder()
            .user(user)
            .assetSymbol("BULK_ALERT")
            .alertType(alertType)
            .details(message)
            .seen(false)
            .build();
            
            riskAlertRepository.save(alert);
            sendNotification(user, alert);
        }
    }
    
    /**
     * Send periodic risk summary
     */
    public void sendRiskSummary(User user) {
        List<RiskAlert> recentAlerts = riskAlertRepository.findByUserOrderByCreatedAtDesc(user);
        
        // Get only alerts from last 24 hours
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        recentAlerts.removeIf(alert -> alert.getCreatedAt().isBefore(yesterday));
        
        if (!recentAlerts.isEmpty()) {
            String summary = String.format("Daily Risk Summary: %d alerts in the last 24 hours.", 
                recentAlerts.size());
            
            RiskAlert summaryAlert = RiskAlert.builder()
                .user(user)
                .assetSymbol("SUMMARY")
                .alertType(RiskAlert.AlertType.NEWS)
                .details(summary)
                .seen(false)
                .build();
            
            riskAlertRepository.save(summaryAlert);
            sendNotification(user, summaryAlert);
        }
    }
    
    /**
     * Mark notification as read
     */
    public void markNotificationAsRead(Long alertId) {
        RiskAlert alert = riskAlertRepository.findById(alertId).orElse(null);
        if (alert != null) {
            alert.setSeen(true);
            riskAlertRepository.save(alert);
        }
    }
    
    /**
     * Get unread notifications count for user
     */
    public long getUnreadNotificationsCount(User user) {
        return riskAlertRepository.countByUserAndSeenFalse(user);
    }
}
