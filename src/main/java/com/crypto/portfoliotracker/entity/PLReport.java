package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pl_reports")
public class PLReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "report_name", nullable = false)
    private String reportName;
    
    @Column(name = "report_type", nullable = false)
    private String reportType; // MONTHLY, QUARTERLY, YEARLY, CUSTOM
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;
    
    @Column(name = "total_invested", precision=19, scale=8)
    private BigDecimal totalInvested;
    
    @Column(name = "total_value", precision=19, scale=8)
    private BigDecimal totalValue;
    
    @Column(name = "total_profit_loss", precision=19, scale=8)
    private BigDecimal totalProfitLoss;
    
    @Column(name = "total_profit_loss_percentage", precision=10, scale=4)
    private BigDecimal totalProfitLossPercentage;
    
    @Column(name = "realized_gains", precision=19, scale=8)
    private BigDecimal realizedGains;
    
    @Column(name = "realized_losses", precision=19, scale=8)
    private BigDecimal realizedLosses;
    
    @Column(name = "unrealized_gains", precision=19, scale=8)
    private BigDecimal unrealizedGains;
    
    @Column(name = "unrealized_losses", precision=19, scale=8)
    private BigDecimal unrealizedLosses;
    
    @Column(name = "total_fees", precision=19, scale=8)
    private BigDecimal totalFees;
    
    @Column(name = "total_trades")
    private Integer totalTrades;
    
    @Column(name = "winning_trades")
    private Integer winningTrades;
    
    @Column(name = "losing_trades")
    private Integer losingTrades;
    
    @Column(name = "win_rate", precision=5, scale=4)
    private BigDecimal winRate;
    
    @Column(name = "average_holding_period_days")
    private Integer averageHoldingPeriodDays;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    // Constructors
    public PLReport() {}
    
    public PLReport(User user, String reportName, String reportType, 
                   LocalDateTime startDate, LocalDateTime endDate) {
        this.user = user;
        this.reportName = reportName;
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public BigDecimal getTotalProfitLoss() { return totalProfitLoss; }
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) { this.totalProfitLoss = totalProfitLoss; }
    
    public BigDecimal getTotalProfitLossPercentage() { return totalProfitLossPercentage; }
    public void setTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) { this.totalProfitLossPercentage = totalProfitLossPercentage; }
    
    public BigDecimal getRealizedGains() { return realizedGains; }
    public void setRealizedGains(BigDecimal realizedGains) { this.realizedGains = realizedGains; }
    
    public BigDecimal getRealizedLosses() { return realizedLosses; }
    public void setRealizedLosses(BigDecimal realizedLosses) { this.realizedLosses = realizedLosses; }
    
    public BigDecimal getUnrealizedGains() { return unrealizedGains; }
    public void setUnrealizedGains(BigDecimal unrealizedGains) { this.unrealizedGains = unrealizedGains; }
    
    public BigDecimal getUnrealizedLosses() { return unrealizedLosses; }
    public void setUnrealizedLosses(BigDecimal unrealizedLosses) { this.unrealizedLosses = unrealizedLosses; }
    
    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
    
    public Integer getTotalTrades() { return totalTrades; }
    public void setTotalTrades(Integer totalTrades) { this.totalTrades = totalTrades; }
    
    public Integer getWinningTrades() { return winningTrades; }
    public void setWinningTrades(Integer winningTrades) { this.winningTrades = winningTrades; }
    
    public Integer getLosingTrades() { return losingTrades; }
    public void setLosingTrades(Integer losingTrades) { this.losingTrades = losingTrades; }
    
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    
    public Integer getAverageHoldingPeriodDays() { return averageHoldingPeriodDays; }
    public void setAverageHoldingPeriodDays(Integer averageHoldingPeriodDays) { this.averageHoldingPeriodDays = averageHoldingPeriodDays; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
