package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Asset symbol is required")
    @Column(name = "asset_symbol")
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private TradeSide side;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", message = "Quantity must be positive")
    @Column(name = "quantity", precision = 20, scale = 8)
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "fee", precision = 20, scale = 8)
    private BigDecimal fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    public enum TradeSide {
        BUY,
        SELL
    }

    // Constructors
    public Trade() {}

    public Trade(User user, String assetSymbol, TradeSide side, BigDecimal quantity, BigDecimal price, BigDecimal fee, Exchange exchange, LocalDateTime executedAt) {
        this.user = user;
        this.assetSymbol = assetSymbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.fee = fee;
        this.exchange = exchange;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }

    public TradeSide getSide() { return side; }
    public void setSide(TradeSide side) { this.side = side; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public Exchange getExchange() { return exchange; }
    public void setExchange(Exchange exchange) { this.exchange = exchange; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
