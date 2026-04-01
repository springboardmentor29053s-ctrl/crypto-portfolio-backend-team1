package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class TaxTransaction {

    private String symbol;
    private double quantity;
    private double buyPrice;
    private double sellPrice;
    private double profit;
    private LocalDateTime date;
}