package com.crypto.cryptoPortfolio.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long externalTradeId;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeSide side;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Column(precision = 19, scale = 8)
    private BigDecimal fee;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    private BigDecimal realizedProfit;
}
