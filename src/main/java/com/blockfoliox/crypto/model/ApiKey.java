package com.blockfoliox.crypto.model;

import jakarta.persistence.*;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which user owns this key
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Which exchange this key belongs to
    @ManyToOne
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @Column(name = "api_key", nullable = false)
    private String apiKey;          // encrypted before saving

    @Column(name = "api_secret", nullable = false)
    private String apiSecret;       // encrypted before saving

    private boolean active;

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Exchange getExchange() { return exchange; }
    public void setExchange(Exchange exchange) { this.exchange = exchange; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}