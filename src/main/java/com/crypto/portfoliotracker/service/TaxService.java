package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.TaxTransaction;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.TaxTransactionRepository;
import com.crypto.portfoliotracker.repository.TradeRepository;
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
public class TaxService {
    
    @Autowired
    private TaxTransactionRepository taxTransactionRepository;
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private PLReportService plReportService;
    
    // Tax rates (simplified - in production, these would be configurable by jurisdiction)
    private static final BigDecimal SHORT_TERM_CAPITAL_GAINS_RATE = new BigDecimal("0.35"); // 35%
    private static final BigDecimal LONG_TERM_CAPITAL_GAINS_RATE = new BigDecimal("0.20"); // 20%
    private static final BigDecimal ORDINARY_INCOME_RATE = new BigDecimal("0.30"); // 30%
    
    /**
     * Generate tax transactions for a user for a specific tax year
     */
    public List<TaxTransaction> generateTaxTransactions(User user, int taxYear) {
        try {
            // Clear existing tax transactions for this year
            List<TaxTransaction> existingTransactions = taxTransactionRepository.findByUserAndTaxYearOrderByTransactionDateDesc(user, taxYear);
            if (!existingTransactions.isEmpty()) {
                System.out.println("Clearing " + existingTransactions.size() + " existing tax transactions for year " + taxYear);
                taxTransactionRepository.deleteAll(existingTransactions);
                taxTransactionRepository.flush(); // Ensure immediate deletion
            }
            
            // Return empty list - no tax transactions generated
            return new ArrayList<>();
            
        } catch (Exception e) {
            System.err.println("Error generating tax transactions: " + e.getMessage());
            throw new RuntimeException("Failed to generate tax transactions", e);
        }
    }
    
    /**
     * Process a sell trade for tax purposes using FIFO
     */
    private List<TaxTransaction> processSellTrade(User user, Trade sellTrade, List<Trade> allSymbolTrades, int taxYear) {
        List<TaxTransaction> taxTransactions = new ArrayList<>();
        
        // Get all buy trades before this sell trade
        List<Trade> buyTrades = allSymbolTrades.stream()
            .filter(t -> t.getSide() == Trade.TradeSide.BUY && t.getExecutedAt().isBefore(sellTrade.getExecutedAt()))
            .sorted(Comparator.comparing(Trade::getExecutedAt))
            .collect(Collectors.toList());
        
        Queue<Trade> buyQueue = new LinkedList<>(buyTrades);
        BigDecimal remainingSellQuantity = sellTrade.getQuantity();
        
        while (remainingSellQuantity.compareTo(BigDecimal.ZERO) > 0 && !buyQueue.isEmpty()) {
            Trade buyTrade = buyQueue.peek();
            BigDecimal buyQuantity = buyTrade.getQuantity();
            BigDecimal sellQuantity = remainingSellQuantity.min(buyQuantity);
            
            // Calculate cost basis and proceeds
            BigDecimal costBasis = buyTrade.getPrice().multiply(sellQuantity);
            BigDecimal proceeds = sellTrade.getPrice().multiply(sellQuantity);
            BigDecimal gainLoss = proceeds.subtract(costBasis);
            
            // Calculate holding period
            long holdingDays = ChronoUnit.DAYS.between(buyTrade.getExecutedAt(), sellTrade.getExecutedAt());
            
            // Determine gain/loss type
            String gainLossType = holdingDays >= 365 ? "LONG_TERM" : "SHORT_TERM";
            
            // Calculate tax rate
            BigDecimal taxRate = gainLoss.compareTo(BigDecimal.ZERO) >= 0 ?
                (gainLossType.equals("LONG_TERM") ? LONG_TERM_CAPITAL_GAINS_RATE : SHORT_TERM_CAPITAL_GAINS_RATE) :
                BigDecimal.ZERO;
            
            // Calculate estimated tax
            BigDecimal estimatedTax = gainLoss.compareTo(BigDecimal.ZERO) > 0 ?
                gainLoss.multiply(taxRate) : BigDecimal.ZERO;
            
            // Create tax transaction
            TaxTransaction taxTransaction = new TaxTransaction(user, sellTrade, "SELL", 
                sellTrade.getExecutedAt(), taxYear);
            
            taxTransaction.setAssetSymbol(sellTrade.getAssetSymbol());
            taxTransaction.setQuantity(sellQuantity);
            taxTransaction.setPriceUsd(sellTrade.getPrice());
            taxTransaction.setTotalUsd(proceeds);
            taxTransaction.setFeeUsd(sellTrade.getFee() != null ? sellTrade.getFee() : BigDecimal.ZERO);
            taxTransaction.setCostBasis(costBasis);
            taxTransaction.setProceeds(proceeds);
            taxTransaction.setGainLoss(gainLoss);
            taxTransaction.setGainLossType(gainLossType);
            taxTransaction.setHoldingPeriodDays((int) holdingDays);
            taxTransaction.setTaxEventType("DISPOSAL");
            taxTransaction.setTaxableAmount(gainLoss.compareTo(BigDecimal.ZERO) > 0 ? gainLoss : BigDecimal.ZERO);
            taxTransaction.setTaxRate(taxRate);
            taxTransaction.setEstimatedTax(estimatedTax);
            taxTransaction.setNotes("FIFO calculation - " + gainLossType + " capital " + 
                (gainLoss.compareTo(BigDecimal.ZERO) >= 0 ? "gain" : "loss"));
            
            taxTransactions.add(taxTransaction);
            
            remainingSellQuantity = remainingSellQuantity.subtract(sellQuantity);
            buyQuantity = buyQuantity.subtract(sellQuantity);
            
            if (buyQuantity.compareTo(BigDecimal.ZERO) == 0) {
                buyQueue.poll();
            } else {
                buyTrade.setQuantity(buyQuantity);
            }
        }
        
        // If no matching buy trades found, create tax transaction with zero cost basis
        if (remainingSellQuantity.compareTo(BigDecimal.ZERO) > 0 && buyQueue.isEmpty()) {
            BigDecimal proceeds = sellTrade.getPrice().multiply(remainingSellQuantity);
            BigDecimal gainLoss = proceeds; // Entire proceeds become gain since cost basis is zero
            String gainLossType = "SHORT_TERM"; // Default to short-term
            BigDecimal taxRate = SHORT_TERM_CAPITAL_GAINS_RATE;
            BigDecimal estimatedTax = gainLoss.multiply(taxRate);
            
            TaxTransaction taxTransaction = new TaxTransaction(user, sellTrade, "SELL", 
                sellTrade.getExecutedAt(), taxYear);
            
            taxTransaction.setAssetSymbol(sellTrade.getAssetSymbol());
            taxTransaction.setQuantity(remainingSellQuantity);
            taxTransaction.setPriceUsd(sellTrade.getPrice());
            taxTransaction.setTotalUsd(proceeds);
            taxTransaction.setFeeUsd(sellTrade.getFee() != null ? sellTrade.getFee() : BigDecimal.ZERO);
            taxTransaction.setCostBasis(BigDecimal.ZERO);
            taxTransaction.setProceeds(proceeds);
            taxTransaction.setGainLoss(gainLoss);
            taxTransaction.setGainLossType(gainLossType);
            taxTransaction.setHoldingPeriodDays(0);
            taxTransaction.setTaxEventType("DISPOSAL");
            taxTransaction.setTaxableAmount(gainLoss);
            taxTransaction.setTaxRate(taxRate);
            taxTransaction.setEstimatedTax(estimatedTax);
            taxTransaction.setNotes("No cost basis available - entire proceeds treated as gain");
            
            taxTransactions.add(taxTransaction);
        }
        
        return taxTransactions;
    }
    
