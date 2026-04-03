package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.PLReport;
import com.crypto.portfoliotracker.entity.TaxTransaction;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.PLReportRepository;
import com.crypto.portfoliotracker.repository.TaxTransactionRepository;
import com.crypto.portfoliotracker.repository.TradeRepository;
import com.crypto.portfoliotracker.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PLReportService {
    
    @Autowired
    private PLReportRepository plReportRepository;
    
    @Autowired
    private TaxTransactionRepository taxTransactionRepository;
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private HoldingRepository holdingRepository;
    
    @Autowired
    private PricingService pricingService;
    
    /**
     * Generate comprehensive P&L report for user
     */
    public PLReport generatePLReport(User user, String reportType, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Get trades within date range
            List<Trade> trades = tradeRepository.findByUserAndExecutedAtBetweenOrderByExecutedAtAsc(user, startDate, endDate);
            
            // Get current holdings
            List<Holding> holdings = holdingRepository.findByUser(user);
            
            // Create report
            PLReport report = new PLReport(user, generateReportName(reportType, startDate, endDate), reportType, startDate, endDate);
            
            // Calculate metrics
            calculatePLMetrics(report, trades, holdings, startDate, endDate);
            
            // Save report
            report.setGeneratedAt(LocalDateTime.now());
            return plReportRepository.save(report);
            
        } catch (Exception e) {
            System.err.println("Error generating P&L report: " + e.getMessage());
            throw new RuntimeException("Failed to generate P&L report", e);
        }
    }
    
    /**
     * Calculate all P&L metrics
     */
    private void calculatePLMetrics(PLReport report, List<Trade> trades, List<Holding> holdings, 
                                   LocalDateTime startDate, LocalDateTime endDate) {
        
        // Initialize totals
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal realizedGains = BigDecimal.ZERO;
        BigDecimal realizedLosses = BigDecimal.ZERO;
        BigDecimal unrealizedGains = BigDecimal.ZERO;
        BigDecimal unrealizedLosses = BigDecimal.ZERO;
        
        Map<String, List<Trade>> tradesBySymbol = trades.stream()
            .collect(Collectors.groupingBy(Trade::getAssetSymbol));
        
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        
        // Get current prices for all assets
        for (String symbol : tradesBySymbol.keySet()) {
            try {
                Map<String, Object> priceData = pricingService.getCurrentPrice(symbol);
                BigDecimal currentPrice = (BigDecimal) priceData.get("price");
                currentPrices.put(symbol, currentPrice);
            } catch (Exception e) {
                // Use last trade price if current price unavailable
                BigDecimal lastPrice = tradesBySymbol.get(symbol).get(tradesBySymbol.get(symbol).size() - 1).getPrice();
                currentPrices.put(symbol, (BigDecimal) lastPrice);
            }
        }
        
        // Calculate realized P&L from completed trades (FIFO)
        for (Map.Entry<String, List<Trade>> entry : tradesBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<Trade> symbolTrades = entry.getValue();
            
            // Sort by date for FIFO calculation
            symbolTrades.sort(Comparator.comparing(Trade::getExecutedAt));
            
            // Calculate using FIFO
            Queue<Trade> buyQueue = new LinkedList<>();
            BigDecimal symbolRealizedGains = BigDecimal.ZERO;
            BigDecimal symbolRealizedLosses = BigDecimal.ZERO;
            
            for (Trade trade : symbolTrades) {
                if (trade.getSide() == com.crypto.portfoliotracker.entity.Trade.TradeSide.BUY) {
                    buyQueue.add(trade);
                    totalInvested = totalInvested.add(trade.getQuantity().multiply(trade.getPrice()));
                    totalFees = totalFees.add(trade.getFee() != null ? trade.getFee() : BigDecimal.ZERO);
                } else if (trade.getSide() == com.crypto.portfoliotracker.entity.Trade.TradeSide.SELL) {
                    BigDecimal sellQuantity = trade.getQuantity();
                    BigDecimal sellPrice = trade.getPrice();
                    BigDecimal sellFees = trade.getFee() != null ? trade.getFee() : BigDecimal.ZERO;
                    
                    while (sellQuantity.compareTo(BigDecimal.ZERO) > 0 && !buyQueue.isEmpty()) {
                        Trade buyTrade = buyQueue.peek();
                        BigDecimal buyQuantity = buyTrade.getQuantity();
                        BigDecimal buyPrice = buyTrade.getPrice();
                        BigDecimal buyFees = buyTrade.getFee() != null ? buyTrade.getFee() : BigDecimal.ZERO;
                        
                        BigDecimal tradeQuantity = sellQuantity.min(buyQuantity);
                        BigDecimal costBasis = buyPrice.multiply(tradeQuantity).add(
                            buyFees.multiply(tradeQuantity).divide(buyQuantity, 8, RoundingMode.HALF_UP));
                        BigDecimal proceeds = sellPrice.multiply(tradeQuantity).subtract(
                            sellFees.multiply(tradeQuantity).divide(trade.getQuantity(), 8, RoundingMode.HALF_UP));
                        
                        BigDecimal pl = proceeds.subtract(costBasis);
                        
                        if (pl.compareTo(BigDecimal.ZERO) >= 0) {
                            symbolRealizedGains = symbolRealizedGains.add(pl);
                            realizedGains = realizedGains.add(pl);
                        } else {
                            symbolRealizedLosses = symbolRealizedLosses.add(pl.abs());
                            realizedLosses = realizedLosses.add(pl.abs());
                        }
                        
                        sellQuantity = sellQuantity.subtract(tradeQuantity);
                        buyQuantity = buyQuantity.subtract(tradeQuantity);
                        
                        if (buyQuantity.compareTo(BigDecimal.ZERO) == 0) {
                            buyQueue.poll();
                        } else {
                            buyTrade.setQuantity(buyQuantity);
                        }
                    }
                    
                    totalFees = totalFees.add(sellFees);
                }
            }
            
            // Calculate unrealized P&L for remaining holdings
            BigDecimal remainingQuantity = buyQueue.stream()
                .map(Trade::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgCost = buyQueue.stream()
                    .map(t -> t.getPrice().multiply(t.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(remainingQuantity, 8, RoundingMode.HALF_UP);
                
                BigDecimal currentPrice = currentPrices.get(symbol);
                BigDecimal currentValue = currentPrice.multiply(remainingQuantity);
                BigDecimal costBasis = avgCost.multiply(remainingQuantity);
                BigDecimal unrealizedPL = currentValue.subtract(costBasis);
                
                if (unrealizedPL.compareTo(BigDecimal.ZERO) >= 0) {
                    unrealizedGains = unrealizedGains.add(unrealizedPL);
                } else {
                    unrealizedLosses = unrealizedLosses.add(unrealizedPL.abs());
                }
                
                totalValue = totalValue.add(currentValue);
            }
        }
        
        // Add current holdings value
        for (Holding holding : holdings) {
            BigDecimal currentPrice = currentPrices.getOrDefault(holding.getAssetSymbol(), BigDecimal.ZERO);
            totalValue = totalValue.add(currentPrice.multiply(holding.getQuantity()));
        }
        
        // Calculate totals
        BigDecimal totalProfitLoss = realizedGains.add(realizedLosses.negate())
            .add(unrealizedGains).add(unrealizedLosses.negate());
        
        BigDecimal totalProfitLossPercentage = totalInvested.compareTo(BigDecimal.ZERO) > 0 ?
            totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
            BigDecimal.ZERO;
        
        // Calculate trade statistics
        int totalTrades = trades.size();
        int winningTrades = 0;
        int losingTrades = 0;
        long totalHoldingDays = 0;
        int tradeCount = 0;
        
        for (Trade trade : trades) {
            if (trade.getSide() == com.crypto.portfoliotracker.entity.Trade.TradeSide.SELL) {
                // Find corresponding buy to calculate holding period
                for (Trade buyTrade : trades) {
                    if (buyTrade.getSide() == com.crypto.portfoliotracker.entity.Trade.TradeSide.BUY &&
                        buyTrade.getAssetSymbol().equals(trade.getAssetSymbol()) &&
                        buyTrade.getExecutedAt().isBefore(trade.getExecutedAt())) {
                        
                        long holdingDays = ChronoUnit.DAYS.between(buyTrade.getExecutedAt(), trade.getExecutedAt());
                        totalHoldingDays += holdingDays;
                        tradeCount++;
                        break;
                    }
                }
            }
        }
        
        // Calculate win rate
        Map<String, BigDecimal> symbolPL = new HashMap<>();
        for (Trade trade : trades) {
            if (trade.getSide() == com.crypto.portfoliotracker.entity.Trade.TradeSide.SELL) {
                BigDecimal pl = calculateTradePL(trade, trades);
                symbolPL.merge(trade.getAssetSymbol(), pl, BigDecimal::add);
            }
        }
        
        for (BigDecimal pl : symbolPL.values()) {
            if (pl.compareTo(BigDecimal.ZERO) >= 0) {
                winningTrades++;
            } else {
                losingTrades++;
            }
        }
        
        BigDecimal winRate = totalTrades > 0 ? 
            new BigDecimal(winningTrades).divide(new BigDecimal(totalTrades), 4, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        Integer averageHoldingPeriodDays = tradeCount > 0 ? (int) (totalHoldingDays / tradeCount) : 0;
        
        // Set report values
        report.setTotalInvested(totalInvested);
        report.setTotalValue(totalValue);
        report.setTotalProfitLoss(totalProfitLoss);
        report.setTotalProfitLossPercentage(totalProfitLossPercentage);
        report.setRealizedGains(realizedGains);
        report.setRealizedLosses(realizedLosses);
        report.setUnrealizedGains(unrealizedGains);
        report.setUnrealizedLosses(unrealizedLosses);
        report.setTotalFees(totalFees);
        report.setTotalTrades(totalTrades);
        report.setWinningTrades(winningTrades);
        report.setLosingTrades(losingTrades);
        report.setWinRate(winRate);
        report.setAverageHoldingPeriodDays(averageHoldingPeriodDays);
    }
    
    /**
     * Calculate P&L for a single trade
     */
    private BigDecimal calculateTradePL(Trade sellTrade, List<Trade> allTrades) {
        // Find corresponding buy trades (simplified FIFO)
        List<Trade> buyTrades = allTrades.stream()
            .filter(t -> t.getSide() == com.crypto.portfoliotracker.entity.Trade.TradeSide.BUY &&
                        t.getAssetSymbol().equals(sellTrade.getAssetSymbol()) &&
                        t.getExecutedAt().isBefore(sellTrade.getExecutedAt()))
            .sorted(Comparator.comparing(Trade::getExecutedAt))
            .collect(Collectors.toList());
        
        BigDecimal totalPL = BigDecimal.ZERO;
        BigDecimal remainingSellQuantity = sellTrade.getQuantity();
        
        for (Trade buyTrade : buyTrades) {
            if (remainingSellQuantity.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal tradeQuantity = remainingSellQuantity.min(buyTrade.getQuantity());
            BigDecimal pl = sellTrade.getPrice().subtract(buyTrade.getPrice()).multiply(tradeQuantity);
            totalPL = totalPL.add(pl);
            remainingSellQuantity = remainingSellQuantity.subtract(tradeQuantity);
        }
        
        return totalPL;
    }
    
    /**
     * Generate report name based on type and dates
     */
    private String generateReportName(String reportType, LocalDateTime startDate, LocalDateTime endDate) {
        switch (reportType) {
            case "MONTHLY":
                return startDate.getMonth() + " " + startDate.getYear() + " P&L Report";
            case "QUARTERLY":
                return "Q" + ((startDate.getMonthValue() - 1) / 3 + 1) + " " + startDate.getYear() + " P&L Report";
            case "YEARLY":
                return startDate.getYear() + " P&L Report";
            default:
                return "Custom P&L Report (" + startDate.toLocalDate() + " to " + endDate.toLocalDate() + ")";
        }
    }
    
    /**
     * Get all user reports
     */
    public List<PLReport> getUserReports(User user) {
        return plReportRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Get report by ID
     */
    public PLReport getReportById(User user, Long reportId) {
        return plReportRepository.findByUserAndId(user, reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));
    }
    
    /**
     * Delete report
     */
    public void deleteReport(User user, Long reportId) {
        PLReport report = getReportById(user, reportId);
        plReportRepository.delete(report);
    }
    
    /**
     * Generate monthly report
     */
    public PLReport generateMonthlyReport(User user, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
        return generatePLReport(user, "MONTHLY", startDate, endDate);
    }
    
    /**
     * Generate quarterly report
     */
    public PLReport generateQuarterlyReport(User user, int year, int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDateTime startDate = LocalDateTime.of(year, startMonth, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(3).minusSeconds(1);
        return generatePLReport(user, "QUARTERLY", startDate, endDate);
    }
    
    /**
     * Generate yearly report
     */
    public PLReport generateYearlyReport(User user, int year) {
        LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year + 1, 1, 1, 0, 0).minusSeconds(1);
        return generatePLReport(user, "YEARLY", startDate, endDate);
    }
}
