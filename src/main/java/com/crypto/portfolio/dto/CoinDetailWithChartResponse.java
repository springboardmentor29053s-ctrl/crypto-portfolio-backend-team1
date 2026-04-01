package com.crypto.portfolio.dto;


import lombok.Data;

@Data
public class CoinDetailWithChartResponse {

    private CoinDetail coinDetail;
    private CoinChartResponse chart;
}