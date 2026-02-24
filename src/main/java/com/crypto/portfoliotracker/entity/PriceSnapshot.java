package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_snapshots")
public class PriceSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Asset symbol is required")
    @Column(name = "asset_symbol")
    private String assetSymbol;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    @Column(name = "price_usd", precision = 20, scale = 8)
    private BigDecimal priceUsd;

    @Column(name = "market_cap", precision = 30, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "source")
    private String source;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }

    // Constructors
    public PriceSnapshot() {}

    public PriceSnapshot(String assetSymbol, BigDecimal priceUsd, BigDecimal marketCap, String source) {
        this.assetSymbol = assetSymbol;
        this.priceUsd = priceUsd;
        this.marketCap = marketCap;
        this.source = source;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }

    public BigDecimal getPriceUsd() { return priceUsd; }
    public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
