package com.crypto.portfoliotracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExchangeConnectionRequest {
    @NotBlank(message = "Exchange name is required")
    private String exchangeName;

    @NotBlank(message = "API Key is required")
    private String apiKey;

    @NotBlank(message = "API Secret is required")
    private String apiSecret;

    @Size(max = 100, message = "Label should not exceed 100 characters")
    private String label;

    // Constructors
    public ExchangeConnectionRequest() {}

    public ExchangeConnectionRequest(String exchangeName, String apiKey, String apiSecret, String label) {
        this.exchangeName = exchangeName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.label = label;
    }

    // Getters and Setters
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
