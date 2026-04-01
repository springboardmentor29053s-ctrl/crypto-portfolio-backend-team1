package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TradeResponse {

    private String assetSymbol;
    private String side;
    private Double quantity;
    private Double price;
    private LocalDateTime executedAt;
    private Double realizedProfit;
}