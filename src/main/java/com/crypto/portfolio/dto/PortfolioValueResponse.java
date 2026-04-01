package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PortfolioValueResponse {

    private Double totalValue;
    private Double totalProfitLoss;
    private Double profitPercentage;


}
