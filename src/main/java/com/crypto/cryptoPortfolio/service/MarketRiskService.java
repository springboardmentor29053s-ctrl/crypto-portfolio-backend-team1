package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.entity.PriceSnapshot;
import com.crypto.cryptoPortfolio.entity.RiskAlert;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.PriceSnapshotRepository;
import com.crypto.cryptoPortfolio.repository.RiskAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Detects market-level risk for a user's holdings:
 *   1. Price drop alert  — price fell more than PRICE_DROP_THRESHOLD (8%) vs last snapshot
 *   2. Volume spike alert — 24h volume is more than VOLUME_SPIKE_MULTIPLIER (2×) vs 7-day average
 *   3. Portfolio loss alert — unrealized loss exceeds PORTFOLIO_LOSS_THRESHOLD (10%) of total cost
 *
 * Uses PriceSnapshot rows already stored by CoinGeckoPricingService (Module C).
 * No external API calls are made here — we work purely off the snapshot table.
 */
@Service
public class MarketRiskService {

    private static final Logger log = LoggerFactory.getLogger(MarketRiskService.class);

    // ── Thresholds ────────────────────────────────────────────────────────────
    /** Price drop percentage that triggers an alert (0.08 = 8%) */
    private static final double PRICE_DROP_THRESHOLD = 0.08;

    /** Volume must be this many times the 7-day average to trigger a spike alert */
    private static final double VOLUME_SPIKE_MULTIPLIER = 2.0;

    /** Unrealized portfolio loss percentage that triggers a portfolio alert (0.10 = 10%) */
    private static final double PORTFOLIO_LOSS_THRESHOLD = 0.10;

    /** Deduplicate: don't fire the same alert type for the same symbol within this window */
    private static final long DEDUP_HOURS = 24;

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final HoldingRepository holdingRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final RiskAlertRepository riskAlertRepository;

