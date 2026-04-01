package com.crypto.portfolio.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//entity missing
public class ConnectExchangeRequest {

    private Long exchangeId;   // "Binance TestNet"
    private String apiKey;
    private String apiSecret;
}