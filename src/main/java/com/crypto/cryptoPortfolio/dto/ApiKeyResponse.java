package com.crypto.cryptoPortfolio.dto;

import java.time.LocalDateTime;

public class ApiKeyResponse {
    private Long id;
    private String exchange;
    private String apiKeyMasked;
    private String label;
    private LocalDateTime createdAt;
    private boolean isActive;

    // Constructor
    public ApiKeyResponse(Long id, String exchange, String apiKeyMasked,
                          String label, LocalDateTime createdAt, boolean isActive) {
        this.id = id;
        this.exchange = exchange;
        this.apiKeyMasked = apiKeyMasked;
        this.label = label;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    // Getters
    public Long getId() { return id; }
    public String getExchange() { return exchange; }
    public String getApiKeyMasked() { return apiKeyMasked; }
    public String getLabel() { return label; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public void setApiKeyMasked(String apiKeyMasked) { this.apiKeyMasked = apiKeyMasked; }
    public void setLabel(String label) { this.label = label; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { isActive = active; }
}
