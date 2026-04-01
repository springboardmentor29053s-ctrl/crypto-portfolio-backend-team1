package com.crypto.portfolio.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertRequest {

    private String type; // PRICE / PROFIT
    private String symbol;
    private Double targetValue;
}