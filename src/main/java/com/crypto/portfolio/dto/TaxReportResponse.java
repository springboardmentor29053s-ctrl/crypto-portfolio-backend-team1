package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TaxReportResponse {

    private double totalProfit;
    private double totalLoss;
    private double TaxableIncome;
    private double tax;
    private double netAfterTax;

    private List<TaxTransaction> transactions;
}