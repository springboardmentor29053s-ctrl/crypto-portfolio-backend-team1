package com.crypto.portfolio.scheduler;


import com.crypto.portfolio.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskScanScheduler {

    private final RiskService riskService;

    // runs every 6 hours
    @Scheduled(fixedRate = 21600000)
    public void runRiskScan() {

        System.out.println("Running automated risk scan...");

        riskService.scanAllUsers();

    }
}
