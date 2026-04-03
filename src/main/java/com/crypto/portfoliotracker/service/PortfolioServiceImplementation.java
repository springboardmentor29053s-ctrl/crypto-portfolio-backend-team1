package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.HoldingRepository;
import com.crypto.portfoliotracker.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PortfolioServiceImplementation implements PortfolioService {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TradeRepository tradeRepository;

    public List<Holding> getUserHoldings(User user) {
        return holdingRepository.findByUser(user);
    }

    public List<Trade> getUserTrades(User user) {
        return tradeRepository.findByUserOrderByExecutedAtAsc(user);
    }

    public Trade saveTrade(Trade trade) {
        return tradeRepository.save(trade);
    }

    public Trade getTradeById(Long id) {
        return tradeRepository.findById(id).orElse(null);
    }

    public void deleteTrade(Long id) {
        tradeRepository.deleteById(id);
    }
}
