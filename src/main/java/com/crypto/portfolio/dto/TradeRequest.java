package com.crypto.portfolio.dto;

import lombok.Data;

@Data
public class TradeRequest {

    private String coinId;
    private Double quantity;
}