package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.PriceSnapshot;
import com.crypto.portfoliotracker.repository.PriceSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PriceSnapshotScheduler {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    // List of top cryptocurrencies to track
    private final List<String> TRACKED_SYMBOLS = Arrays.asList(
        "BTC", "ETH", "BNB", "ADA", "SOL", "XRP", "DOT", "DOGE", 
        "AVAX", "MATIC", "LINK", "UNI", "LTC", "BCH", "ATOM", 
        "VET", "FIL", "TRX", "ETC", "XLM"
    );

    // Every 5 minutes - capture prices for major cryptocurrencies
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    @Transactional
    public void captureMajorCryptoPrices() {
        try {
            for (String symbol : TRACKED_SYMBOLS) {
                try {
                    Map<String, Object> priceData = pricingService.getCurrentPrice(symbol);
                    if (!priceData.containsKey("error")) {
                        Map<String, Object> data = (Map<String, Object>) priceData;
                        Object price = data.get("usd");
                        if (price != null) {
                            PriceSnapshot snapshot = new PriceSnapshot();
                            snapshot.setAssetSymbol(symbol);
                            snapshot.setPriceUsd(new BigDecimal(price.toString()));
                            snapshot.setSource("CoinGecko");
                            priceSnapshotRepository.save(snapshot);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to capture price for " + symbol + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in price snapshot scheduler: " + e.getMessage());
        }
    }

    // Every hour - capture prices for user portfolio symbols
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    public void captureUserPortfolioPrices() {
        try {
            List<String> portfolioSymbols = priceSnapshotRepository.findAllDistinctSymbols();
            
            for (String symbol : portfolioSymbols) {
                try {
                    Map<String, Object> priceData = pricingService.getCurrentPrice(symbol);
                    if (!priceData.containsKey("error")) {
                        Map<String, Object> data = (Map<String, Object>) priceData;
                        Object price = data.get("usd");
                        if (price != null) {
                            PriceSnapshot snapshot = new PriceSnapshot();
                            snapshot.setAssetSymbol(symbol);
                            snapshot.setPriceUsd(new BigDecimal(price.toString()));
                            snapshot.setSource("CoinGecko");
                            priceSnapshotRepository.save(snapshot);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to capture portfolio price for " + symbol + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in portfolio price snapshot scheduler: " + e.getMessage());
        }
    }

    // Daily at midnight - clean up old price snapshots
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupOldSnapshots() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            priceSnapshotRepository.deleteByCapturedAtBefore(cutoff);
        } catch (Exception e) {
            System.err.println("Error in cleanup scheduler: " + e.getMessage());
        }
    }

    // Every 30 minutes - update trending coins
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    @Transactional
    public void updateTrendingCoins() {
        try {
            List<String> trendingSymbols = Arrays.asList(
                "BTC", "ETH", "BNB", "ADA", "SOL", "XRP", "DOT", "DOGE", 
                "AVAX", "MATIC"
            );
            
            for (String symbol : trendingSymbols) {
                try {
                    Map<String, Object> priceData = pricingService.getCurrentPrice(symbol);
                    if (!priceData.containsKey("error")) {
                        Map<String, Object> data = (Map<String, Object>) priceData;
                        Object price = data.get("usd");
                        if (price != null) {
                            PriceSnapshot snapshot = new PriceSnapshot();
                            snapshot.setAssetSymbol(symbol);
                            snapshot.setPriceUsd(new BigDecimal(price.toString()));
                            snapshot.setSource("CoinGecko");
                            priceSnapshotRepository.save(snapshot);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to update trending " + symbol + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in trending coins scheduler: " + e.getMessage());
        }
    }
}
