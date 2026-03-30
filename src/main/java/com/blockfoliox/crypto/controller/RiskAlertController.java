package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.RiskAlert;
import com.blockfoliox.crypto.service.RiskAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@CrossOrigin(origins = "http://localhost:3000")
public class RiskAlertController {

    private static final Logger log = LoggerFactory.getLogger(RiskAlertController.class);

    private final RiskAlertService riskAlertService;

    public RiskAlertController(RiskAlertService riskAlertService) {
        this.riskAlertService = riskAlertService;
    }

    // Check a contract address for risk
    @PostMapping("/check")
    public RiskAlert checkContract(@RequestBody Map<String, String> body) {
        log.info("Risk check request for contract={}", body.get("contractAddress"));
        return riskAlertService.checkContract(
                Long.parseLong(body.get("userId")),
                body.get("contractAddress"),
                body.get("assetSymbol")
        );
    }

    // Get all alerts for a user
    @GetMapping("/alerts/{userId}")
    public List<RiskAlert> getAlerts(@PathVariable Long userId) {
        log.info("Get alerts request for userId={}", userId);
        return riskAlertService.getAlerts(userId);
    }
}
