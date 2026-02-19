package com.crypto.cryptoPortfolio.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class HoldingRequest {

    private String assetSymbol;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private String walletType;
}

