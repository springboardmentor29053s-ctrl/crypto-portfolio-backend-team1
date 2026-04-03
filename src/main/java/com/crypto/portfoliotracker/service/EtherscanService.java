package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.ScamToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EtherscanService {
    
    @Value("${etherscan.api.key:YourApiKey}")
    private String apiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String ETHERSCAN_BASE_URL = "https://api.etherscan.io/api";
    
    /**
     * Check if a contract address is suspicious using Etherscan data
     */
    public ScamToken analyzeContract(String contractAddress) {
        try {
            // Get contract source code verification status
            String sourceCodeUrl = String.format("%s?module=contract&action=getsourcecode&address=%s&apikey=%s", 
                ETHERSCAN_BASE_URL, contractAddress, apiKey);
            
            JsonNode sourceCodeResponse = restTemplate.getForObject(sourceCodeUrl, JsonNode.class);
            
            // Get contract creation transaction
            String txUrl = String.format("%s?module=proxy&action=eth_getTransactionByHash&txhash=%s&apikey=%s", 
                ETHERSCAN_BASE_URL, getContractCreationTx(contractAddress), apiKey);
            
            // Get token holder count (if available)
            String holderUrl = String.format("%s?module=token&action=tokenholderlist&contractaddress=%s&page=1&offset=100&apikey=%s", 
                ETHERSCAN_BASE_URL, contractAddress, apiKey);
            
            JsonNode holderResponse = restTemplate.getForObject(holderUrl, JsonNode.class);
            
            // Analyze the data for risk indicators
            return analyzeContractData(contractAddress, sourceCodeResponse, holderResponse);
            
        } catch (Exception e) {
            System.err.println("Error analyzing contract: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get contract creation transaction hash
     */
    private String getContractCreationTx(String contractAddress) {
        try {
            String url = String.format("%s?module=contract&action=getcontractcreation&contractaddress=%s&apikey=%s", 
                ETHERSCAN_BASE_URL, contractAddress, apiKey);
            
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("result") && response.get("result").size() > 0) {
                return response.get("result").get(0).get("txHash").asText();
            }
        } catch (Exception e) {
            System.err.println("Error getting contract creation tx: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Analyze contract data for risk indicators
     */
    private ScamToken analyzeContractData(String contractAddress, JsonNode sourceCodeResponse, JsonNode holderResponse) {
        ScamToken.RiskLevel riskLevel = ScamToken.RiskLevel.LOW;
        String reason = "";
        double confidenceScore = 0.1;
        
        // Check if source code is verified
        if (sourceCodeResponse != null && sourceCodeResponse.has("result") && 
            sourceCodeResponse.get("result").size() > 0) {
            
            JsonNode contractInfo = sourceCodeResponse.get("result").get(0);
            String sourceCode = contractInfo.get("SourceCode").asText();
            
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                riskLevel = ScamToken.RiskLevel.HIGH;
                reason += "Source code not verified. ";
                confidenceScore += 0.4;
            }
            
            // Check for suspicious patterns in source code
            if (sourceCode.contains("honeypot") || sourceCode.contains("blacklist") || 
                sourceCode.contains("antiwhale") || sourceCode.contains("maxTxAmount")) {
                riskLevel = ScamToken.RiskLevel.HIGH;
                reason += "Suspicious functions detected. ";
                confidenceScore += 0.3;
            }
        }
        
        // Check holder concentration
        if (holderResponse != null && holderResponse.has("result") && 
            holderResponse.get("result").isArray()) {
            
            int holderCount = holderResponse.get("result").size();
            if (holderCount < 10) {
                riskLevel = ScamToken.RiskLevel.MEDIUM;
                reason += "Low holder count (" + holderCount + "). ";
                confidenceScore += 0.2;
            }
        }
        
        // Determine final risk level
        if (confidenceScore >= 0.7) {
            riskLevel = ScamToken.RiskLevel.CRITICAL;
        } else if (confidenceScore >= 0.5) {
            riskLevel = ScamToken.RiskLevel.HIGH;
        } else if (confidenceScore >= 0.3) {
            riskLevel = ScamToken.RiskLevel.MEDIUM;
        }
        
        ScamToken scamToken = new ScamToken();
        scamToken.setContractAddress(contractAddress);
        scamToken.setChain("ethereum");
        scamToken.setRiskLevel(riskLevel);
        scamToken.setSource("Etherscan");
        scamToken.setLastSeen(LocalDateTime.now());
        return scamToken;
    }
    
    /**
     * Get token information from Etherscan
     */
    public List<ScamToken> getTokenInfo(String contractAddress) {
        List<ScamToken> tokens = new ArrayList<>();
        
        try {
            // Get token info
            String tokenInfoUrl = String.format("%s?module=token&action=gettokeninfo&contractaddress=%s&apikey=%s", 
                ETHERSCAN_BASE_URL, contractAddress, apiKey);
            
            JsonNode response = restTemplate.getForObject(tokenInfoUrl, JsonNode.class);
            
            if (response != null && response.has("result") && response.get("result").size() > 0) {
                JsonNode tokenInfo = response.get("result").get(0);
                
                String symbol = tokenInfo.get("symbol").asText();
                String name = tokenInfo.get("name").asText();
                
                // Analyze contract for risks
                ScamToken scamToken = analyzeContract(contractAddress);
                if (scamToken != null) {
                    tokens.add(scamToken);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting token info: " + e.getMessage());
        }
        
        return tokens;
    }
    
    /**
     * Check if contract is verified on Etherscan
     */
    public boolean isContractVerified(String contractAddress) {
        try {
            String url = String.format("%s?module=contract&action=getsourcecode&address=%s&apikey=%s", 
                ETHERSCAN_BASE_URL, contractAddress, apiKey);
            
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            
            if (response != null && response.has("result") && response.get("result").size() > 0) {
                JsonNode contractInfo = response.get("result").get(0);
                String sourceCode = contractInfo.get("SourceCode").asText();
                return sourceCode != null && !sourceCode.trim().isEmpty();
            }
        } catch (Exception e) {
            System.err.println("Error checking contract verification: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get transaction count for a contract
     */
    public int getContractTxCount(String contractAddress) {
        try {
            String url = String.format("%s?module=proxy&action=eth_getTransactionCount&address=%s&tag=latest&apikey=%s", 
                ETHERSCAN_BASE_URL, contractAddress, apiKey);
            
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            
            if (response != null && response.has("result")) {
                String txCountHex = response.get("result").asText();
                return Integer.parseInt(txCountHex.substring(2), 16); // Convert hex to decimal
            }
        } catch (Exception e) {
            System.err.println("Error getting transaction count: " + e.getMessage());
        }
        return 0;
    }
}
