package com.crypto.portfolio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "scam_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScamToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contractAddress;

    private String chain;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private String source;

    private LocalDateTime lastSeen;

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}