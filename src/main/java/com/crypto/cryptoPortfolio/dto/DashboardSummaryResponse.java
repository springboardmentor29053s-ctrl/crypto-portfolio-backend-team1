package com.crypto.cryptoPortfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardSummaryResponse {

    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal totalPnL;
    private BigDecimal totalPnLPercentage;
    private List<AssetAllocation> assetAllocation;

    public DashboardSummaryResponse(
            BigDecimal totalValue,
            BigDecimal totalInvested,
            BigDecimal totalPnL,
            BigDecimal totalPnLPercentage,
            List<AssetAllocation> assetAllocation) {

        this.totalValue = totalValue;
        this.totalInvested = totalInvested;
        this.totalPnL = totalPnL;
        this.totalPnLPercentage = totalPnLPercentage;
        this.assetAllocation = assetAllocation;
    }

    public static class AssetAllocation {
        private String symbol;
        private BigDecimal percentage;

        public AssetAllocation(String symbol, BigDecimal percentage) {
            this.symbol = symbol;
            this.percentage = percentage;
        }

        public String getSymbol() { return symbol; }
        public BigDecimal getPercentage() { return percentage; }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }

    public BigDecimal getTotalValue() { return totalValue; }
    public BigDecimal getTotalInvested() { return totalInvested; }
    public BigDecimal getTotalPnL() { return totalPnL; }
    public BigDecimal getTotalPnLPercentage() { return totalPnLPercentage; }
    public List<AssetAllocation> getAssetAllocation() { return assetAllocation; }


}