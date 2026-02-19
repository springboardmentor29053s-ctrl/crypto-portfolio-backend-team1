package com.crypto.cryptoPortfolio.dto;

import java.util.List;

public class DashboardLiveResponse {

    private double totalValue;
    private double totalProfitLoss;
    private double totalProfitLossPercent;
    private List<DashboardAssetResponse> assets;

    // getters and setters

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public double getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void setTotalProfitLoss(double totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }

    public double getTotalProfitLossPercent() {
        return totalProfitLossPercent;
    }

    public void setTotalProfitLossPercent(double totalProfitLossPercent) {
        this.totalProfitLossPercent = totalProfitLossPercent;
    }

    public List<DashboardAssetResponse> getAssets() {
        return assets;
    }

    public void setAssets(List<DashboardAssetResponse> assets) {
        this.assets = assets;
    }
}

