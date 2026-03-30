package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.model.*;
import com.blockfoliox.crypto.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskAlertService {

    private static final Logger log = LoggerFactory.getLogger(RiskAlertService.class);

    private final RestTemplate restTemplate;
    private final RiskAlertRepository riskAlertRepository;
    private final ScamTokenRepository scamTokenRepository;
    private final UserRepository userRepository;

    @Value("${etherscan.api.key}")
    private String etherscanApiKey;

    public RiskAlertService(RestTemplate restTemplate,
                            RiskAlertRepository riskAlertRepository,
                            ScamTokenRepository scamTokenRepository,
                            UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.riskAlertRepository = riskAlertRepository;
        this.scamTokenRepository = scamTokenRepository;
        this.userRepository = userRepository;
    }

    public RiskAlert checkContract(Long userId, String contractAddress, String assetSymbol) {
        log.info("Checking contract risk for userId={} contract={}", userId, contractAddress);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Check if alert already exists for this symbol today — avoid duplicates
        List<RiskAlert> existing = riskAlertRepository.findByAssetSymbol(assetSymbol.toUpperCase());
        boolean alreadyExists = existing.stream().anyMatch(a ->
                a.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate())
        );

        if (alreadyExists) {
            log.info("ℹ️ Alert already exists for {} today, returning existing", assetSymbol);
            return existing.get(0);
        }

        String goPlusRisk = checkGoPlus(contractAddress);
        boolean isVerified = checkEtherscan(contractAddress);

        RiskAlert.AlertType alertType;
        String details;
        ScamToken.RiskLevel riskLevel;

        if (goPlusRisk.equals("honeypot")) {
            alertType = RiskAlert.AlertType.rugpull_warning;
            details = "⚠️ GoPlus flagged this token as: " + goPlusRisk
                    + ". Contract verified on Etherscan: " + isVerified;
            riskLevel = ScamToken.RiskLevel.high;
        } else if (!isVerified) {
            alertType = RiskAlert.AlertType.contract_risk;
            details = "⚠️ Contract is NOT verified on Etherscan. Proceed with caution.";
            riskLevel = ScamToken.RiskLevel.medium;
        } else {
            alertType = RiskAlert.AlertType.contract_risk;
            details = "✅ Contract verified on Etherscan. GoPlus status: " + goPlusRisk;
            riskLevel = ScamToken.RiskLevel.low;
        }

        // Save to ScamTokens
        ScamToken scamToken = scamTokenRepository
                .findByContractAddress(contractAddress)
                .orElse(new ScamToken());
        scamToken.setContractAddress(contractAddress);
        scamToken.setChain("ethereum");
        scamToken.setRiskLevel(riskLevel);
        scamToken.setSource("etherscan+goplus");
        scamToken.setLastSeen(LocalDateTime.now());
        scamTokenRepository.save(scamToken);

        // Save RiskAlert
        RiskAlert alert = new RiskAlert();
        alert.setUser(user);
        alert.setAssetSymbol(assetSymbol.toUpperCase());
        alert.setAlertType(alertType);
        alert.setDetails(details);
        alert.setCreatedAt(LocalDateTime.now());
        riskAlertRepository.save(alert);

        log.info("✅ Risk alert saved for {} riskLevel={}", assetSymbol, riskLevel);
        return alert;
    }

    private String checkGoPlus(String contractAddress) {
        try {
            String url = "https://api.gopluslabs.io/api/v1/token_security/1?contract_addresses="
                    + contractAddress;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode result = root.path("result").path(contractAddress.toLowerCase());

            if (result.isMissingNode()) return "unknown";

            boolean isHoneypot = result.path("is_honeypot").asText("0").equals("1");
            boolean isScam = result.path("is_blacklisted").asText("0").equals("1");
            boolean cannotSell = result.path("cannot_sell_all").asText("0").equals("1");

            if (isHoneypot || cannotSell) return "honeypot";
            if (isScam) return "blacklisted";
            return "clean";

        } catch (Exception e) {
            log.error("❌ GoPlus API error: {}", e.getMessage());
            return "unknown";
        }
    }

    private boolean checkEtherscan(String contractAddress) {
        try {
            String url = "https://api.etherscan.io/v2/api"
                    + "?chainid=1"
                    + "&module=contract"
                    + "&action=getsourcecode"
                    + "&address=" + contractAddress
                    + "&apikey=" + etherscanApiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String sourceCode = root.path("result").get(0).path("SourceCode").asText("");
            return !sourceCode.isEmpty();

        } catch (Exception e) {
            log.error(" Etherscan API error: {}", e.getMessage());
            return false;
        }
    }

    public List<RiskAlert> getAlerts(Long userId) {
        log.info("Fetching risk alerts for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return riskAlertRepository.findByUserOrderByCreatedAtDesc(user);
    }
}