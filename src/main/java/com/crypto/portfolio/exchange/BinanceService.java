package com.crypto.portfolio.exchange;

import com.crypto.portfolio.model.*;
import com.crypto.portfolio.repository.*;
import com.crypto.portfolio.service.HoldingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BinanceService {

    private final BinanceClient binanceClient;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final ExchangeRepository exchangeRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final HoldingService holdingService;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🔥 MAIN ENTRY
    public void syncAll(String username) {
        syncBalances(username);
        syncTrades(username);
    }

    // ================= BALANCE SYNC =================
    public void syncBalances(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository
                .findByNameIgnoreCase("BINANCE")   // ✅ FIXED
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        ApiKey apiKey = apiKeyRepository
                .findByUserAndExchange(user, exchange)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        String url = binanceClient.buildAccountUrl(apiKey.getApiSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map<String, Object> body = response.getBody();

        if (body == null || !body.containsKey("balances")) {
            throw new RuntimeException("Invalid response from Binance");
        }

        List<Map<String, String>> balances =
                (List<Map<String, String>>) body.get("balances");

        Set<String> syncedAssets = new HashSet<>();

        for (Map<String, String> balance : balances) {

            String asset = balance.get("asset").toUpperCase();

            double free = Double.parseDouble(balance.get("free"));
            double locked = Double.parseDouble(balance.get("locked"));

            double total = free + locked;

            if (total <= 0) continue;

            syncedAssets.add(asset);

            saveOrUpdateHolding(user, exchange, asset, total);
        }

        // 🔥 FIXED: DO NOT DELETE
        List<Holding> existingHoldings =
                holdingRepository.findByUserAndExchange(user, exchange);

        for (Holding holding : existingHoldings) {

            if (!syncedAssets.contains(holding.getAssetSymbol())) {

                holding.setQuantity(0.0);   // ✅ FIX
                holding.setUpdatedAt(LocalDateTime.now());

                holdingRepository.save(holding);
            }
        }
    }

    private void saveOrUpdateHolding(User user, Exchange exchange, String asset, double quantity) {

        Optional<Holding> existing =
                holdingRepository.findByUserAndExchangeAndAssetSymbol(user, exchange, asset);

        Holding holding;

        if (existing.isPresent()) {
            holding = existing.get();
            holding.setQuantity(quantity);
        } else {
            holding = new Holding();
            holding.setUser(user);
            holding.setExchange(exchange);
            holding.setAssetSymbol(asset);
            holding.setAvgCost(0.0);
            holding.setQuantity(quantity);
        }

        holding.setUpdatedAt(LocalDateTime.now());
        holdingRepository.save(holding);
    }

    // ================= TRADE SYNC =================
    public void syncTrades(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository
                .findByNameIgnoreCase("BINANCE")
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        ApiKey apiKey = apiKeyRepository
                .findByUserAndExchange(user, exchange)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<Holding> holdings =
                holdingRepository.findByUserAndExchange(user, exchange);

        for (Holding holding : holdings) {

            String assetSymbols = holding.getAssetSymbol().toUpperCase();
            String binanceSymbol = assetSymbols + "USDT";

            // 🔥 STEP 1: Get last synced trade
            Optional<Trade> lastTradeOpt =
                    tradeRepository.findTopByUserAndExchangeAndAssetSymbolOrderByExecutedAtDesc(
                            user, exchange, assetSymbols
                    );

            Long startTime = null;

            if (lastTradeOpt.isPresent()) {
                startTime = lastTradeOpt.get()
                        .getExecutedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }

            try {

                String url = binanceClient.buildTradeUrl(
                        apiKey.getApiSecret(),
                        binanceSymbol,
                        startTime
                );

                ResponseEntity<List> response =
                        restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

                List<Map<String, Object>> trades = response.getBody();

                if (trades == null || trades.isEmpty()) continue;

                for (Map<String, Object> tradeData : trades) {

                    String assetSymbol = binanceSymbol.replace("USDT", "");

                    double price = Double.parseDouble((String) tradeData.get("price"));
                    double quantity = Double.parseDouble((String) tradeData.get("qty"));

                    boolean isBuyer = (Boolean) tradeData.get("isBuyer");

                    long time = ((Number) tradeData.get("time")).longValue();

                    LocalDateTime executedAt =
                            Instant.ofEpochMilli(time)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();

                    Optional<Trade> existingTrade =
                            tradeRepository.findByExchangeIdAndExecutedAtAndAssetSymbol(
                                    exchange.getId(),
                                    executedAt,
                                    assetSymbol
                            );

                    if (existingTrade.isPresent()) continue;

                    Trade trade = new Trade();
                    trade.setUser(user);
                    trade.setExchange(exchange);
                    trade.setAssetSymbol(assetSymbol);
                    trade.setPrice(price);
                    trade.setQuantity(quantity);
                    trade.setFee(0.0);
                    trade.setExecutedAt(executedAt);
                    trade.setSide(isBuyer ? Trade.Side.BUY : Trade.Side.SELL);

                    tradeRepository.save(trade);

                    // 🔥 Update holding
                    if (isBuyer) {
                        holdingService.updateHolding(user, exchange, assetSymbol, quantity, price);
                    } else {
                        holdingService.reduceHolding(user, exchange, assetSymbol, quantity);
                    }
                }

            } catch (Exception e) {
                continue;
            }
        }
    }
}
