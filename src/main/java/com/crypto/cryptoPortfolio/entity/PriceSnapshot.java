package com.crypto.cryptoPortfolio.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_snapshots",
        indexes = {
                @Index(name = "idx_ps_symbol_time", columnList = "assetSymbol, capturedAt DESC")
        })
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String assetSymbol;

    private BigDecimal priceUsd;

    private BigDecimal marketCap;

    /**
     * 24-hour trading volume in USD.
     * Populated from CoinGecko "total_volume" field.
     * Required by MarketRiskService for volume spike detection.
     */
    private BigDecimal volume24h;

    private String source;

    private Instant capturedAt;

    // ── Getters & Setters ─────────────────────────────────

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }

    public String getAssetSymbol()            { return assetSymbol; }
    public void setAssetSymbol(String s)      { this.assetSymbol = s; }

    public BigDecimal getPriceUsd()           { return priceUsd; }
    public void setPriceUsd(BigDecimal p)     { this.priceUsd = p; }

    public BigDecimal getMarketCap()          { return marketCap; }
    public void setMarketCap(BigDecimal m)    { this.marketCap = m; }

    public BigDecimal getVolume24h()          { return volume24h; }
    public void setVolume24h(BigDecimal v)    { this.volume24h = v; }

    public String getSource()                 { return source; }
    public void setSource(String s)           { this.source = s; }

    public Instant getCapturedAt()            { return capturedAt; }
    public void setCapturedAt(Instant t)      { this.capturedAt = t; }
}