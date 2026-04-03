package com.crypto.portfoliotracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configuration for risk thresholds and limits
 */
@Configuration
public class RiskThresholdConfig {

    @Value("${risk.threshold.high-value:10000}")
    private BigDecimal highValueThreshold;

    @Value("${risk.threshold.medium-value:5000}")
    private BigDecimal mediumValueThreshold;

    @Value("${risk.threshold.high-quantity:1000}")
    private BigDecimal highQuantityThreshold;

    @Value("${risk.threshold.medium-quantity:500}")
    private BigDecimal mediumQuantityThreshold;

    @Value("${risk.threshold.rapid-trade-count:3}")
    private Integer rapidTradeCountThreshold;

    @Value("${risk.threshold.rapid-trade-window-minutes:5}")
    private Integer rapidTradeWindowMinutes;

    // Getters
    public BigDecimal getHighValueThreshold() {
        return highValueThreshold;
    }

    public BigDecimal getMediumValueThreshold() {
        return mediumValueThreshold;
    }

    public BigDecimal getHighQuantityThreshold() {
        return highQuantityThreshold;
    }

    public BigDecimal getMediumQuantityThreshold() {
        return mediumQuantityThreshold;
    }

    public Integer getRapidTradeCountThreshold() {
        return rapidTradeCountThreshold;
    }

    public Integer getRapidTradeWindowMinutes() {
        return rapidTradeWindowMinutes;
    }

    // Setters for runtime configuration updates
    public void setHighValueThreshold(BigDecimal highValueThreshold) {
        this.highValueThreshold = highValueThreshold;
    }

    public void setMediumValueThreshold(BigDecimal mediumValueThreshold) {
        this.mediumValueThreshold = mediumValueThreshold;
    }

    public void setHighQuantityThreshold(BigDecimal highQuantityThreshold) {
        this.highQuantityThreshold = highQuantityThreshold;
    }

    public void setMediumQuantityThreshold(BigDecimal mediumQuantityThreshold) {
        this.mediumQuantityThreshold = mediumQuantityThreshold;
    }

    public void setRapidTradeCountThreshold(Integer rapidTradeCountThreshold) {
        this.rapidTradeCountThreshold = rapidTradeCountThreshold;
    }

    public void setRapidTradeWindowMinutes(Integer rapidTradeWindowMinutes) {
        this.rapidTradeWindowMinutes = rapidTradeWindowMinutes;
    }
}
