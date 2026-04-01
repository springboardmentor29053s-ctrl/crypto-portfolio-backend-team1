package com.crypto.portfolio.scheduler;

import com.crypto.portfolio.service.PriceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PriceScheduler {

    private final PriceCacheService priceCacheService;

    @Scheduled(fixedRate = 60000) // every 60 sec
    public void refreshPrices() {

        Set<String> symbols = Set.of("BTC", "ETH", "USDT", "SOL");


        priceCacheService.getPrices(symbols);

        System.out.println("✅ Prices refreshed globally");
    }
}