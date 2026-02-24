package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "scam_tokens")
public class ScamToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Contract address is required")
    @Size(max = 255)
    @Column(name = "contract_address")
    private String contractAddress;

    @NotBlank(message = "Chain is required")
    @Size(max = 100)
    @Column(name = "chain")
    private String chain;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Size(max = 100)
    @Column(name = "source")
    private String source;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }

    // Constructors
    public ScamToken() {}

    public ScamToken(String contractAddress, String chain, RiskLevel riskLevel, String source) {
        this.contractAddress = contractAddress;
        this.chain = chain;
        this.riskLevel = riskLevel;
        this.source = source;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
