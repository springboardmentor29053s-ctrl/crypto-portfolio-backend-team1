package com.blockfoliox.crypto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CryptoService {

    @Autowired
    private RestTemplate restTemplate;

    // ✅ Cache the last response and timestamp
    private String cachedData = null;
    private long lastFetched = 0;
    private static final long CACHE_DURATION = 60 * 1000; // 60 seconds

    public String getCryptoPrices() {
        long now = System.currentTimeMillis();

        // ✅ Return cached data if it's less than 60 seconds old
        if (cachedData != null && (now - lastFetched) < CACHE_DURATION) {
            return cachedData;
        }

        try {
            String url = "https://api.coingecko.com/api/v3/coins/markets"
                    + "?vs_currency=usd"
                    + "&order=market_cap_desc"
                    + "&per_page=25"
                    + "&page=1"
                    + "&sparkline=false"
                    + "&price_change_percentage=24h";

            cachedData = restTemplate.getForObject(url, String.class);
            lastFetched = now;
            return cachedData;

        } catch (Exception e) {
            // ✅ If rate limited, return last cached data instead of crashing
            if (cachedData != null) {
                return cachedData;
            }
            return "[]"; // return empty array so React doesn't crash
        }
    }
}