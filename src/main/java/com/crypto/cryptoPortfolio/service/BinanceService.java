package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.dto.PortfolioResponse;
import com.crypto.cryptoPortfolio.entity.*;
import com.crypto.cryptoPortfolio.repository.TradeRepository;
import com.crypto.cryptoPortfolio.security.EncryptionUtil;
import com.crypto.cryptoPortfolio.security.HmacUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
public class BinanceService {

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final TradeRepository tradeRepository;
    private final Map<String, BigDecimal> priceCache = new HashMap<>();
    private Instant lastCacheTime = Instant.now();
    private final String BASE_URL = "https://testnet.binance.vision";

    private static final List<String> COMMON_SYMBOLS = List.of(
            "BTCUSDT",
            "ETHUSDT",
            "SOLUSDT",
            "BNBUSDT",
            "XRPUSDT",
            "ADAUSDT",
            "DOGEUSDT"
    );

    public BinanceService(RestTemplate restTemplate,
                          EncryptionUtil encryptionUtil,
                          TradeRepository tradeRepository) {
        this.restTemplate = restTemplate;
        this.encryptionUtil = encryptionUtil;
        this.tradeRepository = tradeRepository;
    }

    // ===============================
    // FETCH BINANCE SERVER TIME
    // ===============================
    private long fetchBinanceServerTime() {
        try {
            String url = BASE_URL + "/api/v3/time";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            long serverTime = ((Number) response.getBody().get("serverTime")).longValue();
            System.out.println("Binance server time: " + serverTime +
                    " | Local time: " + System.currentTimeMillis() +
                    " | Diff: " + (System.currentTimeMillis() - serverTime) + "ms");
            return serverTime;
        } catch (Exception e) {
            System.out.println("Failed to fetch Binance time, using local: " + e.getMessage());
            return System.currentTimeMillis();
        }
    }

    // ===============================
    // FETCH USER TRADES
    // ===============================
    public List<Map<String, Object>> fetchUserTrades(
            ApiKey apiKeyEntity,
            String symbol) {

        String apiKey = encryptionUtil.decrypt(apiKeyEntity.getApiKey()).trim();
        String secret = encryptionUtil.decrypt(apiKeyEntity.getApiSecret()).trim();

        long timestamp = fetchBinanceServerTime();

        String queryString = "symbol=" + symbol +
                "&timestamp=" + timestamp;

        String signature = HmacUtil.generateSignature(secret, queryString);

        String url = BASE_URL + "/api/v3/myTrades?" +
                queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            return response.getBody();
        } catch (Exception e) {
            System.out.println("Invalid or unsupported symbol: " + symbol);
            return Collections.emptyList();
        }
    }

    // ===============================
    // FETCH ACCOUNT BALANCES
    // ===============================
    public List<PortfolioResponse> fetchPortfolio(ApiKey apiKeyEntity) {

        String apiKey = encryptionUtil.decrypt(apiKeyEntity.getApiKey()).trim();
        String secret = encryptionUtil.decrypt(apiKeyEntity.getApiSecret()).trim();

        long timestamp = fetchBinanceServerTime();
        String queryString = "timestamp=" + timestamp;

        String signature = HmacUtil.generateSignature(secret, queryString);

        String url = BASE_URL + "/api/v3/account?" +
                queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        List<Map<String, String>> balances =
                (List<Map<String, String>>) response.getBody().get("balances");

        List<PortfolioResponse> result = new ArrayList<>();

        for (Map<String, String> balance : balances) {

            BigDecimal free = new BigDecimal(balance.get("free"));
            BigDecimal locked = new BigDecimal(balance.get("locked"));

            if (free.compareTo(BigDecimal.ZERO) > 0 ||
                    locked.compareTo(BigDecimal.ZERO) > 0) {

                result.add(new PortfolioResponse(
                        balance.get("asset"),
                        free.toPlainString(),
                        locked.toPlainString()
                ));
            }
        }

        return result;
    }

    // ===============================
    // SYNC TRADES
    // ===============================
    public int syncTrades(ApiKey apiKeyEntity,
                          String symbol,
                          User user,
                          Exchange exchange) {

        List<Map<String, Object>> trades =
                fetchUserTrades(apiKeyEntity, symbol);

        if (trades == null || trades.isEmpty()) {
            System.out.println("No trades returned from Binance for symbol: " + symbol);
            return 0;
        }

        int savedCount = 0;

        for (Map<String, Object> t : trades) {

            Long externalId = ((Number) t.get("id")).longValue();

            if (tradeRepository.existsByExternalTradeId(externalId)) {
                continue;
            }

            boolean isBuyer = (Boolean) t.get("isBuyer");

            Trade trade = new Trade();
            trade.setExternalTradeId(externalId);
            trade.setUser(user);
            trade.setExchange(exchange);
            trade.setAssetSymbol(symbol);
            trade.setSide(isBuyer ? TradeSide.BUY : TradeSide.SELL);
            trade.setQuantity(new BigDecimal((String) t.get("qty")));
            trade.setPrice(new BigDecimal((String) t.get("price")));
            trade.setFee(new BigDecimal((String) t.get("commission")));

            long time = ((Number) t.get("time")).longValue();
            trade.setExecutedAt(
                    Instant.ofEpochMilli(time)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
            );

            tradeRepository.save(trade);
            savedCount++;

            System.out.println("Saved trade → ID: " + externalId +
                    " | Side: " + trade.getSide() +
                    " | Symbol: " + symbol +
                    " | Qty: " + trade.getQuantity() +
                    " | Price: " + trade.getPrice());
        }

        System.out.println("Sync complete for " + symbol + ". New trades saved: " + savedCount);
        return savedCount;
    }

    // ===============================
    // PLACE ORDER
    // ===============================
    public String placeOrder(ApiKey apiKeyEntity,
                             String symbol,
                             String side,
                             String type,
                             String quantity) {

        String apiKey = encryptionUtil.decrypt(apiKeyEntity.getApiKey()).trim();
        String secret = encryptionUtil.decrypt(apiKeyEntity.getApiSecret()).trim();

        long timestamp = fetchBinanceServerTime();
        symbol = symbol.toUpperCase();

        String queryString =
                "symbol=" + symbol +
                        "&side=" + side +
                        "&type=" + type +
                        "&quantity=" + quantity +
                        "&timestamp=" + timestamp;

        String signature = HmacUtil.generateSignature(secret, queryString);

        String url = BASE_URL + "/api/v3/order?" +
                queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        System.out.println("Binance placeOrder response: " + response.getBody());

        return response.getBody();
    }

    // ===============================
    // GET CURRENT PRICE
    // ===============================
    public BigDecimal getCurrentPrice(String symbol) {

        if (!symbol.endsWith("USDT")) {
            symbol = symbol + "USDT";
        }

        if (priceCache.containsKey(symbol) &&
                Instant.now().minusSeconds(30).isBefore(lastCacheTime)) {
            return priceCache.get(symbol);
        }

        try {
            String url = BASE_URL + "/api/v3/ticker/price?symbol=" + symbol;

            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);

            BigDecimal price =
                    new BigDecimal(response.getBody().get("price").toString());

            priceCache.put(symbol, price);
            lastCacheTime = Instant.now();

            return price;

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}