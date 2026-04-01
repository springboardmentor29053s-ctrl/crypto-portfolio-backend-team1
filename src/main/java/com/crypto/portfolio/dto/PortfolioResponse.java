package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor

public class PortfolioResponse {

    private String assetSymbol;
    private Double quantity;
    private Double avgCost;
    private Double currentPrice;
    private Double value;
    private Double profitLoss;
}