    public MarketRiskService(HoldingRepository holdingRepository,
                             PriceSnapshotRepository priceSnapshotRepository,
                             RiskAlertRepository riskAlertRepository) {
        this.holdingRepository       = holdingRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.riskAlertRepository     = riskAlertRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Run all market-risk checks for one user.
     * Called by RiskScheduler every 6 hours alongside RiskScanService.
     *
     * @return number of new alerts created
     */
    public int scanUserHoldings(User user) {
        List<Holding> holdings = holdingRepository.findByUserId(user.getId());
        int alertsCreated = 0;

        double totalCostBasis    = 0.0;
        double totalCurrentValue = 0.0;

        for (Holding holding : holdings) {
            String symbol = normalizeSymbol(holding.getAssetSymbol());

            // Latest snapshot for this symbol
            Optional<PriceSnapshot> latestOpt = priceSnapshotRepository
                    .findTopByAssetSymbolOrderByCapturedAtDesc(symbol);

            if (latestOpt.isEmpty()) {
                log.debug("No price snapshot for {} — skipping market risk check", symbol);
                continue;
            }

            PriceSnapshot latest = latestOpt.get();
            double currentPrice = latest.getPriceUsd() != null
                    ? latest.getPriceUsd().doubleValue() : 0.0;

            if (currentPrice <= 0) continue;

            // ── CHECK 1: Price drop ──────────────────────────────────────────
            alertsCreated += checkPriceDrop(user, holding, symbol, currentPrice);

            // ── CHECK 2: Volume spike ────────────────────────────────────────
            alertsCreated += checkVolumeSpike(user, holding, symbol, latest);

            // ── Accumulate for portfolio-level check ─────────────────────────
            double qty = holding.getQuantity() != null ? holding.getQuantity().doubleValue() : 0.0;
            double avgCost = holding.getAvgCost() != null ? holding.getAvgCost().doubleValue() : 0.0;

            totalCostBasis    += qty * avgCost;
            totalCurrentValue += qty * currentPrice;
        }

        // ── CHECK 3: Portfolio-level loss ────────────────────────────────────
        alertsCreated += checkPortfolioLoss(user, totalCostBasis, totalCurrentValue);

        log.info("Market risk scan complete for user {} — {} alert(s) created",
                user.getId(), alertsCreated);
        return alertsCreated;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private checks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Alert if the current price is more than PRICE_DROP_THRESHOLD below
     * the previous snapshot's price.
     */
    private int checkPriceDrop(User user, Holding holding, String symbol, double currentPrice) {
        if (isDuplicate(user.getId(), holding.getAssetSymbol(), RiskAlert.AlertType.price_drop)) {
            return 0;
        }

        // Get the snapshot just before the latest one
        List<PriceSnapshot> recent = priceSnapshotRepository
                .findTop2ByAssetSymbolOrderByCapturedAtDesc(symbol);

        if (recent.size() < 2) return 0; // need at least two snapshots to compare

        PriceSnapshot previous = recent.get(1); // second-most-recent
        if (previous.getPriceUsd() == null) return 0;

        double previousPrice = previous.getPriceUsd().doubleValue();
        if (previousPrice <= 0) return 0;

        double changePct = (currentPrice - previousPrice) / previousPrice; // negative = drop

        if (changePct <= -PRICE_DROP_THRESHOLD) {
            double dropPct = Math.abs(changePct) * 100;
            String details = String.format(
                    "📉 %s dropped %.1f%% (from $%.4f to $%.4f). Consider reviewing your position.",
                    symbol,
                    dropPct,
                    round(previousPrice),
                    round(currentPrice)
            );
            createAlert(user.getId(), holding.getAssetSymbol(),
                    RiskAlert.AlertType.price_drop, details);
            return 1;
        }

        return 0;
    }

    /**
     * Alert if the latest snapshot's 24h volume is more than VOLUME_SPIKE_MULTIPLIER
     * times the average volume from the past 7 days of snapshots.
     */
    private int checkVolumeSpike(User user, Holding holding, String symbol, PriceSnapshot latest) {
        if (isDuplicate(user.getId(), holding.getAssetSymbol(), RiskAlert.AlertType.volatility_warning)) {
            return 0;
        }

        if (latest.getVolume24h() == null) return 0;

        double currentVolume = latest.getVolume24h().doubleValue();
        if (currentVolume <= 0) return 0;

        // Fetch snapshots from the past 7 days (excluding the latest)
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<PriceSnapshot> history = priceSnapshotRepository
                .findByAssetSymbolAndCapturedAtAfterOrderByCapturedAtDesc(symbol, sevenDaysAgo);

        if (history.size() < 3) return 0; // not enough history to compute a meaningful average

        // Average volume across historical snapshots (skip latest which is history.get(0))
        double avgVolume = history.stream()
                .skip(1)
                .filter(s -> s.getVolume24h() != null && s.getVolume24h().doubleValue() > 0)
                .mapToDouble(s -> s.getVolume24h().doubleValue())
                .average()
                .orElse(0.0);

        if (avgVolume <= 0) return 0;

        double spikeRatio = currentVolume / avgVolume;

        if (spikeRatio >= VOLUME_SPIKE_MULTIPLIER) {
            String details = String.format(
                    "📊 %s volume spiked %.1f× above its 7-day average ($%.0f vs avg $%.0f). " +
                            "High volatility — proceed with caution.",
                    symbol,
                    round(spikeRatio),
                    round(currentVolume),
                    round(avgVolume)
            );
            createAlert(user.getId(), holding.getAssetSymbol(),
                    RiskAlert.AlertType.volatility_warning, details);
            return 1;
        }

        return 0;
    }

    /**
     * Alert if the user's total unrealized portfolio loss exceeds PORTFOLIO_LOSS_THRESHOLD.
     * Uses a synthetic symbol "PORTFOLIO" so dedup and alert rows work like any other symbol.
     */
    private int checkPortfolioLoss(User user, double totalCostBasis, double totalCurrentValue) {
        if (totalCostBasis <= 0) return 0;

        if (isDuplicate(user.getId(), "PORTFOLIO", RiskAlert.AlertType.portfolio_loss)) {
            return 0;
        }

        double changePct = (totalCurrentValue - totalCostBasis) / totalCostBasis;

        if (changePct <= -PORTFOLIO_LOSS_THRESHOLD) {
            double lossPct = Math.abs(changePct) * 100;
            double lossUsd = totalCostBasis - totalCurrentValue;
            String details = String.format(
                    "🔴 Your overall portfolio is down %.1f%% (unrealized loss ~$%.2f). " +
                            "Total value: $%.2f vs cost basis: $%.2f.",
                    lossPct,
                    round(lossUsd),
                    round(totalCurrentValue),
                    round(totalCostBasis)
            );
            createAlert(user.getId(), "PORTFOLIO",
                    RiskAlert.AlertType.portfolio_loss, details);
            return 1;
        }

        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Strip common suffixes so "ETHUSDT" → "ETH", "BTCUSDT" → "BTC".
     * Keeps the symbol consistent with PriceSnapshot.assetSymbol entries
     * (which are typically stored as "BTC", "ETH", etc. by CoinGecko).
     */
    private String normalizeSymbol(String raw) {
        if (raw == null) return "";
        return raw.toUpperCase()
                .replace("USDT", "")
                .replace("BUSD", "")
                .replace("BTC",  "")
                .trim();
    }

    /** Returns true if we already created an alert of this type for this symbol within DEDUP_HOURS. */
    private boolean isDuplicate(Long userId, String assetSymbol, RiskAlert.AlertType type) {
        Instant since = Instant.now().minus(DEDUP_HOURS, ChronoUnit.HOURS);
        boolean exists = riskAlertRepository
                .existsByUserIdAndAssetSymbolAndAlertTypeAndCreatedAtAfter(
                        userId, assetSymbol, type, since);
        if (exists) {
            log.info("Skipping {} {} alert for user {} — already alerted within {}h",
                    assetSymbol, type, userId, DEDUP_HOURS);
        }
        return exists;
    }

    private void createAlert(Long userId, String assetSymbol,
                             RiskAlert.AlertType type, String details) {
        RiskAlert alert = new RiskAlert();
        alert.setUserId(userId);
        alert.setAssetSymbol(assetSymbol);
        alert.setAlertType(type);
        alert.setDetails(details);
        alert.setDismissed(false);
        alert.setCreatedAt(Instant.now());
        riskAlertRepository.save(alert);
        log.info("Market alert created: userId={} symbol={} type={}", userId, assetSymbol, type);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}