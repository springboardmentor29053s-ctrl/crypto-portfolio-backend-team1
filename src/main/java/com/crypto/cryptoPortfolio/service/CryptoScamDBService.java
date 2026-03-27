package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.ScamToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * CryptoScamDB integration.
 *
 * NOTE: CryptoScamDB (api.cryptoscamdb.org) is an unreliable third-party service
 * that frequently returns 502/503. All methods catch exceptions and return safe
 * defaults (null / false) so a dead API never crashes the scan pipeline.
 */
@Service
public class CryptoScamDBService {

    private static final Logger log = LoggerFactory.getLogger(CryptoScamDBService.class);
    private static final String EMPTY = "";
    private static final String BASE_URL = "https://api.cryptoscamdb.org/v1";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Check a contract address against CryptoScamDB.
     * Returns the risk level or null if not found / API unavailable.
     */
    public ScamToken.RiskLevel checkAddress(String address) {
        try {
            String url = BASE_URL + "/check/" + address;
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            Boolean success = (Boolean) response.get("success");
            if (Boolean.FALSE.equals(success)) return null;

            Object inputObj = response.get("input");
            if (inputObj instanceof Map<?, ?> input) {
                Object entriesObj = input.get("entries");
                if (entriesObj instanceof List<?> entries && !entries.isEmpty()) {
                    Object first = entries.get(0);
                    if (first instanceof Map<?, ?> temp) {
                        Map<String, Object> entry = (Map<String, Object>) temp;
                        String type = String.valueOf(entry.getOrDefault("type", EMPTY));
                        if ("scam".equalsIgnoreCase(type) || "trust-trading".equalsIgnoreCase(type)) {
                            log.warn("CryptoScamDB: address {} flagged as HIGH risk ({})", address, type);
                            return ScamToken.RiskLevel.high;
                        } else if ("phishing".equalsIgnoreCase(type)) {
                            log.warn("CryptoScamDB: address {} flagged as MEDIUM risk ({})", address, type);
                            return ScamToken.RiskLevel.medium;
                        } else {
                            log.warn("CryptoScamDB: address {} flagged as LOW risk ({})", address, type);
                            return ScamToken.RiskLevel.low;
                        }
                    }
                }
            }
            return null;

        } catch (RestClientException e) {
            // 502, 503, timeout etc. — API is down, skip silently
            log.warn("CryptoScamDB unavailable for address {} ({}), skipping", address, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("CryptoScamDB unexpected error for address {}: {}", address, e.getMessage());
            return null;
        }
    }

    /**
     * Check by coin name / symbol.
     * Returns false if API is unavailable — never throws.
     */
    public boolean isKnownScamByName(String coinName) {
        try {
            String url = BASE_URL + "/scams";
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return false;

            Object resultObj = response.get("result");
            if (resultObj instanceof List<?> scams) {
                for (Object scam : scams) {
                    if (scam instanceof Map<?, ?> temp) {
                        Map<String, Object> s = (Map<String, Object>) temp;
                        String name = String.valueOf(s.getOrDefault("name", EMPTY));
                        if (name.equalsIgnoreCase(coinName)) {
                            return true;
                        }
                    }
                }
            }
            return false;

        } catch (RestClientException e) {
            // 502, 503 etc. — treat as "not a known scam" and continue
            log.warn("CryptoScamDB unavailable for name check '{}' ({}), skipping", coinName, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("CryptoScamDB name check error for '{}': {}", coinName, e.getMessage());
            return false;
        }
    }
}