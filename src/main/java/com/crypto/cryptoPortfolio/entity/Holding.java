package com.crypto.cryptoPortfolio.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@lombok.Data
@Entity
@Table(name = "holdings")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "avg_cost", precision = 19, scale = 8)
    private BigDecimal avgCost;

    @Column(name = "wallet_type")
    private String walletType;

    // ✅ NEW: Flag to distinguish manual vs auto-calculated holdings
    @Column(name = "is_manual")
    private Boolean isManual = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relationship with Exchange (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}