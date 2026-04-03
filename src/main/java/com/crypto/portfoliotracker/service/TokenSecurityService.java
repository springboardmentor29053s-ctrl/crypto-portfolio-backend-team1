package com.crypto.portfoliotracker.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TokenSecurityService {

    private static final Map<String, String> RISKY_TOKENS = new HashMap<>();
    
    static {
        // Mock risky tokens for demonstration
        RISKY_TOKENS.put("0x1234567890123456789012345678901234567890", 
            "Honeypot detected - buyers cannot sell");
        RISKY_TOKENS.put("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd", 
            "Liquidity drained - rug pull detected");
        RISKY_TOKENS.put("0xscam123456789012345678901234567890123456", 
            "Contract allows owner to mint unlimited tokens");
    }

    public boolean isRiskyToken(String contractAddress) {
        return RISKY_TOKENS.containsKey(contractAddress.toLowerCase());
    }

    public String getRiskSummary(String contractAddress) {
        return RISKY_TOKENS.getOrDefault(contractAddress.toLowerCase(), 
            "Unknown risk - contract analysis required");
    }
}
