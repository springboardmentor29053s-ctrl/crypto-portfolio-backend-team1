package com.crypto.cryptoPortfolio.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldingRequest {

    private Integer exchangeId;
    private String assetSymbol;
    private BigDecimal quantity;
    private BigDecimal avgCost;
}