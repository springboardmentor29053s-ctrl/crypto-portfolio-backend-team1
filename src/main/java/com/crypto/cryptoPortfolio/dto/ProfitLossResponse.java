package com.crypto.cryptoPortfolio.dto;

import java.math.BigDecimal;
import java.util.*;

public class ProfitLossResponse {

    private BigDecimal totalValue;
    private BigDecimal totalUnrealizedPnL;
    private BigDecimal totalRealizedPnL;
    private BigDecimal totalInvested;

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalUnrealizedPnL() {
        return totalUnrealizedPnL;
    }

    public void setTotalUnrealizedPnL(BigDecimal totalUnrealizedPnL) {
        this.totalUnrealizedPnL = totalUnrealizedPnL;
    }

    public BigDecimal getTotalRealizedPnL() {
        return totalRealizedPnL;
    }

    public void setTotalRealizedPnL(BigDecimal totalRealizedPnL) {
        this.totalRealizedPnL = totalRealizedPnL;
    }

    public BigDecimal getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(BigDecimal totalInvested) {
        this.totalInvested = totalInvested;
    }
}
