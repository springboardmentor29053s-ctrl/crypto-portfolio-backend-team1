package com.crypto.cryptoPortfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TradeRequest {

    private String assetSymbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal fee;
    private Integer exchangeId;
    private LocalDateTime executedAt;
}