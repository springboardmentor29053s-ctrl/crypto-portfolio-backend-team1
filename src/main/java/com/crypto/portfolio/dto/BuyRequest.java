package com.crypto.portfolio.dto;


import lombok.Data;

@Data
public class BuyRequest {

    private String symbol;
    private Double quantity;
    private String exchange;
    private double price; // 🔥 ADD THIS

}
