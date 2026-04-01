package com.crypto.portfolio.scheduler;

import com.crypto.portfolio.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {

    private final PortfolioSnapshotService snapshotService;

    @Scheduled(fixedRate = 300000) // every 5 minutes = 300000
    public void takeSnapshot() {

        snapshotService.createSnapshots();
    }
}