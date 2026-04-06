package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.*;
import com.crypto.portfolio.model.Holding;
import com.crypto.portfolio.model.PortfolioSnapshot;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.HoldingRepository;
import com.crypto.portfolio.repository.PortfolioSnapshotRepository;
import com.crypto.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final CryptoMarketService cryptoMarketService;
    private final UserRepository userRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PriceCacheService priceCacheService;


    public List<PortfolioResponse> getPortfolio(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Holding> holdings = holdingRepository.findByUser(user);

        // Step 1: Aggregate holdings by symbol
        Map<String, List<Holding>> grouped =
                holdings.stream()
                        .collect(Collectors.groupingBy(
                                h -> h.getAssetSymbol().toUpperCase()
                        ));

        //List<String> symbols = new ArrayList<>(grouped.keySet());
        Set<String> symbols = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .collect(Collectors.toSet());

        Map<String, Double> prices =
                priceCacheService.getPrices(symbols);

        List<PortfolioResponse> portfolio = new ArrayList<>();

        for (String symbol : symbols) {

            List<Holding> assetHoldings = grouped.get(symbol);

            double totalQuantity = 0;
            double totalCost = 0;

            for (Holding holding : assetHoldings) {
                double qty = holding.getQuantity();
                double avgCost = holding.getAvgCost();

                totalQuantity += qty;
                totalCost += qty * avgCost;
            }

            double avgCost = totalQuantity == 0 ? 0 : totalCost / totalQuantity;

            // ✅ FIXED LINE
            Double currentPrice = prices.get(symbol);

            if (currentPrice == null) {
                System.out.println("⚠️ Missing price for " + symbol);
                continue;
            }

            double value = totalQuantity * currentPrice;
            double profitLoss = (currentPrice - avgCost) * totalQuantity;

            portfolio.add(new PortfolioResponse(
                    symbol,
                    totalQuantity,
                    avgCost,
                    currentPrice,
                    value,
                    profitLoss
            ));
        }

        return portfolio;
    }

    public PortfolioValueResponse getPortfolioValue(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Holding> holdings = holdingRepository.findByUser(user);

        /*List<String> symbols = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .distinct()
                .toList();*/
        Set<String> symbols = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .collect(Collectors.toSet());

        Map<String, Double> prices =
                priceCacheService.getPrices(symbols);

        double totalValue = 0;
        double totalProfitLoss = 0;
        double totalInvested = 0;

        for (Holding holding : holdings) {

            String symbol = holding.getAssetSymbol().toUpperCase();

            //String coinId = cryptoMarketService.getCoinId(symbol);

            Double currentPrice = prices.get(symbol);

            boolean priceAvailable = currentPrice != null;
            if (!priceAvailable) currentPrice = 0.0;

            double quantity = holding.getQuantity();
            double avgCost = holding.getAvgCost();

            double value = quantity * currentPrice;
            double invested = quantity * avgCost;

            double profitLoss = value - invested;

            totalValue += value;
            totalProfitLoss += profitLoss;
            totalInvested += invested;
        }

        double profitPercentage = 0;

        if (totalInvested > 0) {
            profitPercentage = (totalProfitLoss / totalInvested) * 100;
        }
        System.out.println("Prices Map: " + prices);
        return new PortfolioValueResponse(
                Math.round(totalValue * 100.0) / 100.0,
                Math.round(totalProfitLoss * 100.0) / 100.0,
                Math.round(profitPercentage * 100.0) / 100.0
        );
    }


    private double round(double value, int scale) {
        return new BigDecimal(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }



    public List<PortfolioAllocationResponse> getAllocation(String username) {


        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Holding> holdings = holdingRepository.findByUser(user);

        List<String> symbols = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .distinct()
                .toList();

        Map<String, String> coinIds =
                cryptoMarketService.getCoinIds(symbols);

        Set<String> symbol = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .collect(Collectors.toSet());
        Map<String, Double> prices = priceCacheService.getPrices(symbol);

        // Calculate asset values
        Map<String, Double> assetValues = holdings.stream()
                .map(h -> {
                    String symbol1 = h.getAssetSymbol().toUpperCase();
                    String coinId = coinIds.get(symbol1);
                    Double price = prices.get(symbol1);;

                    if (price == null) return null;

                    return Map.entry(symbol1, h.getQuantity() * price);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::sum
                ));

        double totalPortfolioValue = assetValues.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        if (totalPortfolioValue == 0) {
            return List.of();
        }

        double othersValue = 0;

        List<PortfolioAllocationResponse> allocation = new ArrayList<>();

        for (Map.Entry<String, Double> entry : assetValues.entrySet()) {

            double percentage = (entry.getValue() / totalPortfolioValue) * 100;

            if (percentage < 1.0) {
                othersValue += entry.getValue();
                continue;
            }

            allocation.add(
                    new PortfolioAllocationResponse(
                            entry.getKey(),
                            round(entry.getValue(), 2),
                            round(percentage, 4)
                    )
            );
        }

        if (othersValue > 0) {
            allocation.add(
                    new PortfolioAllocationResponse(
                            "OTHERS",
                            round(othersValue, 2),
                            round((othersValue / totalPortfolioValue) * 100, 4)
                    )
            );
        }

        allocation.sort((a, b) ->
                Double.compare(b.getPercentage(), a.getPercentage()));

        return allocation;
    }


    private List<List<Double>> sampleTimeline(List<List<Double>> timeline, int targetPoints) {

        if (timeline.size() <= targetPoints) {
            return timeline;
        }

        List<List<Double>> sampled = new ArrayList<>();

        int step = timeline.size() / targetPoints;

        for (int i = 0; i < timeline.size(); i += step) {
            sampled.add(timeline.get(i));
        }

        return sampled;
    }
    public double calculateChangePercent(List<PortfolioPerformancePoint> data) {

        if (data.size() < 2) return 0;

        double first = data.get(0).getValue();
        double last = data.get(data.size() - 1).getValue();

        return ((last - first) / first) * 100;
    }

    @Cacheable(value = "portfolioPerformance", key = "#username + '-' + #days")
    public List<PortfolioPerformancePoint> getPortfolioPerformance(String username, int days) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        List<PortfolioSnapshot> snapshots =
                snapshotRepository.findByUserIdAndTimestampAfterOrderByTimestampAsc(
                        user.getId(),
                        fromDate
                );

        List<PortfolioPerformancePoint> result = new ArrayList<>();

        for (PortfolioSnapshot snapshot : snapshots) {

            result.add(
                    new PortfolioPerformancePoint(
                            snapshot.getTimestamp()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli(),
                            snapshot.getPortfolioValue()
                    )
            );
        }

        return result;
    }

    public List<PortfolioQuantityAllocationResponse> getQuantityAllocation(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Holding> holdings = holdingRepository.findByUser(user);

        Map<String, Double> assetQuantities =
                holdings.stream()
                        .collect(Collectors.toMap(
                                h -> h.getAssetSymbol().toUpperCase(),
                                Holding::getQuantity,
                                Double::sum
                        ));

        double totalQuantity = assetQuantities.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        List<PortfolioQuantityAllocationResponse> result = new ArrayList<>();

        for (Map.Entry<String, Double> entry : assetQuantities.entrySet()) {

            double quantity = entry.getValue();

            double percentage = (quantity / totalQuantity) * 100;

            result.add(
                    new PortfolioQuantityAllocationResponse(
                            entry.getKey(),
                            quantity,
                            Math.round(percentage * 100.0) / 100.0
                    )
            );
        }

        return result;
    }



    public PortfolioSummaryResponse getPortfolioSummary(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Holding> holdings = holdingRepository.findByUser(user);

        /*List<String> symbols = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .toList();*/
        Set<String> symbols = holdings.stream()
                .map(h -> h.getAssetSymbol().toUpperCase())
                .collect(Collectors.toSet());

        Map<String, Double> prices =
                priceCacheService.getPrices(symbols);

        double totalInvestment = 0;
        double currentValue = 0;

        for (Holding holding : holdings) {

            String symbol = holding.getAssetSymbol().toUpperCase();



            Double currentPrice = prices.get(symbol);;

            if (currentPrice == null) continue;

            double quantity = holding.getQuantity();
            double avgCost = holding.getAvgCost();

            totalInvestment += avgCost * quantity;
            currentValue += currentPrice * quantity;
        }

        double profit = currentValue - totalInvestment;

        double profitPercentage =
                totalInvestment == 0 ? 0 :
                        (profit / totalInvestment) * 100;

        PortfolioSummaryResponse response = new PortfolioSummaryResponse();

        response.setTotalInvestment(totalInvestment);
        response.setCurrentValue(currentValue);
        response.setProfit(profit);
        response.setProfitPercentage(profitPercentage);

        return response;
    }


    public ProfitLossResponse getProfitLoss(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Holding> holdings = holdingRepository.findByUser(user);

        if (holdings.isEmpty()) {
            return new ProfitLossResponse(0,0,0,0,new ArrayList<>());
        }

        // ✅ Step 1 — aggregate holdings by symbol
        Map<String, Holding> mergedHoldings = new HashMap<>();

        for (Holding h : holdings) {

            String symbol = h.getAssetSymbol().toUpperCase();

            mergedHoldings.putIfAbsent(symbol, new Holding());

            Holding existing = mergedHoldings.get(symbol);

            double qty = h.getQuantity() == null ? 0 : h.getQuantity();
            double avgCost = h.getAvgCost() == null ? 0 : h.getAvgCost();

            double invested = qty * avgCost;

            double existingQty = existing.getQuantity() == null ? 0 : existing.getQuantity();
            double existingInvested =
                    (existing.getQuantity() == null || existing.getAvgCost() == null)
                            ? 0
                            : existing.getQuantity() * existing.getAvgCost();

            double totalQty = existingQty + qty;
            double totalInvested = existingInvested + invested;

            existing.setQuantity(totalQty);

            if (totalQty > 0) {
                existing.setAvgCost(totalInvested / totalQty);
            }

            existing.setAssetSymbol(symbol);
        }

        List<String> symbols = new ArrayList<>(mergedHoldings.keySet());

        // Step 2 — get coinIds
        Map<String, String> coinIds = cryptoMarketService.getCoinIds(symbols);

        Set<String> symbolsSet = mergedHoldings.keySet();

        Map<String, Double> prices = priceCacheService.getPrices(symbolsSet);

        double totalValue = 0;
        double totalInvested = 0;
        double totalProfitLoss = 0;

        List<AssetPL> assetList = new ArrayList<>();

        // ✅ Step 4 — now iterate merged holdings
        for (Holding holding : mergedHoldings.values()) {

            String symbol1 = holding.getAssetSymbol();
            String coinId = coinIds.get(symbol1);

            Double currentPrice = prices.get(symbol1); // ✅ FIXED

            if (currentPrice == null) continue;

            double quantity = holding.getQuantity();
            double avgCost = holding.getAvgCost();

            double invested = quantity * avgCost;
            double value = quantity * currentPrice;

            double profitLoss = value - invested;

            double profitPercentage = 0;
            if (invested > 0) {
                profitPercentage = (profitLoss / invested) * 100;
            }

            totalValue += value;
            totalInvested += invested;
            totalProfitLoss += profitLoss;

            assetList.add(
                    new AssetPL(
                            symbol1,
                            quantity,
                            avgCost,
                            currentPrice,
                            round(invested, 2),
                            round(value, 2),
                            round(profitLoss, 2),
                            round(profitPercentage, 2)
                    )
            );
        }

        double totalPercentage = 0;
        if (totalInvested > 0) {
            totalPercentage = (totalProfitLoss / totalInvested) * 100;
        }

        return new ProfitLossResponse(
                round(totalValue, 2),
                round(totalInvested, 2),
                round(totalProfitLoss, 2),
                round(totalPercentage, 2),
                assetList
        );
    }

    public double getPerformanceChange(List<PortfolioPerformancePoint> data) {

        if (data.size() < 2) return 0;

        double first = data.get(0).getValue();
        double last = data.get(data.size() - 1).getValue();

        return ((last - first) / first) * 100;
    }
}