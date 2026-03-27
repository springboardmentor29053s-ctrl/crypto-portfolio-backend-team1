package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    // ── Existing (keep as-is) ─────────────────────────────────────────────────

    Optional<PriceSnapshot> findTopByAssetSymbolOrderByCapturedAtDesc(String assetSymbol);

    List<PriceSnapshot> findByAssetSymbolAndCapturedAtBetween(
            String assetSymbol, Instant start, Instant end);

    // ── New — required by MarketRiskService ───────────────────────────────────

    /**
     * Two most recent snapshots for a symbol.
     * Used to compute price change: latest vs the one before it.
     */
    List<PriceSnapshot> findTop2ByAssetSymbolOrderByCapturedAtDesc(String assetSymbol);

    /**
     * All snapshots for a symbol newer than a given timestamp, newest first.
     * Used to compute 7-day average volume for spike detection.
     */
    List<PriceSnapshot> findByAssetSymbolAndCapturedAtAfterOrderByCapturedAtDesc(
            String assetSymbol, Instant after);
}