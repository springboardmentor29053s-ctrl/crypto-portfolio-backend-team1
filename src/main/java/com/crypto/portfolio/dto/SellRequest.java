package com.crypto.portfolio.dto;

import lombok.Data;

@Data
public class SellRequest {

    private String symbol;
    private Double quantity;
    private String exchange;

}