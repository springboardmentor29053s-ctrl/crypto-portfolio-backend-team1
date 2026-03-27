package com.crypto.cryptoPortfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class PerformanceResponse {

    private Instant date;
    private BigDecimal totalValue;

    public PerformanceResponse(Instant date, BigDecimal totalValue) {
        this.date = date;
        this.totalValue = totalValue;
    }

    // getters

    public Instant getDate() {
        return date;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
