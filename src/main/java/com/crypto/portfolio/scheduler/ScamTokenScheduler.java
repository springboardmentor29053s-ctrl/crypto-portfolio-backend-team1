package com.crypto.portfolio.scheduler;

import com.crypto.portfolio.service.ScamTokenSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScamTokenScheduler {

    private final ScamTokenSyncService scamTokenSyncService;

    @Scheduled(cron = "0 0 */12 * * *")
    public void updateScamTokens(){

        System.out.println("Updating scam tokens...");

        scamTokenSyncService.syncScamTokens();

        System.out.println("Scam token update finished.");
    }
}