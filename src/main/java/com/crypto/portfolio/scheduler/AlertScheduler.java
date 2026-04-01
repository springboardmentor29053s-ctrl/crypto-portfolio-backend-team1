package com.crypto.portfolio.scheduler;

import com.crypto.portfolio.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class AlertScheduler {

    private final AlertService alertService;

    @Scheduled(fixedRate = 60000)
    public void checkAlerts() {
        alertService.processAlerts();
    }
}