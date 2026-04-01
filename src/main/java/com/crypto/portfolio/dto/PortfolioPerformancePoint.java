package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PortfolioPerformancePoint {

    private long timestamp;

    private double value;
}
