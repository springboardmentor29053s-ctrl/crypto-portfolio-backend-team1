package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
// info
@Getter
@Setter
@AllArgsConstructor
public class AssetPL {

    private String symbol;
    private double quantity;
    private double avgCost;
    private double currentPrice;
    private double invested;
    private double value;
    private double profitLoss;
    private double profitPercentage;
}
