package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.RiskAlertRepository;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.service.RiskDetectionService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@CrossOrigin(origins = "http://localhost:3000")
public class RiskController {

    private final RiskAlertRepository riskAlertRepository;
    private final UserRepository userRepository;
    private final RiskDetectionService riskDetectionService;

    public RiskController(
            RiskAlertRepository riskAlertRepository,
            UserRepository userRepository,
            RiskDetectionService riskDetectionService) {

        this.riskAlertRepository = riskAlertRepository;
        this.userRepository = userRepository;
        this.riskDetectionService = riskDetectionService;
    }

    // GET /api/risk/alerts — all alerts for user
    @GetMapping("/alerts")
    public List<RiskAlert> getAlerts(Authentication auth) {
        User user = getUser(auth);
        return riskAlertRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // GET /api/risk/alerts/unread — only unseen alerts
    @GetMapping("/alerts/unread")
    public List<RiskAlert> getUnreadAlerts(Authentication auth) {
        User user = getUser(auth);
        return riskAlertRepository.findByUserAndSeenFalseOrderByCreatedAtDesc(user);
    }

    // GET /api/risk/alerts/count — unread count (for notification badge)
    @GetMapping("/alerts/count")
    public Map<String, Long> getUnreadCount(Authentication auth) {
        User user = getUser(auth);
        Map<String, Long> response = new HashMap<>();
        response.put("unread", riskAlertRepository.countByUserAndSeenFalse(user));
        return response;
    }

    // PATCH /api/risk/alerts/{id}/seen — mark one alert as seen
    @PatchMapping("/alerts/{id}/seen")
    public ResponseEntity<?> markSeen(@PathVariable Long id, Authentication auth) {
        User user = getUser(auth);
        RiskAlert alert = riskAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getUser().getId().equals(user.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Forbidden");
            return ResponseEntity.status(403).body(error);
        }

        alert.setSeen(true);
        riskAlertRepository.save(alert);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Marked as seen");
        return ResponseEntity.ok(response);
    }

    // PATCH /api/risk/alerts/seen-all — mark all alerts as seen
    @PatchMapping("/alerts/seen-all")
    public ResponseEntity<?> markAllSeen(Authentication auth) {
        User user = getUser(auth);
        List<RiskAlert> unread = riskAlertRepository
                .findByUserAndSeenFalseOrderByCreatedAtDesc(user);
        unread.forEach(alert -> alert.setSeen(true));
        riskAlertRepository.saveAll(unread);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All alerts marked as seen");
        return ResponseEntity.ok(response);
    }

    // POST /api/risk/scan — manually trigger a scan for current user
    @PostMapping("/scan")
    public ResponseEntity<?> triggerScan(Authentication auth) {
        User user = getUser(auth);
        try {
            List<RiskAlert> alerts = riskDetectionService.scanUserHoldings(user);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scan failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
