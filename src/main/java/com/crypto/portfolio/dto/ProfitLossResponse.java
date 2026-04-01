package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
// info
@Getter
@Setter
@AllArgsConstructor
public class ProfitLossResponse {

    private double totalValue;
    private double totalInvested;
    private double totalProfitLoss;
    private double profitPercentage;

    private List<AssetPL> assets;
}

