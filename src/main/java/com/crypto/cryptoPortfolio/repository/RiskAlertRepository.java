package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    // ── Used by RiskAlertController + NotificationController + RiskScheduler ──

    /** All active (non-dismissed) alerts for a user, newest first. */
    List<RiskAlert> findByUserIdAndDismissedFalseOrderByCreatedAtDesc(Long userId);

    /** All alerts (including dismissed) for history views. */
    List<RiskAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Unread count — navbar badge. */
    long countByUserIdAndDismissedFalse(Long userId);

    // ── Dedup checks ──────────────────────────────────────────────────────────

    /**
     * Used by RiskScanService — dedup by user + symbol (any type).
     * Prevents re-alerting the same asset within 24h regardless of alert type.
     */
    boolean existsByUserIdAndAssetSymbolAndCreatedAtAfter(
            Long userId, String assetSymbol, Instant since);

    /**
     * Used by MarketRiskService — dedup by user + symbol + specific alert type.
     * Allows price_drop and volatility_warning to fire independently
     * for the same symbol within the same 24h window.
     */
    boolean existsByUserIdAndAssetSymbolAndAlertTypeAndCreatedAtAfter(
            Long userId, String assetSymbol, RiskAlert.AlertType alertType, Instant since);

    // ── Dashboard summary cards ───────────────────────────────────────────────

    /** Count active alerts by type — used for the summary stat cards in the UI. */
    long countByUserIdAndAlertTypeAndDismissedFalse(Long userId, RiskAlert.AlertType alertType);
}