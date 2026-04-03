package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.ScamToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "http://localhost:3000")
public class PublicRiskController {

    /**
     * Get all active scam tokens (public endpoint)
     */
    @GetMapping("/scam-tokens")
    public ResponseEntity<List<ScamToken>> getPublicScamTokens() {
        // Return mock data directly
        List<ScamToken> mockTokens = new ArrayList<>();
        
        // Mock scam token 1
        ScamToken token1 = new ScamToken();
        token1.setId(1L);
        token1.setContractAddress("0x1234567890123456789012345678901234567890");
        token1.setChain("ethereum");
        token1.setTokenSymbol("SCAM");
        token1.setTokenName("Scam Token");
        token1.setRiskLevel(ScamToken.RiskLevel.HIGH);
        token1.setSource("CryptoScamDB");
        token1.setReason("Honeypot detected");
        token1.setConfidenceScore(0.95);
        token1.setIsActive(true);
        mockTokens.add(token1);
        
        // Mock scam token 2
        ScamToken token2 = new ScamToken();
        token2.setId(2L);
        token2.setContractAddress("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd");
        token2.setChain("bsc");
        token2.setTokenSymbol("RUG");
        token2.setTokenName("Rug Pull Token");
        token2.setRiskLevel(ScamToken.RiskLevel.CRITICAL);
        token2.setSource("Etherscan");
        token2.setReason("Liquidity drained");
        token2.setConfidenceScore(0.98);
        token2.setIsActive(true);
        mockTokens.add(token2);
        
        return ResponseEntity.ok(mockTokens);
    }
}
