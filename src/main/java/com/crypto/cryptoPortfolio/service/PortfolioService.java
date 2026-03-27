package com.crypto.cryptoPortfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.crypto.cryptoPortfolio.dto.HoldingResponse;
import com.crypto.cryptoPortfolio.dto.PerformanceResponse;
import com.crypto.cryptoPortfolio.dto.ProfitLossResponse;
import com.crypto.cryptoPortfolio.entity.*;
import com.crypto.cryptoPortfolio.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final BinanceService binanceService;

    /*
     * ===============================
     * FIFO HOLDINGS CALCULATION
     * ===============================
     */

    public List<HoldingResponse> getHoldingsFIFO(String email, Exchange exchange) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Trade> trades = tradeRepository.findByUserAndExchange(user, exchange);

        if (trades.isEmpty()) {
            return holdingRepository
                    .findByUserIdAndExchangeId(user.getId(), exchange.getId())
                    .stream()
                    .map(h -> {
                        HoldingResponse r = new HoldingResponse();
                        r.setId(h.getId());
                        r.setAssetSymbol(h.getAssetSymbol());
                        r.setNetQuantity(h.getQuantity());
                        r.setAverageCost(h.getAvgCost());
                        r.setTotalInvested(h.getQuantity().multiply(h.getAvgCost()));
                        return r;
                    })
                    .toList();
        }

        Map<String, List<Trade>> grouped =
                trades.stream().collect(Collectors.groupingBy(Trade::getAssetSymbol));

        List<HoldingResponse> responseList = new ArrayList<>();
        List<Trade> updatedTrades = new ArrayList<>();

        for (String symbol : grouped.keySet()) {

            List<Trade> assetTrades = grouped.get(symbol);
            assetTrades.sort(Comparator.comparing(Trade::getExecutedAt));

            Queue<TradeLot> buyQueue = new LinkedList<>();

            for (Trade trade : assetTrades) {
                if (trade.getSide() == TradeSide.BUY) {
                    buyQueue.add(new TradeLot(trade.getQuantity(), trade.getPrice()));
                } else {
                    BigDecimal sellQty = trade.getQuantity();
                    BigDecimal realizedProfit = BigDecimal.ZERO;

                    while (sellQty.compareTo(BigDecimal.ZERO) > 0 && !buyQueue.isEmpty()) {
                        TradeLot lot = buyQueue.peek();
                        BigDecimal matchedQty = sellQty.min(lot.quantity);
                        BigDecimal profit = trade.getPrice().subtract(lot.price).multiply(matchedQty);
                        realizedProfit = realizedProfit.add(profit);
                        lot.quantity = lot.quantity.subtract(matchedQty);
                        sellQty = sellQty.subtract(matchedQty);
                        if (lot.quantity.compareTo(BigDecimal.ZERO) == 0) buyQueue.poll();
                    }

                    trade.setRealizedProfit(realizedProfit);
                    updatedTrades.add(trade);
                }
            }

            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalInvested = BigDecimal.ZERO;

            for (TradeLot lot : buyQueue) {
                totalQuantity = totalQuantity.add(lot.quantity);
                totalInvested = totalInvested.add(lot.quantity.multiply(lot.price));
            }

            if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal averageCost = totalInvested.divide(totalQuantity, 8, RoundingMode.HALF_UP);

                HoldingResponse summary = new HoldingResponse();
                summary.setAssetSymbol(symbol);
                summary.setNetQuantity(totalQuantity);
                summary.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
                summary.setAverageCost(averageCost.setScale(2, RoundingMode.HALF_UP));
                responseList.add(summary);
            }
        }

        if (!updatedTrades.isEmpty()) tradeRepository.saveAll(updatedTrades);

        updateHoldingsTable(user, exchange, responseList);
        return responseList;
    }

    private void updateHoldingsTable(User user, Exchange exchange, List<HoldingResponse> summaries) {
        List<Holding> existing = holdingRepository.findByUserIdAndExchangeId(user.getId(), exchange.getId());
        List<Holding> autoHoldings = existing.stream()
                .filter(h -> !Boolean.TRUE.equals(h.getIsManual()))
                .toList();
        holdingRepository.deleteAll(autoHoldings);

        for (HoldingResponse summary : summaries) {
            Holding holding = new Holding();
            holding.setUser(user);
            holding.setExchange(exchange);
            holding.setAssetSymbol(summary.getAssetSymbol());
            holding.setQuantity(summary.getNetQuantity());
            holding.setAvgCost(summary.getAverageCost());
            holding.setWalletType("SPOT");
            holding.setIsManual(false);
            holding.setUpdatedAt(java.time.LocalDateTime.now());
            holdingRepository.save(holding);
        }
    }

    private static class TradeLot {
        BigDecimal quantity;
        BigDecimal price;
        TradeLot(BigDecimal quantity, BigDecimal price) {
            this.quantity = quantity;
            this.price = price;
        }
    }

    /*
     * ===============================
     * PORTFOLIO CALCULATIONS
     * ===============================
     */

    public BigDecimal calculateUnrealizedPnL(Long userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        BigDecimal totalPnL = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            BigDecimal currentPrice = binanceService.getCurrentPrice(holding.getAssetSymbol());
            BigDecimal pnl = currentPrice.subtract(holding.getAvgCost()).multiply(holding.getQuantity());
            totalPnL = totalPnL.add(pnl);
        }
        return totalPnL;
    }

    public BigDecimal calculateTotalPortfolioValue(Long userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            BigDecimal currentPrice = binanceService.getCurrentPrice(holding.getAssetSymbol());
            totalValue = totalValue.add(holding.getQuantity().multiply(currentPrice));
        }
        return totalValue;
    }

    public ProfitLossResponse getProfitAndLoss(Long userId) {
        BigDecimal unrealized = calculateUnrealizedPnL(userId);
        BigDecimal realized = tradeRepository.sumRealizedProfitByUserId(userId).orElse(BigDecimal.ZERO);
        BigDecimal totalValue = calculateTotalPortfolioValue(userId);
        BigDecimal totalInvested = holdingRepository.findByUserId(userId).stream()
                .map(h -> h.getQuantity().multiply(h.getAvgCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProfitLossResponse response = new ProfitLossResponse();
        response.setTotalValue(totalValue.setScale(2, RoundingMode.HALF_UP));
        response.setTotalUnrealizedPnL(unrealized.setScale(4, RoundingMode.HALF_UP));
        response.setTotalRealizedPnL(realized.setScale(2, RoundingMode.HALF_UP));
        response.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
        return response;
    }

    /*
     * ===============================
     * PRICE SNAPSHOT METHODS
     * ── NEW: Saves per-asset price to price_snapshots table
     * ── Required by MarketRiskService for price drop + volume spike checks
     * ===============================
     */

    /**
     * Runs every 6 hours and saves a PriceSnapshot for every unique holding symbol.
     * This feeds the MarketRiskService price drop and volume spike checks.
     * Cron: 0 0 *\/6 * * * = at minute 0, every 6th hour
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void captureAllPriceSnapshots() {
        log.info("⏰ Price snapshot job started...");

        // Get all unique symbols across all users
        List<Holding> allHoldings = holdingRepository.findAll();
        Set<String> symbols = allHoldings.stream()
                .map(Holding::getAssetSymbol)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());

        for (String symbol : symbols) {
            try {
                savePriceSnapshot(symbol);
            } catch (Exception e) {
                log.error("Failed to save price snapshot for {}: {}", symbol, e.getMessage());
            }
        }

        log.info("⏰ Price snapshot job complete — {} symbol(s) processed", symbols.size());
    }

    /**
     * Saves a single PriceSnapshot for a given symbol.
     * Fetches the current price from Binance.
     * Called by captureAllPriceSnapshots() and also manually for testing.
     */
    public void savePriceSnapshot(String symbol) {
        // Normalise: "ETHUSDT" stays as "ETHUSDT" for Binance,
        // but we store the clean symbol "ETH" for MarketRiskService lookups
        String binanceSymbol = symbol.toUpperCase().endsWith("USDT")
                ? symbol.toUpperCase()
                : symbol.toUpperCase() + "USDT";

        // Clean symbol for storage: strip USDT suffix so MarketRiskService
        // can match "ETH" against holdings like "ETHUSDT"
        String cleanSymbol = binanceSymbol.replace("USDT", "");

        BigDecimal price = binanceService.getCurrentPrice(binanceSymbol);
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Skipping price snapshot for {} — price is 0 or null", symbol);
            return;
        }

        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setAssetSymbol(cleanSymbol);   // stores "ETH", "BTC" etc.
        snapshot.setPriceUsd(price);
        snapshot.setSource("binance");
        snapshot.setCapturedAt(Instant.now());
        // volume24h left null for now — MarketRiskService handles null safely

        priceSnapshotRepository.save(snapshot);
        log.info("Price snapshot saved: {} = ${}", cleanSymbol, price);
    }

    /*
     * ===============================
     * PORTFOLIO SNAPSHOT METHODS (existing — unchanged)
     * ===============================
     */

    @Scheduled(cron = "0 0 0 * * ?")
    public void captureDailySnapshot() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            captureSnapshotForUser(user);
        }
    }

    public void captureSnapshotForUser(User user) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        boolean alreadyExists = portfolioSnapshotRepository
                .existsByUserIdAndCapturedAtAfter(user.getId(), startOfDay);
        if (alreadyExists) return;

        BigDecimal totalValue = calculateTotalPortfolioValue(user.getId());

        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        snapshot.setUser(user);
        snapshot.setTotalValue(totalValue);
        snapshot.setCapturedAt(Instant.now());
        portfolioSnapshotRepository.save(snapshot);
    }

    public void simulateHistoricalSnapshots(User user, int days) {
        BigDecimal currentValue = calculateTotalPortfolioValue(user.getId());
        for (int i = days - 1; i >= 0; i--) {
            Instant snapshotTime = Instant.now().minus(i, ChronoUnit.DAYS);
            double variation = 0.95 + (Math.random() * 0.1);
            BigDecimal historicalValue = currentValue.multiply(BigDecimal.valueOf(variation));

            PortfolioSnapshot snapshot = new PortfolioSnapshot();
            snapshot.setUser(user);
            snapshot.setTotalValue(historicalValue);
            snapshot.setCapturedAt(snapshotTime);
            portfolioSnapshotRepository.save(snapshot);
        }
    }

    public void recalculateAndStoreHoldings(String email, Exchange exchange) {
        getHoldingsFIFO(email, exchange);
    }

    public List<PerformanceResponse> getPerformance(Long userId, int days) {
        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        List<PortfolioSnapshot> snapshots = portfolioSnapshotRepository
                .findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(userId, start);
        return snapshots.stream()
                .map(s -> new PerformanceResponse(
                        s.getCapturedAt(),
                        s.getTotalValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }
}