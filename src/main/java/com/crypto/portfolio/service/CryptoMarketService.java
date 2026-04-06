package com.crypto.portfolio.service;

import com.crypto.portfolio.config.CoinGeckoConfig;
import com.crypto.portfolio.dto.*;

import com.crypto.portfolio.model.Coin;
import com.crypto.portfolio.repository.CoinRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CryptoMarketService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CoinGeckoConfig config;
    private Map<String, String> symbolToId = new HashMap<>();
    private final CoinRepository coinRepository;
    // 🔥 Dashboard cache
    private PageResponse_dto<CryptoCoin_dto> cachedDashboard;

    private long dashboardLastUpdated = 0;

    private static final long DASHBOARD_CACHE_DURATION = 60000; // 60 sec

    private final Object dashboardLock = new Object();



    public PageResponse_dto<CryptoCoin_dto> getDashboardData(int page, int size) {

        long now = System.currentTimeMillis();

        // 🔥 Step 1: Refresh cache if needed
        if (cachedDashboard == null || (now - dashboardLastUpdated) > DASHBOARD_CACHE_DURATION) {

            synchronized (dashboardLock) {

                if (cachedDashboard == null || (now - dashboardLastUpdated) > DASHBOARD_CACHE_DURATION) {

                    try {
                        System.out.println("🔄 Fetching dashboard from API...");

                        String url = config.getBaseUrl()
                                + "/coins/markets"
                                + "?vs_currency=usd"
                                + "&order=market_cap_desc"
                                + "&per_page=100"   // 🔥 fetch more once
                                + "&page=1"
                                + "&sparkline=true"
                                + "&price_change_percentage=24h,7d";

                        List<CryptoCoin_dto> coins =
                                restTemplate.exchange(
                                        url,
                                        org.springframework.http.HttpMethod.GET,
                                        null,
                                        new ParameterizedTypeReference<List<CryptoCoin_dto>>() {}
                                ).getBody();

                        PageResponse_dto<CryptoCoin_dto> response = new PageResponse_dto<>();

                        response.setContent(coins);
                        response.setPageNumber(1);
                        response.setPageSize(100);
                        response.setTotalElements(10000);
                        response.setTotalPages(1000);
                        response.setLast(false);

                        cachedDashboard = response;
                        dashboardLastUpdated = now;

                    } catch (HttpClientErrorException.TooManyRequests ex) {
                        System.out.println("⚠️ Rate limit hit, using cached dashboard");
                    }
                }
            }
        }

        // 🔥 Step 2: Pagination from cached data
        List<CryptoCoin_dto> fullList = cachedDashboard.getContent();

        int start = Math.max((page - 1) * size, 0);
        int end = Math.min(start + size, fullList.size());

        List<CryptoCoin_dto> paginatedList = new ArrayList<>();

        if (start < end) {
            paginatedList = fullList.subList(start, end);
        }

        // 🔥 Step 3: Build response
        PageResponse_dto<CryptoCoin_dto> response = new PageResponse_dto<>();

        response.setContent(paginatedList);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(fullList.size());
        response.setTotalPages((int) Math.ceil((double) fullList.size() / size));
        response.setLast(end >= fullList.size());

        return response;
    }

    @Cacheable(value = "coinDetail", key = "#coinId")
    public CoinDetail getCoinDetail(String coinId) {

        String url = config.getBaseUrl()
                + "/coins/" + coinId;

        return restTemplate.getForObject(url, CoinDetail.class);
    }

    @Cacheable(value = "coinChart", key = "#coinId+ '-' + #days")
    public CoinChartResponse getCoinChart(String coinId, String days) {

        String url = config.getBaseUrl()
                + "/coins/" + coinId + "/market_chart"
                + "?vs_currency=usd"
                + "&days="+ days;

        try {
            return restTemplate.getForObject(url, CoinChartResponse.class);
        } catch (HttpClientErrorException.TooManyRequests ex) {

            System.out.println("CoinGecko rate limit reached. Returning empty chart.");

            return new CoinChartResponse(); // prevent crash
        }
    }

    @Cacheable(value = "prices", key = "#symbol")
    public Double getCurrentPrice(String symbol) {

        String coinId = symbolToId.get(symbol.toUpperCase());

        if (coinId == null) {
            throw new RuntimeException("Coin not supported");
        }
        String url =
                config.getBaseUrl()+"/simple/price?ids="
                        + coinId +
                        "&vs_currencies=usd";

        Map<String, Map<String, Object>> response =
                restTemplate.getForObject(url, Map.class);

        Map<String, Object> coinData = response.get(coinId);

        if (coinData == null || coinData.get("usd") == null) {
            throw new RuntimeException("Price not available for " + symbol);
        }

        Number price = (Number) coinData.get("usd");
        return price.doubleValue();
/*test symbol and its value
        System.out.println("SYMBOL: " + symbol);
        System.out.println("COIN ID: " + coinId);*/

    }

    @PostConstruct
    public void loadCoins() {
        try {

            String url = config.getBaseUrl()+"/coins/list";

            CoinGeckoCoin[] coins = restTemplate.getForObject(url, CoinGeckoCoin[].class);

            for (CoinGeckoCoin coin : coins) {
                symbolToId.put(
                        coin.getSymbol().toUpperCase(),
                        coin.getId()
                );
            }

            // ✅ Force correct mappings (VERY IMPORTANT)
            symbolToId.put("BTC", "bitcoin");
            symbolToId.put("ETH", "ethereum");
            symbolToId.put("BNB", "binancecoin");
            symbolToId.put("USDT", "tether");
            symbolToId.put("SOL", "solana");
            symbolToId.put("ADA", "cardano");
            symbolToId.put("XRP", "ripple");
            symbolToId.put("DOGE", "dogecoin");
            symbolToId.put("TRX", "tron");
            symbolToId.put("USDC", "usd-coin");
            symbolToId.put("LEO", "leo-token");   // 🔥 VERY IMPORTANT

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Cacheable(value = "cryptoPrices", key = "#symbols")
    public Map<String, Double> getPrices(List<String> symbols) {

        List<String> ids = new ArrayList<>();

        for (String symbol : symbols) {

            String coinId = symbolToId.get(symbol.toUpperCase());

            if (coinId != null) {
                ids.add(coinId);
            }
        }

        if(ids.isEmpty()){
            return new HashMap<>();
        }

        String joinedIds = String.join(",", ids);

        String url =
                "https://api.coingecko.com/api/v3/simple/price?ids="
                        + joinedIds +
                        "&vs_currencies=usd";

        Map<String, Map<String, Object>> response =
                restTemplate.getForObject(url, Map.class);

        Map<String, Double> prices = new HashMap<>();

        for (String id : response.keySet()) {

            Map<String, Object> coinData = response.get(id);

            if (coinData == null || coinData.get("usd") == null) {
                System.out.println("⚠️ Missing price for coin id: " + id);
                continue; // skip safely
            }

            Number price = (Number) coinData.get("usd");

            // 🔥 convert ID → SYMBOL
            String symbol = symbolToId.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(id))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(id);

            prices.put(symbol, price.doubleValue());
        }

        return prices;
    }

    @Cacheable("coinIds")
    public String getCoinId(String symbol) {
        return symbolToId.get(symbol.toUpperCase());
    }

    // neww
    public Map<String, String> getCoinIds(List<String> symbols) {

        Map<String, String> coinIds = new HashMap<>();

        for (String symbol : symbols) {
            coinIds.put(symbol, getCoinId(symbol));
        }

        return coinIds;
    }
}