package com.crypto.cryptoPortfolio.scheduler;

import com.crypto.cryptoPortfolio.entity.RiskAlert;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.RiskAlertRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.MarketRiskService;
import com.crypto.cryptoPortfolio.service.NewsService;
import com.crypto.cryptoPortfolio.service.NotificationService;
import com.crypto.cryptoPortfolio.service.RiskScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiskScheduler.class);

    private final UserRepository      userRepository;
    private final RiskScanService     riskScanService;
    private final MarketRiskService   marketRiskService;
    private final NewsService         newsService;
    private final NotificationService notificationService;
    private final RiskAlertRepository riskAlertRepository;

    public RiskScheduler(UserRepository userRepository,
                         RiskScanService riskScanService,
                         MarketRiskService marketRiskService,
                         NewsService newsService,
                         NotificationService notificationService,
                         RiskAlertRepository riskAlertRepository) {
        this.userRepository      = userRepository;
        this.riskScanService     = riskScanService;
        this.marketRiskService   = marketRiskService;
        this.newsService         = newsService;
        this.notificationService = notificationService;
        this.riskAlertRepository = riskAlertRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Job 1: Full scan every 6 hours
    // Runs: scam/contract + market risk + news alerts for ALL users
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledRiskScan() {
        log.info("⏰ Scheduled risk scan started...");

        List<User> allUsers = userRepository.findAll();
        int totalAlerts = 0;

        for (User user : allUsers) {
            try {
                int scamAlerts   = riskScanService.scanUserHoldings(user);
                int marketAlerts = marketRiskService.scanUserHoldings(user);
                int newsAlerts   = newsService.scanNewsForUser(user);     // ← NEW

                int newAlerts = scamAlerts + marketAlerts + newsAlerts;
                totalAlerts  += newAlerts;

                log.info("✅ Scanned user {} — {} scam, {} market, {} news alert(s)",
                        user.getEmail(), scamAlerts, marketAlerts, newsAlerts);

                if (newAlerts > 0) {
                    sendNewAlertEmails(user, newAlerts);
                }

            } catch (Exception e) {
                log.error("❌ Scan failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("⏰ Scan complete — {} total alert(s) across {} users",
                totalAlerts, allUsers.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Job 2: Daily digest at 8 AM IST (02:30 UTC)
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 30 2 * * *")
    public void sendDailyDigest() {
        log.info("📧 Daily digest started...");

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                List<RiskAlert> active =
                        riskAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(
                                user.getId());
                if (!active.isEmpty()) {
                    notificationService.sendDailySummary(user, active);
                    log.info("📧 Digest sent to {} ({} alerts)", user.getEmail(), active.size());
                }
            } catch (Exception e) {
                log.error("❌ Digest failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("📧 Daily digest complete");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private void sendNewAlertEmails(User user, int count) {
        riskAlertRepository
                .findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId())
                .stream()
                .limit(count)
                .forEach(alert -> notificationService.sendAlertEmail(user, alert));
    }
}