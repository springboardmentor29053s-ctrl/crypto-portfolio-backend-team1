package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_transactions")
public class TaxTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    private Trade trade;
    
    @Column(name = "transaction_type", nullable = false)
    private String transactionType; // BUY, SELL, STAKING_REWARD, AIRDROP, FEE
    
    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;
    
    @Column(name = "quantity", precision=19, scale=8)
    private BigDecimal quantity;
    
    @Column(name = "price_usd", precision=19, scale=8)
    private BigDecimal priceUsd;
    
    @Column(name = "total_usd", precision=19, scale=8)
    private BigDecimal totalUsd;
    
    @Column(name = "fee_usd", precision=19, scale=8)
    private BigDecimal feeUsd;
    
    @Column(name = "cost_basis", precision=19, scale=8)
    private BigDecimal costBasis;
    
    @Column(name = "proceeds", precision=19, scale=8)
    private BigDecimal proceeds;
    
    @Column(name = "gain_loss", precision=19, scale=8)
    private BigDecimal gainLoss;
    
    @Column(name = "gain_loss_type")
    private String gainLossType; // SHORT_TERM, LONG_TERM, NONE
    
    @Column(name = "holding_period_days")
    private Integer holdingPeriodDays;
    
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    @Column(name = "tax_event_type")
    private String taxEventType; // DISPOSAL, INCOME, EXPENSE
    
    @Column(name = "taxable_amount", precision=19, scale=8)
    private BigDecimal taxableAmount;
    
    @Column(name = "tax_rate", precision=5, scale=4)
    private BigDecimal taxRate;
    
    @Column(name = "estimated_tax", precision=19, scale=8)
    private BigDecimal estimatedTax;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "notes")
    private String notes;
    
    // Constructors
    public TaxTransaction() {}
    
    public TaxTransaction(User user, Trade trade, String transactionType, 
                         LocalDateTime transactionDate, Integer taxYear) {
        this.user = user;
        this.trade = trade;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.taxYear = taxYear;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Trade getTrade() { return trade; }
    public void setTrade(Trade trade) { this.trade = trade; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPriceUsd() { return priceUsd; }
    public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }
    
    public BigDecimal getTotalUsd() { return totalUsd; }
    public void setTotalUsd(BigDecimal totalUsd) { this.totalUsd = totalUsd; }
    
    public BigDecimal getFeeUsd() { return feeUsd; }
    public void setFeeUsd(BigDecimal feeUsd) { this.feeUsd = feeUsd; }
    
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    
    public BigDecimal getProceeds() { return proceeds; }
    public void setProceeds(BigDecimal proceeds) { this.proceeds = proceeds; }
    
    public BigDecimal getGainLoss() { return gainLoss; }
    public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
    
    public String getGainLossType() { return gainLossType; }
    public void setGainLossType(String gainLossType) { this.gainLossType = gainLossType; }
    
    public Integer getHoldingPeriodDays() { return holdingPeriodDays; }
    public void setHoldingPeriodDays(Integer holdingPeriodDays) { this.holdingPeriodDays = holdingPeriodDays; }
    
    public Integer getTaxYear() { return taxYear; }
    public void setTaxYear(Integer taxYear) { this.taxYear = taxYear; }
    
    public String getTaxEventType() { return taxEventType; }
    public void setTaxEventType(String taxEventType) { this.taxEventType = taxEventType; }
    
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
    
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    
    public BigDecimal getEstimatedTax() { return estimatedTax; }
    public void setEstimatedTax(BigDecimal estimatedTax) { this.estimatedTax = estimatedTax; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
