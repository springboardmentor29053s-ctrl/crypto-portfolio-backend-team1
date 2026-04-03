package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for logging trades with risk assessment
 */
@Entity
@Table(name = "trade_risk_logs")
public class TradeRiskLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "trade_value", precision = 20, scale = 8, nullable = false)
    private BigDecimal tradeValue;

    @Column(name = "has_risk_alert", nullable = false)
    private Boolean hasRiskAlert;

    @Column(name = "risk_details", columnDefinition = "TEXT")
    private String riskDetails;

    @Column(name = "coin_risk_assessment", columnDefinition = "JSON")
    private String coinRiskAssessment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public TradeRiskLog() {}

    public TradeRiskLog(User user, Trade trade, String riskLevel, BigDecimal tradeValue, 
                        Boolean hasRiskAlert, String riskDetails, String coinRiskAssessment) {
        this.user = user;
        this.trade = trade;
        this.riskLevel = riskLevel;
        this.tradeValue = tradeValue;
        this.hasRiskAlert = hasRiskAlert;
        this.riskDetails = riskDetails;
        this.coinRiskAssessment = coinRiskAssessment;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Trade getTrade() { return trade; }
    public void setTrade(Trade trade) { this.trade = trade; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public BigDecimal getTradeValue() { return tradeValue; }
    public void setTradeValue(BigDecimal tradeValue) { this.tradeValue = tradeValue; }

    public Boolean getHasRiskAlert() { return hasRiskAlert; }
    public void setHasRiskAlert(Boolean hasRiskAlert) { this.hasRiskAlert = hasRiskAlert; }

    public String getRiskDetails() { return riskDetails; }
    public void setRiskDetails(String riskDetails) { this.riskDetails = riskDetails; }

    public String getCoinRiskAssessment() { return coinRiskAssessment; }
    public void setCoinRiskAssessment(String coinRiskAssessment) { this.coinRiskAssessment = coinRiskAssessment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
