package com.crypto.cryptoPortfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TradeResponse {

    private Long id;
    private String assetSymbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal fee;
    private String exchangeName;
    private LocalDateTime executedAt;
    private BigDecimal realizedProfit;
}