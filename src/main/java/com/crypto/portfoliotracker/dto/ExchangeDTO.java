package com.crypto.portfoliotracker.dto;

import java.time.LocalDateTime;

public class ExchangeDTO {
    private Long id;
    private String name;
    private String baseUrl;
    private LocalDateTime createdAt;

    public ExchangeDTO() {}

    public ExchangeDTO(Long id, String name, String baseUrl, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
