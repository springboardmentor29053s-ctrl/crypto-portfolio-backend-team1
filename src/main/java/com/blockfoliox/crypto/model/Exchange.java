package com.blockfoliox.crypto.model;

import jakarta.persistence.*;

@Entity
@Table(name = "exchanges")
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // e.g. "Binance", "Coinbase"
    private String websiteUrl;  // e.g. "https://binance.com"
    private boolean active;     // is this exchange enabled?

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}