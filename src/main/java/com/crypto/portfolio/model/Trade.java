package com.crypto.portfolio.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;



@Data
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    private Side side;

    private Double quantity;

    private Double price;

    private Double fee;

    private LocalDateTime executedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    public enum Side {
        BUY,
        SELL
    }
}