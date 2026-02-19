package com.crypto.cryptoPortfolio.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CoinGeckoService {

    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, Map<String, Double>> cachedPrices = new HashMap<>();
    private long lastFetchTime = 0;

    private static final long CACHE_DURATION = 30000; // 30 seconds

    public Map<String, Map<String, Double>> getPrices(List<String> coinIds) {

        long now = System.currentTimeMillis();

        if (now - lastFetchTime < CACHE_DURATION && !cachedPrices.isEmpty()) {
            return cachedPrices;
        }

        try {
            String ids = String.join(",", coinIds);

            String url =
                    "https://api.coingecko.com/api/v3/simple/price?ids="
                            + ids
                            + "&vs_currencies=usd"
                            + "&include_24hr_change=true";

            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);

            Map<String, Map<String, Object>> body = response.getBody();

            Map<String, Map<String, Double>> result = new HashMap<>();

            for (String id : body.keySet()) {

                Map<String, Object> values = body.get(id);

                double price =
                        ((Number) values.get("usd")).doubleValue();

                double change =
                        values.get("usd_24h_change") == null
                                ? 0.0
                                : ((Number) values.get("usd_24h_change")).doubleValue();

                Map<String, Double> coinData = new HashMap<>();
                coinData.put("price", price);
                coinData.put("change24h", change);

                result.put(id, coinData);
            }

            cachedPrices = result;
            lastFetchTime = now;

            return result;

        } catch (Exception e) {

            // If API fails, return last cached data
            return cachedPrices;
        }
    }
}



