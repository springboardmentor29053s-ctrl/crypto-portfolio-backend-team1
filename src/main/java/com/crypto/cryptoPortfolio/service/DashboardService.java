package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.dto.DashboardAssetResponse;
import com.crypto.cryptoPortfolio.dto.DashboardLiveResponse;
import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final HoldingRepository holdingRepository;
    private final CoinGeckoService coinGeckoService;

    public DashboardService(HoldingRepository holdingRepository,
                            CoinGeckoService coinGeckoService) {
        this.holdingRepository = holdingRepository;
        this.coinGeckoService = coinGeckoService;
    }

    public DashboardLiveResponse getLiveDashboard(Long userId) {

        List<Holding> holdings =
                holdingRepository.findByUserId(userId);

        List<String> coinIds = new ArrayList<>();

        for (Holding h : holdings) {
            if (h.getQuantity().doubleValue() > 0) {
                coinIds.add(mapToCoinGeckoId(h.getAssetSymbol()));
            }
        }

        Map<String, Map<String, Double>> prices =
                coinGeckoService.getPrices(coinIds);

        List<DashboardAssetResponse> assets = new ArrayList<>();

        double totalValue = 0;
        double totalCost = 0;

        for (Holding h : holdings) {

            double quantity = h.getQuantity().doubleValue();
            if (quantity <= 0) continue;

            double avgCost = h.getAvgCost().doubleValue();

            String coinId =
                    mapToCoinGeckoId(h.getAssetSymbol());

            Map<String, Double> coinData = prices.getOrDefault(coinId, new HashMap<>());

            double currentPrice = coinData.getOrDefault("price", 0.0);
            double change24h = coinData.getOrDefault("change24h", 0.0);


            double currentValue = quantity * currentPrice;
            double invested = quantity * avgCost;
            double profitLoss = currentValue - invested;

            double percent = invested == 0 ? 0 :
                    (profitLoss / invested) * 100;

            DashboardAssetResponse dto =
                    new DashboardAssetResponse();

            dto.setAsset(h.getAssetSymbol());
            dto.setQuantity(quantity);
            dto.setAvgCost(avgCost);
            dto.setCurrentPrice(currentPrice);
            dto.setCurrentValue(currentValue);
            dto.setProfitLoss(profitLoss);
            dto.setProfitLossPercent(percent);
            dto.setChange24h(change24h);

            assets.add(dto);

            totalValue += currentValue;
            totalCost += invested;
        }

        DashboardLiveResponse response =
                new DashboardLiveResponse();

        response.setAssets(assets);
        response.setTotalValue(totalValue);

        double totalPL = totalValue - totalCost;
        response.setTotalProfitLoss(totalPL);

        double totalPercent =
                totalCost == 0 ? 0 : (totalPL / totalCost) * 100;

        response.setTotalProfitLossPercent(totalPercent);

        return response;
    }

    private String mapToCoinGeckoId(String symbol) {

        switch (symbol) {
            case "BTC": return "bitcoin";
            case "ETH": return "ethereum";
            case "BNB": return "binancecoin";
            default: return symbol.toLowerCase();
        }
    }
}

