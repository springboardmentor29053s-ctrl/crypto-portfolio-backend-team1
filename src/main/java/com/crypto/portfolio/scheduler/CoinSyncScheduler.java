package com.crypto.portfolio.scheduler;

import com.crypto.portfolio.service.CoinSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoinSyncScheduler {

    private final CoinSyncService coinSyncService;

    @Scheduled(cron="0 0 0 * * *")   // once per day
    public void syncCoins(){

        System.out.println("Syncing coin metadata...");

        coinSyncService.syncCoins();
    }
}
