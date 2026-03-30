package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.repository.CryptoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CryptoService {

    private final CryptoRepository cryptoRepository;
    private final RestTemplate restTemplate;

    public CryptoService(CryptoRepository cryptoRepository,
                         RestTemplate restTemplate) {
        this.cryptoRepository = cryptoRepository;
        this.restTemplate = restTemplate;
    }

    private String cachedData = null;
    private long lastFetched = 0;
    private static final long CACHE_DURATION = 60 * 1000;

    public String getCryptoPrices() {
        String url = "https://api.coingecko.com/api/v3/coins/markets"
                + "?vs_currency=usd"
                + "&order=market_cap_desc"
                + "&per_page=100"
                + "&page=1"
                + "&sparkline=true"
                + "&price_change_percentage=24h";

        long now = System.currentTimeMillis();
        if (cachedData != null && (now - lastFetched) < CACHE_DURATION) {
            return cachedData;
        }

        try {
            cachedData = restTemplate.getForObject(url, String.class);
            lastFetched = now;
            return cachedData;
        } catch (Exception e) {
            if (cachedData != null) return cachedData;
            return "[]";
        }
    }
}