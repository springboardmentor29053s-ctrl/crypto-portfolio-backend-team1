package com.crypto.cryptoPortfolio.dto;

public class AddApiKeyRequest {

    private Integer exchangeId;
    private String apiKey;
    private String apiSecret;
    private String label;

    public Integer getExchangeId() { return exchangeId; }
    public void setExchangeId(Integer exchangeId) { this.exchangeId = exchangeId; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
