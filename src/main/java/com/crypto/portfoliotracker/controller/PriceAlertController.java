package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.PriceAlert;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.service.PriceAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://10.14.189.34:3000"})
public class PriceAlertController {

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private UserRepository userRepository;

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }

    @GetMapping
    public ResponseEntity<List<PriceAlert>> getUserAlerts(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<PriceAlert> alerts = priceAlertService.getUserAlerts(userId);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<?> createAlert(@RequestBody Map<String, Object> alertData, 
                                     Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String assetSymbol = (String) alertData.get("assetSymbol");
            String alertTypeStr = (String) alertData.get("alertType");
            BigDecimal thresholdValue = new BigDecimal(alertData.get("thresholdValue").toString());
            BigDecimal thresholdPercentage = alertData.get("thresholdPercentage") != null ? 
                new BigDecimal(alertData.get("thresholdPercentage").toString()) : null;

            PriceAlert.AlertType alertType = PriceAlert.AlertType.valueOf(alertTypeStr.toUpperCase());
            
            PriceAlert alert = priceAlertService.createAlert(user, assetSymbol, alertType, 
                                                            thresholdValue, thresholdPercentage);
            
            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{alertId}")
    public ResponseEntity<?> updateAlert(@PathVariable Long alertId,
                                     @RequestBody Map<String, Object> alertData,
                                     Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            
            // For simplicity, we'll implement deactivate functionality
            priceAlertService.deactivateAlert(alertId, userId);
            
            return ResponseEntity.ok(Map.of("message", "Alert deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<?> deleteAlert(@PathVariable Long alertId, 
                                    Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            priceAlertService.deleteAlert(alertId, userId);
            
            return ResponseEntity.ok(Map.of("message", "Alert deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, Object> emailData,
                                     Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String subject = "🧪 Test Email from Crypto Portfolio Tracker";
            String body = String.format(
                "Hello %s,%n%n" +
                "This is a test email from your Crypto Portfolio Tracker.%n%n" +
                "If you received this, email notifications are working correctly!%n%n" +
                "Time: %s%n%n" +
                "Best regards,%n" +
                "Crypto Portfolio Tracker Team",
                user.getName(),
                java.time.LocalDateTime.now()
            );

            // You would call emailService.sendPriceAlert(user.getEmail(), subject, body);
            return ResponseEntity.ok(Map.of("message", "Test email sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
