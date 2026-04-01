package com.crypto.portfolio.controller;

import com.crypto.portfolio.dto.RiskAlertResponse;
import com.crypto.portfolio.model.RiskAlert;
import com.crypto.portfolio.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @PostMapping("/scan")
    public String scanRisk() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        riskService.scanUserHoldings(username);

        return "Risk scan completed";
    }

    @GetMapping("/alerts")
    public List<RiskAlertResponse> getAlerts() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return riskService.getUserAlerts(username);
    }
}