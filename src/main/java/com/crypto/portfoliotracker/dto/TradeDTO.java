package com.crypto.portfoliotracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeDTO {
    private Long id;
    private String assetSymbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal fee;
    private String exchangeName;
    private LocalDateTime executedAt;

    public TradeDTO() {}

    public TradeDTO(Long id, String assetSymbol, String side, BigDecimal quantity, 
                   BigDecimal price, BigDecimal fee, String exchangeName, LocalDateTime executedAt) {
        this.id = id;
        this.assetSymbol = assetSymbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.fee = fee;
        this.exchangeName = exchangeName;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
