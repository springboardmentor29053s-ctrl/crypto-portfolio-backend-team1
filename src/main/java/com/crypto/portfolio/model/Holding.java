package com.crypto.portfolio.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "holdings")
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_symbol")
    private String assetSymbol;

    private Double quantity;
    private Double avgCost;

    @Enumerated(EnumType.STRING)
    private WalletType walletType;

    private String address;
    private LocalDateTime updatedAt;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;
    public enum WalletType {
        EXCHANGE, WALLET
    }
}