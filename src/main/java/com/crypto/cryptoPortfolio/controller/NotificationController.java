package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.entity.RiskAlert;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.RiskAlertRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final RiskAlertRepository riskAlertRepository;
    private final UserRepository userRepository;

    public NotificationController(RiskAlertRepository riskAlertRepository,
                                  UserRepository userRepository) {
        this.riskAlertRepository = riskAlertRepository;
        this.userRepository      = userRepository;
    }

    /**
     * GET /api/notifications
     * Returns all unread notifications for the navbar bell icon.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications() {
        User user = getAuthenticatedUser();

        List<RiskAlert> alerts =
                riskAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> notifications = alerts.stream().map(alert -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",          alert.getId());
            map.put("title",       formatTitle(alert.getAlertType()));
            map.put("message",     alert.getDetails());
            map.put("assetSymbol", alert.getAssetSymbol());
            map.put("type",        alert.getAlertType().name());
            map.put("severity",    getSeverity(alert.getAlertType()));
            map.put("time",        alert.getCreatedAt().toString());
            map.put("dismissed",   alert.isDismissed());
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "count",         (long) alerts.size(),
                "notifications", notifications,
                "hasUnread",     !alerts.isEmpty()
        ));
    }

    /**
     * GET /api/notifications/count
     * Lightweight endpoint — just returns the badge count.
     * Called every 30s from the frontend navbar.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        User user = getAuthenticatedUser();
        long count = riskAlertRepository.countByUserIdAndDismissedFalse(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * POST /api/notifications/dismiss/{id}
     * Dismiss a single notification.
     */
    @PostMapping("/dismiss/{id}")
    public ResponseEntity<Map<String, String>> dismiss(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return riskAlertRepository.findById(id)
                .filter(a -> a.getUserId().equals(user.getId()))
                .map(a -> {
                    a.setDismissed(true);
                    riskAlertRepository.save(a);
                    return ResponseEntity.ok(Map.of("message", "Dismissed"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/notifications/dismiss-all
     * Mark all notifications as read.
     */
    @PostMapping("/dismiss-all")
    public ResponseEntity<Map<String, Object>> dismissAll() {
        User user = getAuthenticatedUser();
        List<RiskAlert> active =
                riskAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId());
        active.forEach(a -> a.setDismissed(true));
        riskAlertRepository.saveAll(active);
        return ResponseEntity.ok(Map.of("cleared", (long) active.size()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Human-readable title for each alert type.
     * ALL 6 enum values are handled — no missing case compile errors.
     */
    private String formatTitle(RiskAlert.AlertType type) {
        return switch (type) {
            case rugpull_warning    -> "🚨 Rug Pull Warning";
            case contract_risk      -> "⚠️ Contract Risk";
            case news               -> "📰 News Alert";
            case price_drop         -> "📉 Price Drop";
            case volatility_warning -> "📊 Volatility Spike";
            case portfolio_loss     -> "🔴 Portfolio Loss";
        };
    }

    /**
     * Severity label used by the frontend for badge colour.
     * high → red, medium → orange, low → yellow/blue
     */
    private String getSeverity(RiskAlert.AlertType type) {
        return switch (type) {
            case rugpull_warning    -> "high";
            case portfolio_loss     -> "high";
            case price_drop         -> "medium";
            case contract_risk      -> "medium";
            case volatility_warning -> "low";
            case news               -> "low";
        };
    }
}