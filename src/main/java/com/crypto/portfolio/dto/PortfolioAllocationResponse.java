package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PortfolioAllocationResponse {

    private String symbol;
    private Double value;
    private Double percentage;

}