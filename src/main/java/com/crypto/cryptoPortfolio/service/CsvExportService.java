package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.Trade;
import com.crypto.cryptoPortfolio.entity.TradeSide;
import com.crypto.cryptoPortfolio.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates two CSV formats matching your existing trade data structure:
 *
 * 1. Trade History CSV  — matches your existing exported format exactly
 *    Columns: Date, Symbol, Type, Quantity, Price (USD), Total (USD), Realized P&L (USD), Status
 *
 * 2. Tax Report CSV — FIFO-based, SELL trades only, tax-ready
 *    Columns: Date Sold, Asset, Qty Sold, Avg Buy Price (USD), Sell Price (USD),
 *             Cost Basis (USD), Proceeds (USD), Realized Gain/Loss (USD), Type
 *
 * Uses same FIFO logic as your PnlService so numbers are consistent.
 */
@Service
public class CsvExportService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Kolkata"));

    private final TradeRepository tradeRepository;

    public CsvExportService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Trade History CSV — your existing format + Realized P&L column
    // ─────────────────────────────────────────────────────────────────────────

    public String generateTradeHistoryCsv(Long userId) {
        // Newest first for display
        List<Trade> trades = tradeRepository.findByUserIdOrderByExecutedAtDesc(userId);

        // Pre-compute FIFO profits so sell rows get the P&L column populated
        Map<Long, Double> profitByTradeId = computeFifoProfits(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("Date,Symbol,Type,Quantity,Price (USD),Total (USD),Realized P&L (USD),Status\n");

        for (Trade trade : trades) {
            String date  = FMT.format(trade.getExecutedAt());
            String sym   = trade.getAssetSymbol();
            String type  = TradeSide.BUY.equals(trade.getSide()) ? "BUY" : "SELL";
            double qty   = trade.getQuantity() != null ? trade.getQuantity().doubleValue() : 0.0;
            double price = trade.getPrice()    != null ? trade.getPrice().doubleValue()    : 0.0;
            double total = round(qty * price, 2);

            String pnlCol = "";
            if (TradeSide.SELL.equals(trade.getSide())) {
                double profit = profitByTradeId.getOrDefault(trade.getId(), 0.0);
                pnlCol = String.valueOf(round(profit, 2));
            }

            sb.append(String.join(",",
                    esc(date), esc(sym), type,
                    String.valueOf(qty),
                    String.valueOf(round(price, 2)),
                    String.valueOf(total),
                    pnlCol,
                    "COMPLETED"
            )).append("\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Tax Report CSV — SELL trades only with FIFO cost basis
    // ─────────────────────────────────────────────────────────────────────────

    public String generateTaxReportCsv(Long userId, Integer year) {
        // Oldest first — required for FIFO (same as PnlService)
        List<Trade> allTrades = tradeRepository.findByUserIdOrderByExecutedAtAsc(userId);

        Map<String, List<Trade>> bySymbol = allTrades.stream()
                .collect(Collectors.groupingBy(Trade::getAssetSymbol));

        double totalGains  = 0;
        double totalLosses = 0;
        List<String[]> rows = new ArrayList<>();

        for (Map.Entry<String, List<Trade>> entry : bySymbol.entrySet()) {
            String      symbol = entry.getKey();
            List<Trade> trades = entry.getValue();

            // FIFO buy queue — same structure as PnlService: double[]{qty, price}
            Deque<double[]> buyLots = new ArrayDeque<>();

            for (Trade trade : trades) {
                double qty   = trade.getQuantity() != null ? trade.getQuantity().doubleValue() : 0.0;
                double price = trade.getPrice()    != null ? trade.getPrice().doubleValue()    : 0.0;

                if (TradeSide.BUY.equals(trade.getSide())) {
                    buyLots.addLast(new double[]{qty, price});

                } else if (TradeSide.SELL.equals(trade.getSide())) {

                    // Filter by year if provided — still consume from queue
                    int tradeYear = trade.getExecutedAt()
                            .atZone(ZoneId.of("Asia/Kolkata"))
                            .getYear();

                    double proceeds  = qty * price;
                    double costBasis = 0;
                    double matched   = 0;
                    double remaining = qty;

                    // Consume from FIFO queue
                    while (remaining > 0 && !buyLots.isEmpty()) {
                        double[] lot = buyLots.peekFirst();
                        double use = Math.min(remaining, lot[0]);
                        costBasis += use * lot[1];
                        matched   += use;
                        lot[0]    -= use;
                        remaining -= use;
                        if (lot[0] <= 0) buyLots.pollFirst();
                    }

                    // Only add to CSV if matches requested year (or all years)
                    if (year == null || tradeYear == year) {
                        double avgBuyPrice = matched > 0 ? costBasis / matched : 0;
                        double gainLoss    = proceeds - costBasis;
                        String gainType    = gainLoss >= 0 ? "GAIN" : "LOSS";

                        if (gainLoss >= 0) totalGains  += gainLoss;
                        else               totalLosses += Math.abs(gainLoss);

                        rows.add(new String[]{
                                FMT.format(trade.getExecutedAt()),
                                symbol,
                                String.valueOf(qty),
                                String.valueOf(round(avgBuyPrice, 2)),
                                String.valueOf(round(price,       2)),
                                String.valueOf(round(costBasis,   2)),
                                String.valueOf(round(proceeds,    2)),
                                String.valueOf(round(gainLoss,    2)),
                                gainType
                        });
                    }
                }
            }
        }

        // Sort newest first
        rows.sort((a, b) -> b[0].compareTo(a[0]));

        StringBuilder sb = new StringBuilder();
        sb.append("Date Sold,Asset,Qty Sold,Avg Buy Price (USD),Sell Price (USD),"
                + "Cost Basis (USD),Proceeds (USD),Realized Gain/Loss (USD),Type\n");

        for (String[] row : rows) {
            sb.append(Arrays.stream(row).map(this::esc).collect(Collectors.joining(","))).append("\n");
        }

        // Summary footer
        double net = totalGains - totalLosses;
        sb.append("\n");
        sb.append(",,,,,,, Total Gains (USD),").append(round(totalGains,  2)).append("\n");
        sb.append(",,,,,,, Total Losses (USD),").append(round(totalLosses, 2)).append("\n");
        sb.append(",,,,,,, Net Taxable Gain (USD),").append(round(net, 2)).append("\n");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIFO profit per trade ID — mirrors PnlService logic exactly
    // ─────────────────────────────────────────────────────────────────────────

    private Map<Long, Double> computeFifoProfits(Long userId) {
        List<Trade> allTrades = tradeRepository.findByUserIdOrderByExecutedAtAsc(userId);
        Map<String, Deque<double[]>> queues  = new HashMap<>();
        Map<Long, Double>            profits = new HashMap<>();

        for (Trade trade : allTrades) {
            String sym = trade.getAssetSymbol();
            queues.putIfAbsent(sym, new ArrayDeque<>());
            Deque<double[]> q = queues.get(sym);

            double qty   = trade.getQuantity() != null ? trade.getQuantity().doubleValue() : 0.0;
            double price = trade.getPrice()    != null ? trade.getPrice().doubleValue()    : 0.0;

            if (TradeSide.BUY.equals(trade.getSide())) {
                q.addLast(new double[]{qty, price});

            } else if (TradeSide.SELL.equals(trade.getSide())) {
                double proceeds  = qty * price;
                double costBasis = 0;
                double remaining = qty;

                while (remaining > 0 && !q.isEmpty()) {
                    double[] lot = q.peekFirst();
                    double use = Math.min(remaining, lot[0]);
                    costBasis += use * lot[1];
                    lot[0]    -= use;
                    remaining -= use;
                    if (lot[0] <= 0) q.pollFirst();
                }

                profits.put(trade.getId(), proceeds - costBasis);
            }
        }

        return profits;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private double round(double v, int places) {
        return BigDecimal.valueOf(v).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private String esc(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}