package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;

import java.util.List;

public interface PortfolioService {
    
    List<Holding> getUserHoldings(User user);
    
    List<Trade> getUserTrades(User user);
    
    Trade saveTrade(Trade trade);
    
    Trade getTradeById(Long id);
    
    void deleteTrade(Long id);
}
