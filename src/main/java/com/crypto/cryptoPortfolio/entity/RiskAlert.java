package com.crypto.cryptoPortfolio.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert {

    public enum AlertType {
        // ── Scam / contract alerts (existing) ─────────────
        rugpull_warning,
        contract_risk,
        news,
        // ── Market risk alerts (new) ──────────────────────
        price_drop,
        volatility_warning,
        portfolio_loss
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "dismissed")
    private boolean dismissed = false;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();

    // ── Getters & Setters ─────────────────────────────────

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public Long getUserId()                      { return userId; }
    public void setUserId(Long userId)           { this.userId = userId; }

    public String getAssetSymbol()               { return assetSymbol; }
    public void setAssetSymbol(String s)         { this.assetSymbol = s; }

    public AlertType getAlertType()              { return alertType; }
    public void setAlertType(AlertType t)        { this.alertType = t; }

    public String getDetails()                   { return details; }
    public void setDetails(String d)             { this.details = d; }

    public boolean isDismissed()                 { return dismissed; }
    public void setDismissed(boolean dismissed)  { this.dismissed = dismissed; }

    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant t)          { this.createdAt = t; }
}