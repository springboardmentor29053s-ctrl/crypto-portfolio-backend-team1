package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "threshold_value", nullable = false, precision = 18, scale = 8)
    private BigDecimal thresholdValue;

    @Column(name = "threshold_percentage", precision = 5, scale = 2)
    private BigDecimal thresholdPercentage;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_triggered", nullable = true)
    private LocalDateTime lastTriggered;

    public enum AlertType {
        PRICE_ABOVE,
        PRICE_BELOW,
        PERCENTAGE_INCREASE,
        PERCENTAGE_DECREASE,
        VOLATILITY_ALERT
    }

    // Constructors
    public PriceAlert() {}

    public PriceAlert(User user, String assetSymbol, AlertType alertType, 
                        BigDecimal thresholdValue, BigDecimal thresholdPercentage) {
        this.user = user;
        this.assetSymbol = assetSymbol.toUpperCase();
        this.alertType = alertType;
        this.thresholdValue = thresholdValue;
        this.thresholdPercentage = thresholdPercentage;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.notificationSent = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public BigDecimal getThresholdPercentage() { return thresholdPercentage; }
    public void setThresholdPercentage(BigDecimal thresholdPercentage) { this.thresholdPercentage = thresholdPercentage; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }
}
