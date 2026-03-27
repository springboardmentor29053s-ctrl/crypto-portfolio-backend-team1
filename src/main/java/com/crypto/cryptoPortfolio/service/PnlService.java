package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.entity.Trade;
import com.crypto.cryptoPortfolio.entity.TradeSide;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PnlService {

    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;

    public PnlService(TradeRepository tradeRepository,
                      HoldingRepository holdingRepository) {
        this.tradeRepository   = tradeRepository;
        this.holdingRepository = holdingRepository;
    }

    public PnlSummaryResponse getPnlSummary(Long userId) {

        List<Trade>   allTrades = tradeRepository.findByUserIdOrderByExecutedAtAsc(userId);
        List<Holding> holdings  = holdingRepository.findByUserId(userId);

        // Group trades by symbol
        Map<String, List<Trade>> bySymbol = allTrades.stream()
                .collect(Collectors.groupingBy(Trade::getAssetSymbol));

        List<CoinPnl> coinPnls       = new ArrayList<>();
        double        totalRealized  = 0.0;
        double        totalInvested  = 0.0;

        for (Map.Entry<String, List<Trade>> entry : bySymbol.entrySet()) {
            String       symbol = entry.getKey();
            List<Trade>  trades = entry.getValue();

            // FIFO queue: each lot = [qty, price] as double
            Deque<double[]> buyLots      = new ArrayDeque<>();
            double          realizedPnl  = 0.0;
            double          totalBuyVal  = 0.0;

            for (Trade trade : trades) {
                // ✅ BigDecimal → double
                double qty   = trade.getQuantity() != null
                        ? trade.getQuantity().doubleValue() : 0.0;
                double price = trade.getPrice() != null
                        ? trade.getPrice().doubleValue() : 0.0;

                // ✅ TradeSide enum comparison (not String)
                if (TradeSide.BUY.equals(trade.getSide())) {
                    buyLots.addLast(new double[]{qty, price});
                    totalBuyVal += qty * price;

                } else if (TradeSide.SELL.equals(trade.getSide())) {
                    double sellRevenue = qty * price;
                    double costBasis   = 0.0;
                    double remaining   = qty;

                    while (remaining > 0 && !buyLots.isEmpty()) {
                        double[] lot = buyLots.peekFirst();
                        if (lot[0] <= remaining) {
                            costBasis += lot[0] * lot[1];
                            remaining -= lot[0];
                            buyLots.pollFirst();
                        } else {
                            costBasis += remaining * lot[1];
                            lot[0]    -= remaining;
                            remaining  = 0;
                        }
                    }
                    realizedPnl += (sellRevenue - costBasis);
                }
            }

            // ── Match with current holding ────────────────────────────────────
            double holdingQty    = 0.0;
            double avgCost       = 0.0;
            double investedValue = 0.0;

            Optional<Holding> holdingOpt = holdings.stream()
                    .filter(h -> h.getAssetSymbol().equalsIgnoreCase(symbol))
                    .findFirst();

            if (holdingOpt.isPresent()) {
                Holding h = holdingOpt.get();

                // ✅ Holding.quantity is BigDecimal
                holdingQty = h.getQuantity() != null
                        ? h.getQuantity().doubleValue() : 0.0;

                // ✅ Holding.avgCost is BigDecimal — field is named avgCost
                avgCost = h.getAvgCost() != null
                        ? h.getAvgCost().doubleValue() : 0.0;

                investedValue  = holdingQty * avgCost;
                totalInvested += investedValue;
            }

            totalRealized += realizedPnl;

            double realizedPct = totalBuyVal > 0
                    ? round((realizedPnl / totalBuyVal) * 100, 2) : 0.0;

            coinPnls.add(new CoinPnl(
                    symbol,
                    round(realizedPnl,   2),
                    round(realizedPct,   2),
                    round(holdingQty,    8),
                    round(avgCost,       6),
                    round(investedValue, 2),
                    round(totalBuyVal,   2)
            ));
        }

        coinPnls.sort(Comparator.comparing(CoinPnl::getSymbol));

        double totalRealizedPct = totalInvested > 0
                ? round((totalRealized / totalInvested) * 100, 2) : 0.0;

        return new PnlSummaryResponse(
                round(totalRealized,    2),
                round(totalRealizedPct, 2),
                round(totalInvested,    2),
                coinPnls
        );
    }

    private double round(double value, int places) {
        return BigDecimal.valueOf(value)
                .setScale(places, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class CoinPnl {
        private String symbol;
        private double realizedPnl;
        private double realizedPnlPercent;
        private double holdingQty;
        private double avgCost;
        private double investedValue;
        private double totalBuyValue;

        public CoinPnl(String symbol, double realizedPnl, double realizedPnlPercent,
                       double holdingQty, double avgCost,
                       double investedValue, double totalBuyValue) {
            this.symbol             = symbol;
            this.realizedPnl        = realizedPnl;
            this.realizedPnlPercent = realizedPnlPercent;
            this.holdingQty         = holdingQty;
            this.avgCost            = avgCost;
            this.investedValue      = investedValue;
            this.totalBuyValue      = totalBuyValue;
        }

        public String getSymbol()            { return symbol; }
        public double getRealizedPnl()        { return realizedPnl; }
        public double getRealizedPnlPercent() { return realizedPnlPercent; }
        public double getHoldingQty()         { return holdingQty; }
        public double getAvgCost()            { return avgCost; }
        public double getInvestedValue()      { return investedValue; }
        public double getTotalBuyValue()      { return totalBuyValue; }
    }

    public static class PnlSummaryResponse {
        private double        totalRealizedPnl;
        private double        totalRealizedPnlPercent;
        private double        totalInvested;
        private List<CoinPnl> coins;

        public PnlSummaryResponse(double totalRealizedPnl, double totalRealizedPnlPercent,
                                  double totalInvested, List<CoinPnl> coins) {
            this.totalRealizedPnl        = totalRealizedPnl;
            this.totalRealizedPnlPercent = totalRealizedPnlPercent;
            this.totalInvested           = totalInvested;
            this.coins                   = coins;
        }

        public double        getTotalRealizedPnl()        { return totalRealizedPnl; }
        public double        getTotalRealizedPnlPercent() { return totalRealizedPnlPercent; }
        public double        getTotalInvested()           { return totalInvested; }
        public List<CoinPnl> getCoins()                   { return coins; }
    }
}