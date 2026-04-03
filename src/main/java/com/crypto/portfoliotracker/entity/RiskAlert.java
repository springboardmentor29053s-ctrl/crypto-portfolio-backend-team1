package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "is_read")
    private Boolean seen = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Boolean isSeen() { return seen; }
    public void setSeen(Boolean seen) { this.seen = seen; }
    
    public static RiskAlertBuilder builder() { return new RiskAlertBuilder(); }

    public static class RiskAlertBuilder {
        private User user;
        private String assetSymbol;
        private AlertType alertType;
        private String details;
        private Boolean seen = false;
        
        public RiskAlertBuilder user(User user) { this.user = user; return this; }
        public RiskAlertBuilder assetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; return this; }
        public RiskAlertBuilder alertType(AlertType alertType) { this.alertType = alertType; return this; }
        public RiskAlertBuilder details(String details) { this.details = details; return this; }
        public RiskAlertBuilder seen(Boolean seen) { this.seen = seen; return this; }
        
        public RiskAlert build() {
            RiskAlert alert = new RiskAlert();
            alert.user = this.user;
            alert.assetSymbol = this.assetSymbol;
            alert.alertType = this.alertType;
            alert.details = this.details;
            alert.seen = this.seen;
            alert.createdAt = LocalDateTime.now();
            return alert;
        }
    }

    public enum AlertType {
        RUGPULL_WARNING,
        CONTRACT_RISK,
        LIQUIDITY_RISK,
        HOLDER_CONCENTRATION,
        NEWS,
        PRICE_VOLATILITY,
        LOW_MARKET_CAP,
        HIGH_VOLATILITY,
        NOT_ON_COINGECKO
    }
}
