package com.crypto.cryptoPortfolio.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scam_tokens")
public class ScamToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_address", nullable = false, unique = true)
    private String contractAddress;

    @Column(name = "chain")
    private String chain;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "source")
    private String source;

    @Column(name = "last_seen")
    private Instant lastSeen = Instant.now();

    public enum RiskLevel {
        low, medium, high
    }

    // Getters & Setters
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

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
}
