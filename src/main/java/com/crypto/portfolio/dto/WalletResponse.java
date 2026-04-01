package com.crypto.portfolio.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletResponse {

    private Double balance;
    private String currency;

}