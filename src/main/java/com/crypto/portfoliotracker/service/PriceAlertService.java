package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.PriceAlert;
import com.crypto.portfoliotracker.entity.PriceSnapshot;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.PriceAlertRepository;
import com.crypto.portfoliotracker.repository.PriceSnapshotRepository;
import com.crypto.portfoliotracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PriceAlertService {

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmailService emailService;

    // Check price alerts every 2 minutes
    @Scheduled(fixedRate = 120000) // 2 minutes
    @Transactional
    public void checkPriceAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findAllActiveAlerts();
        
        for (PriceAlert alert : activeAlerts) {
            try {
                checkAlert(alert);
            } catch (Exception e) {
                System.err.println("Error checking alert " + alert.getId() + ": " + e.getMessage());
            }
        }
    }

    private void checkAlert(PriceAlert alert) {
        // Get latest price for the asset
        List<PriceSnapshot> latestSnapshots = priceSnapshotRepository
            .findByAssetSymbolOrderByCapturedAtDesc(alert.getAssetSymbol(), 
                org.springframework.data.domain.PageRequest.of(0, 1));
        
        if (latestSnapshots.isEmpty()) {
            return;
        }

        BigDecimal currentPrice = latestSnapshots.get(0).getPriceUsd();
        BigDecimal threshold = alert.getThresholdValue();
        LocalDateTime lastTriggered = alert.getLastTriggered();

        boolean shouldTrigger = false;
        String message = "";

        switch (alert.getAlertType()) {
            case PRICE_ABOVE:
                shouldTrigger = currentPrice.compareTo(threshold) > 0 && 
                    (lastTriggered == null || lastTriggered.isBefore(LocalDateTime.now().minusMinutes(5)));
                message = String.format("Price Alert: %s is above $%s (Current: $%s)", 
                    alert.getAssetSymbol(), threshold, currentPrice);
                break;

            case PRICE_BELOW:
                shouldTrigger = currentPrice.compareTo(threshold) < 0 && 
                    (lastTriggered == null || lastTriggered.isBefore(LocalDateTime.now().minusMinutes(5)));
                message = String.format("Price Alert: %s is below $%s (Current: $%s)", 
                    alert.getAssetSymbol(), threshold, currentPrice);
                break;

            case PERCENTAGE_INCREASE:
                if (alert.getThresholdPercentage() != null) {
                    BigDecimal increaseThreshold = threshold.multiply(
                        BigDecimal.ONE.add(alert.getThresholdPercentage().divide(BigDecimal.valueOf(100))));
                    shouldTrigger = currentPrice.compareTo(increaseThreshold) > 0 && 
                        (lastTriggered == null || lastTriggered.isBefore(LocalDateTime.now().minusMinutes(5)));
                    message = String.format("Price Alert: %s increased by %.1f%% (Current: $%s)", 
                        alert.getAssetSymbol(), alert.getThresholdPercentage(), currentPrice);
                }
                break;

            case PERCENTAGE_DECREASE:
                if (alert.getThresholdPercentage() != null) {
                    BigDecimal decreaseThreshold = threshold.multiply(
                        BigDecimal.ONE.subtract(alert.getThresholdPercentage().divide(BigDecimal.valueOf(100))));
                    shouldTrigger = currentPrice.compareTo(decreaseThreshold) < 0 && 
                        (lastTriggered == null || lastTriggered.isBefore(LocalDateTime.now().minusMinutes(5)));
                    message = String.format("Price Alert: %s decreased by %.1f%% (Current: $%s)", 
                        alert.getAssetSymbol(), alert.getThresholdPercentage(), currentPrice);
                }
                break;

            case VOLATILITY_ALERT:
                // Check for high volatility (price change > 10% in 1 hour)
                List<PriceSnapshot> hourlySnapshots = priceSnapshotRepository
                    .findByAssetSymbolAndCapturedAtAfter(alert.getAssetSymbol(), LocalDateTime.now().minusHours(1));
                
                if (hourlySnapshots.size() >= 2) {
                    BigDecimal oldPrice = hourlySnapshots.get(hourlySnapshots.size() - 1).getPriceUsd();
                    BigDecimal percentChange = currentPrice.subtract(oldPrice)
                        .divide(oldPrice, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    
                    shouldTrigger = percentChange.abs().compareTo(BigDecimal.TEN) > 0 && 
                        (lastTriggered == null || lastTriggered.isBefore(LocalDateTime.now().minusMinutes(5)));
                    message = String.format("Volatility Alert: %s changed by %.1f%% (Current: $%s)", 
                        alert.getAssetSymbol(), percentChange, currentPrice);
                }
                break;
        }

        if (shouldTrigger && !alert.getNotificationSent()) {
            // Send notification
            sendAlertNotification(alert, message, currentPrice);
            
            // Update alert
            alert.setNotificationSent(true);
            alert.setLastTriggered(LocalDateTime.now());
            priceAlertRepository.save(alert);
        }
    }

    private void sendAlertNotification(PriceAlert alert, String message, BigDecimal currentPrice) {
        try {
            User user = alert.getUser();
            String subject = "🚨 Price Alert: " + alert.getAssetSymbol();
            String emailBody = buildEmailBody(alert, message, currentPrice);
            
            if (emailService != null) {
                emailService.sendPriceAlert(user.getEmail(), subject, emailBody);
            }
            
            System.out.println("✅ Price alert processed for " + alert.getAssetSymbol() + 
                " to " + user.getEmail() + ": " + message);
        } catch (Exception e) {
            System.err.println("Failed to send alert notification: " + e.getMessage());
        }
    }

    private String buildEmailBody(PriceAlert alert, String message, BigDecimal currentPrice) {
        return String.format(
            "Hello %s,%n%n" +
            "Price Alert Triggered!%n%n" +
            "Asset: %s%n" +
            "Alert Type: %s%n" +
            "Threshold: $%s%n" +
            "Current Price: $%s%n%n" +
            "Message: %s%n%n" +
            "Time: %s%n%n" +
            "Check your portfolio dashboard for more details.%n%n" +
            "Best regards,%n" +
            "Crypto Portfolio Tracker Team",
            
            alert.getUser().getName(),
            alert.getAssetSymbol(),
            alert.getAlertType(),
            alert.getThresholdValue(),
            currentPrice,
            message,
            LocalDateTime.now()
        );
    }

    // Public methods for alert management
    @Transactional
    public PriceAlert createAlert(User user, String assetSymbol, PriceAlert.AlertType alertType, 
                              BigDecimal thresholdValue, BigDecimal thresholdPercentage) {
        PriceAlert alert = new PriceAlert(user, assetSymbol, alertType, thresholdValue, thresholdPercentage);
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public List<PriceAlert> getUserAlerts(Long userId) {
        return priceAlertRepository.findActiveAlertsByUserId(userId);
    }

    @Transactional
    public void deactivateAlert(Long alertId, Long userId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        
        if (!alert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to deactivate this alert");
        }
        
        alert.setIsActive(false);
        priceAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long alertId, Long userId) {
        priceAlertRepository.deleteByIdAndUserId(alertId, userId);
    }
}
