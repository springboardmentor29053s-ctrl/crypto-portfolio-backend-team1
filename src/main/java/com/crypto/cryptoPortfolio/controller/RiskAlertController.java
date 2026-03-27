package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.entity.RiskAlert;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.RiskAlertRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.MarketRiskService;
import com.crypto.cryptoPortfolio.service.NewsService;
import com.crypto.cryptoPortfolio.service.PortfolioService;
import com.crypto.cryptoPortfolio.service.RiskScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class RiskAlertController {

    private final RiskAlertRepository riskAlertRepository;
    private final RiskScanService     riskScanService;
    private final MarketRiskService   marketRiskService;
    private final NewsService         newsService;
    private final UserRepository      userRepository;
    private final HoldingRepository   holdingRepository;
    private final PortfolioService    portfolioService;

    public RiskAlertController(RiskAlertRepository riskAlertRepository,
                               RiskScanService riskScanService,
                               MarketRiskService marketRiskService,
                               NewsService newsService,
                               UserRepository userRepository,
                               HoldingRepository holdingRepository,
                               PortfolioService portfolioService) {
        this.riskAlertRepository = riskAlertRepository;
        this.riskScanService     = riskScanService;
        this.marketRiskService   = marketRiskService;
        this.newsService         = newsService;
        this.userRepository      = userRepository;
        this.holdingRepository   = holdingRepository;
        this.portfolioService    = portfolioService;
    }

    /** GET /api/alerts — all active alerts for the user */
    @GetMapping
    public ResponseEntity<List<RiskAlert>> getAlerts() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(
                riskAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId()));
    }

    /** GET /api/alerts/count — unread badge count */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getAlertCount() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(Map.of("count",
                riskAlertRepository.countByUserIdAndDismissedFalse(user.getId())));
    }

    /** GET /api/alerts/summary — per-type counts for dashboard stat cards */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getAlertSummary() {
        User user = getAuthenticatedUser();
        Long uid  = user.getId();

        Map<String, Long> summary = new java.util.LinkedHashMap<>();
        summary.put("total",              riskAlertRepository.countByUserIdAndDismissedFalse(uid));
        summary.put("rugpull_warning",    riskAlertRepository.countByUserIdAndAlertTypeAndDismissedFalse(uid, RiskAlert.AlertType.rugpull_warning));
        summary.put("contract_risk",      riskAlertRepository.countByUserIdAndAlertTypeAndDismissedFalse(uid, RiskAlert.AlertType.contract_risk));
        summary.put("price_drop",         riskAlertRepository.countByUserIdAndAlertTypeAndDismissedFalse(uid, RiskAlert.AlertType.price_drop));
        summary.put("volatility_warning", riskAlertRepository.countByUserIdAndAlertTypeAndDismissedFalse(uid, RiskAlert.AlertType.volatility_warning));
        summary.put("portfolio_loss",     riskAlertRepository.countByUserIdAndAlertTypeAndDismissedFalse(uid, RiskAlert.AlertType.portfolio_loss));
        summary.put("news",               riskAlertRepository.countByUserIdAndAlertTypeAndDismissedFalse(uid, RiskAlert.AlertType.news));
        return ResponseEntity.ok(summary);
    }

    /** POST /api/alerts/dismiss/{id} */
    @PostMapping("/dismiss/{id}")
    public ResponseEntity<Map<String, String>> dismissAlert(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return riskAlertRepository.findById(id)
                .filter(a -> a.getUserId().equals(user.getId()))
                .map(a -> {
                    a.setDismissed(true);
                    riskAlertRepository.save(a);
                    return ResponseEntity.ok(Map.of("message", "Alert dismissed"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/alerts/dismiss-all */
    @PostMapping("/dismiss-all")
    public ResponseEntity<Map<String, Object>> dismissAll() {
        User user = getAuthenticatedUser();
        List<RiskAlert> active =
                riskAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId());
        active.forEach(a -> a.setDismissed(true));
        riskAlertRepository.saveAll(active);
        return ResponseEntity.ok(Map.of("message", "All alerts dismissed", "count", (long) active.size()));
    }

    /**
     * POST /api/alerts/scan
     * Manual trigger — runs all 3 scans: scam + market + news
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan() {
        User user = getAuthenticatedUser();

        int scamAlerts   = riskScanService.scanUserHoldings(user);
        int marketAlerts = marketRiskService.scanUserHoldings(user);
        int newsAlerts   = newsService.scanNewsForUser(user);

        return ResponseEntity.ok(Map.of(
                "message",      "Scan complete",
                "scamAlerts",   scamAlerts,
                "marketAlerts", marketAlerts,
                "newsAlerts",   newsAlerts,
                "totalCreated", scamAlerts + marketAlerts + newsAlerts
        ));
    }

    /**
     * POST /api/alerts/seed-prices
     * ⚠️ TESTING ONLY — remove before production.
     */
    @PostMapping("/seed-prices")
    public ResponseEntity<Map<String, Object>> seedPriceSnapshots() {
        User user = getAuthenticatedUser();
        List<com.crypto.cryptoPortfolio.entity.Holding> holdings =
                holdingRepository.findByUserId(user.getId());
        int saved = 0;
        for (var h : holdings) {
            try {
                portfolioService.savePriceSnapshot(h.getAssetSymbol());
                saved++;
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Price snapshots saved", "symbolsProcessed", saved));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + auth.getName()));
    }
}