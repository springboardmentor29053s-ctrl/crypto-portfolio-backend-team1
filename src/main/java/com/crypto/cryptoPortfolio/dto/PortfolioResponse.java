package com.crypto.cryptoPortfolio.dto;

public class PortfolioResponse {

    private String asset;
    private String free;
    private String locked;

    public PortfolioResponse(String asset, String free, String locked) {
        this.asset = asset;
        this.free = free;
        this.locked = locked;
    }

    public String getAsset() { return asset; }
    public String getFree() { return free; }
    public String getLocked() { return locked; }
}
