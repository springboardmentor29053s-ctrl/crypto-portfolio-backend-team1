package com.crypto.cryptoPortfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {

    private Long id;
    private String assetSymbol;
    private BigDecimal netQuantity;
    private BigDecimal totalInvested;
    private BigDecimal averageCost;

    public HoldingResponse(Long id,
                           String assetSymbol,
                           BigDecimal quantity,
                           BigDecimal avgCost) {
        this.id = id;
        this.assetSymbol = assetSymbol;
        this.netQuantity = quantity;
        this.averageCost = avgCost;
        this.totalInvested = quantity.multiply(avgCost);
    }
}