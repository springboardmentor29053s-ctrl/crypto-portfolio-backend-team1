package com.crypto.portfolio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class PriceCacheService {

    private final CryptoMarketService cryptoMarketService;

    private volatile Map<String, Double> cachedPrices = new HashMap<>();

    private long lastUpdated = 0;

    private static final long CACHE_DURATION = 60000; // 60 sec

    private static final List<String> GLOBAL_SYMBOLS =
            List.of("BTC", "ETH", "USDT", "TRX", "SOL");

    private final Object lock = new Object(); // 🔥 REQUIRED

    public Map<String, Double> getPrices(Set<String> requestedSymbols) {

        Set<String> allSymbols = new HashSet<>(GLOBAL_SYMBOLS);
        allSymbols.addAll(requestedSymbols);

        long now = System.currentTimeMillis();

        if (cachedPrices.isEmpty() || (now - lastUpdated) > CACHE_DURATION) {

            synchronized (lock) {

                if (cachedPrices.isEmpty() || (now - lastUpdated) > CACHE_DURATION) {

                    try {
                        System.out.println("🔄 Refreshing prices from API...");

                        Map<String, Double> freshPrices =
                                cryptoMarketService.getPrices(new ArrayList<>(allSymbols));

                        if (freshPrices != null && !freshPrices.isEmpty()) {
                            cachedPrices = freshPrices;
                            lastUpdated = now;
                        }

                    } catch (Exception ex) {
                        System.out.println("⚠️ Using old cached prices");

                        if (cachedPrices.isEmpty()) {
                            throw new RuntimeException("No price data available");
                        }
                    }
                }
            }
        }

        return cachedPrices;
    }
}
