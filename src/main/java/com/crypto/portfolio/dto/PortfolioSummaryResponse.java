package com.crypto.portfolio.dto;

import lombok.Data;

@Data
public class PortfolioSummaryResponse {

    private double totalInvestment;
    private double currentValue;
    private double profit;
    private double profitPercentage;
}