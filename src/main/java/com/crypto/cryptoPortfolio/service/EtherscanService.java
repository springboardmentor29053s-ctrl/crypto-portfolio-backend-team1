package com.crypto.cryptoPortfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class EtherscanService {

    private static final Logger log = LoggerFactory.getLogger(EtherscanService.class);

    @Value("${etherscan.api.key:}")
    private String apiKey;

    private static final String EMPTY = "";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Check token info by contract address.
     * Returns a risk label: "safe", "unverified", or "suspicious"
     */
    public String checkContractReputation(String contractAddress) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Etherscan API key not configured — skipping contract check for {}", contractAddress);
            return "unverified";
        }

        try {
            String url = String.format(
                    "https://api.etherscan.io/api?module=token&action=tokeninfo&contractaddress=%s&apikey=%s",
                    contractAddress, apiKey
            );

            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return "unverified";

            String status = String.valueOf(response.get("status"));
            String message = String.valueOf(response.get("message"));

            // status=1 means token found and verified on Etherscan
            if ("1".equals(status)) {
                log.info("Etherscan: contract {} is verified", contractAddress);
                return "safe";
            }

            // status=0 with NOTOK means not found / unverified
            log.warn("Etherscan: contract {} is unverified — status={} message={}", contractAddress, status, message);
            return "unverified";

        } catch (Exception e) {
            log.error("Etherscan API error for contract {}: {}", contractAddress, e.getMessage());
            return "unverified";
        }
    }

    /**
     * Check contract source code verification (extra safety check).
     * Unverified source code is a red flag.
     */
    public boolean isSourceCodeVerified(String contractAddress) {
        if (apiKey == null || apiKey.isBlank()) return false;

        try {
            String url = String.format(
                    "https://api.etherscan.io/api?module=contract&action=getsourcecode&address=%s&apikey=%s",
                    contractAddress, apiKey
            );

            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return false;

            Object resultObj = response.get("result");
            if (resultObj instanceof java.util.List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> tempMap) {
                    Map<String, Object> resultMap = (Map<String, Object>) tempMap;
                    String sourceCode = String.valueOf(resultMap.getOrDefault("SourceCode", EMPTY));
                    return !sourceCode.isBlank() && !sourceCode.equals("null");
                }
            }
            return false;

        } catch (Exception e) {
            log.error("Etherscan source check error: {}", e.getMessage());
            return false;
        }
    }
}