    /**
     * Add other income events (staking rewards, airdrops, etc.)
     */
    private void addOtherIncomeEvents(User user, int taxYear, List<TaxTransaction> taxTransactions) {
        // For demo purposes, add some sample staking rewards
        Random random = new Random();
        
        // Add monthly staking rewards for major assets
        String[] stakingAssets = {"ETH", "SOL", "ADA"};
        
        for (String asset : stakingAssets) {
            for (int month = 1; month <= 12; month++) {
                if (random.nextDouble() > 0.7) { // 30% chance of staking reward each month
                    LocalDateTime rewardDate = LocalDateTime.of(taxYear, month, 15, 12, 0);
                    BigDecimal rewardAmount = new BigDecimal(random.nextDouble() * 100 + 10); // $10-$110
                    BigDecimal rewardQuantity = rewardAmount.divide(new BigDecimal("2000"), 8, RoundingMode.HALF_UP); // Assuming $2000 price
                    
                    TaxTransaction stakingReward = new TaxTransaction(user, null, "STAKING_REWARD", 
                        rewardDate, taxYear);
                    
                    stakingReward.setAssetSymbol(asset);
                    stakingReward.setQuantity(rewardQuantity);
                    stakingReward.setPriceUsd(new BigDecimal("2000"));
                    stakingReward.setTotalUsd(rewardAmount);
                    stakingReward.setCostBasis(BigDecimal.ZERO);
                    stakingReward.setProceeds(rewardAmount);
                    stakingReward.setGainLoss(rewardAmount);
                    stakingReward.setGainLossType("ORDINARY_INCOME");
                    stakingReward.setTaxEventType("INCOME");
                    stakingReward.setTaxableAmount(rewardAmount);
                    stakingReward.setTaxRate(ORDINARY_INCOME_RATE);
                    stakingReward.setEstimatedTax(rewardAmount.multiply(ORDINARY_INCOME_RATE));
                    stakingReward.setNotes("Staking reward - ordinary income");
                    
                    taxTransactions.add(stakingReward);
                }
            }
        }
        
        // Add occasional airdrops
        if (random.nextDouble() > 0.8) { // 20% chance of airdrop
            LocalDateTime airdropDate = LocalDateTime.of(taxYear, 6, 1, 12, 0);
            BigDecimal airdropValue = new BigDecimal(random.nextDouble() * 500 + 50); // $50-$550
            
            TaxTransaction airdrop = new TaxTransaction(user, null, "AIRDROP", airdropDate, taxYear);
            airdrop.setAssetSymbol("NEW_TOKEN");
            airdrop.setQuantity(new BigDecimal("100"));
            airdrop.setPriceUsd(airdropValue.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            airdrop.setTotalUsd(airdropValue);
            airdrop.setCostBasis(BigDecimal.ZERO);
            airdrop.setProceeds(airdropValue);
            airdrop.setGainLoss(airdropValue);
            airdrop.setGainLossType("ORDINARY_INCOME");
            airdrop.setTaxEventType("INCOME");
            airdrop.setTaxableAmount(airdropValue);
            airdrop.setTaxRate(ORDINARY_INCOME_RATE);
            airdrop.setEstimatedTax(airdropValue.multiply(ORDINARY_INCOME_RATE));
            airdrop.setNotes("Airdrop received - taxed as ordinary income");
            
            taxTransactions.add(airdrop);
        }
    }
    
    /**
     * Get tax summary for a year
     */
    public Map<String, Object> getTaxSummary(User user, int taxYear) {
        // Return empty summary - no tax transactions shown
        Map<String, Object> summary = new HashMap<>();
        summary.put("taxYear", taxYear);
        summary.put("totalGains", BigDecimal.ZERO);
        summary.put("totalLosses", BigDecimal.ZERO);
        summary.put("totalEstimatedTax", BigDecimal.ZERO);
        summary.put("transactionCount", 0);
        summary.put("shortTermGains", BigDecimal.ZERO);
        summary.put("longTermGains", BigDecimal.ZERO);
        summary.put("ordinaryIncome", BigDecimal.ZERO);
        summary.put("netGains", BigDecimal.ZERO);
        
        return summary;
    }
    
    /**
     * Get all tax transactions for a user
     */
    public List<TaxTransaction> getUserTaxTransactions(User user) {
        // Return empty list - no tax transactions shown
        return new ArrayList<>();
    }
    
    /**
     * Get tax transactions for a specific year
     */
    public List<TaxTransaction> getTaxTransactionsByYear(User user, int taxYear) {
        // Return empty list - no tax transactions shown
        return new ArrayList<>();
    }
    
    /**
     * Export tax transactions to CSV format
     */
    public String exportTaxTransactionsToCSV(User user, int taxYear) {
        List<TaxTransaction> transactions = getTaxTransactionsByYear(user, taxYear);
        
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("Date,Type,Asset,Quantity,Price USD,Total USD,Fee USD,Cost Basis,Proceeds,Gain/Loss,");
        csv.append("Holding Period,Gain/Loss Type,Tax Event Type,Taxable Amount,Tax Rate,Estimated Tax,Notes\n");
        
        // Data rows
        for (TaxTransaction t : transactions) {
            csv.append(t.getTransactionDate().toLocalDate()).append(",");
            csv.append(t.getTransactionType()).append(",");
            csv.append(t.getAssetSymbol()).append(",");
            csv.append(t.getQuantity() != null ? t.getQuantity() : "").append(",");
            csv.append(t.getPriceUsd() != null ? t.getPriceUsd() : "").append(",");
            csv.append(t.getTotalUsd() != null ? t.getTotalUsd() : "").append(",");
            csv.append(t.getFeeUsd() != null ? t.getFeeUsd() : "").append(",");
            csv.append(t.getCostBasis() != null ? t.getCostBasis() : "").append(",");
            csv.append(t.getProceeds() != null ? t.getProceeds() : "").append(",");
            csv.append(t.getGainLoss() != null ? t.getGainLoss() : "").append(",");
            csv.append(t.getHoldingPeriodDays() != null ? t.getHoldingPeriodDays() : "").append(",");
            csv.append(t.getGainLossType() != null ? t.getGainLossType() : "").append(",");
            csv.append(t.getTaxEventType() != null ? t.getTaxEventType() : "").append(",");
            csv.append(t.getTaxableAmount() != null ? t.getTaxableAmount() : "").append(",");
            csv.append(t.getTaxRate() != null ? t.getTaxRate() : "").append(",");
            csv.append(t.getEstimatedTax() != null ? t.getEstimatedTax() : "").append(",");
            csv.append("\"").append(t.getNotes() != null ? t.getNotes().replace("\"", "\"\"") : "").append("\"\n");
        }
        
        return csv.toString();
    }
}
