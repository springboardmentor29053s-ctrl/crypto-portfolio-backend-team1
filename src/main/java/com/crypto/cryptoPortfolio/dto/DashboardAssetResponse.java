package com.crypto.cryptoPortfolio.dto;


public class DashboardAssetResponse {

    private String asset;
    private double quantity;
    private double avgCost;
    private double currentPrice;
    private double currentValue;
    private double profitLoss;
    private double profitLossPercent;

    public double getChange24h() {
        return change24h;
    }

    public void setChange24h(double change24h) {
        this.change24h = change24h;
    }

    private double change24h;

    // getters and setters

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(double avgCost) {
        this.avgCost = avgCost;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public double getProfitLossPercent() {
        return profitLossPercent;
    }

    public void setProfitLossPercent(double profitLossPercent) {
        this.profitLossPercent = profitLossPercent;
    }
}

