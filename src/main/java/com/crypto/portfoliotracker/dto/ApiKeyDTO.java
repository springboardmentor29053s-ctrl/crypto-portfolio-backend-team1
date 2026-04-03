package com.crypto.portfoliotracker.dto;

import java.time.LocalDateTime;

public class ApiKeyDTO {
    private Long id;
    private String label;
    private ExchangeDTO exchange;
    private LocalDateTime createdAt;

    public ApiKeyDTO() {}

    public ApiKeyDTO(Long id, String label, ExchangeDTO exchange, LocalDateTime createdAt) {
        this.id = id;
        this.label = label;
        this.exchange = exchange;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public ExchangeDTO getExchange() { return exchange; }
    public void setExchange(ExchangeDTO exchange) { this.exchange = exchange; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
