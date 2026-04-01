package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PortfolioQuantityAllocationResponse {

    private String symbol;
    private double quantity;
    private double percentage;

}
