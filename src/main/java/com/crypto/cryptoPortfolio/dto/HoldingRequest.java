package com.crypto.cryptoPortfolio.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldingRequest {

    private Long exchangeId;
    private String assetSymbol;
    private BigDecimal quantity;
    private BigDecimal avgCost;
}