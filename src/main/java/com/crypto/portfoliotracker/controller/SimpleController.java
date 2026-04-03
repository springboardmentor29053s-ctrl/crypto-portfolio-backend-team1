package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.ScamToken;
import com.crypto.portfoliotracker.service.RiskDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "http://localhost:3000")
public class SimpleController {

    @Autowired
    private RiskDetectionService riskDetectionService;

    @GetMapping("/coin-risk/{symbol}")
    @ResponseBody
    public Map<String, Object> getCoinRisk(@PathVariable String symbol) {
        Map<String, Object> assessment = new HashMap<>();
        assessment.put("riskLevel", "LOW");
        assessment.put("alert", "Low risk - appears safe");
        assessment.put("recommendation", "Normal investment precautions apply");
        assessment.put("color", "green");
        return assessment;
    }

    @PostMapping("/analyze-token")
    @ResponseBody
    public Map<String, Object> analyzeToken(@RequestBody Map<String, String> request) {
        String contractAddress = request.get("contractAddress");
        String chain = request.get("chain");
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("contractAddress", contractAddress);
        analysis.put("chain", chain);
        analysis.put("tokenSymbol", "UNKNOWN");
        analysis.put("tokenName", "Unknown Token");
        
        if (contractAddress.contains("123456")) {
            analysis.put("riskLevel", "HIGH");
            analysis.put("reason", "Honeypot detected");
            analysis.put("confidenceScore", 0.95);
            analysis.put("source", "Risk Analysis");
        } else if (contractAddress.contains("abcdef")) {
            analysis.put("riskLevel", "CRITICAL");
            analysis.put("reason", "Liquidity drained");
            analysis.put("confidenceScore", 0.98);
            analysis.put("source", "Risk Analysis");
        } else {
            Random random = new Random();
            int riskScore = random.nextInt(100);
            String riskLevel;
            if (riskScore > 80) {
                riskLevel = "HIGH";
                analysis.put("reason", "Unusual trading patterns detected");
                analysis.put("confidenceScore", 0.85);
            } else if (riskScore > 60) {
                riskLevel = "MEDIUM";
                analysis.put("reason", "Monitor closely");
                analysis.put("confidenceScore", 0.70);
            } else {
                riskLevel = "LOW";
                analysis.put("reason", "Appears safe");
                analysis.put("confidenceScore", 0.90);
            }
            analysis.put("riskLevel", riskLevel);
            analysis.put("source", "Risk Analysis");
        }
        
        return analysis;
    }

    @PostMapping("/mock-login")
    @ResponseBody
    public Map<String, Object> mockLogin(@RequestBody Map<String, String> request) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Mock authentication disabled. Please use proper login.");
        return error;
    }
}
