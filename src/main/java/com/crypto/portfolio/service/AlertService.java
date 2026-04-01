package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.AlertRequest;
import com.crypto.portfolio.dto.PortfolioValueResponse;
import com.crypto.portfolio.model.Alert;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.AlertRepository;
import com.crypto.portfolio.repository.ExchangeRepository;
import com.crypto.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final CryptoMarketService cryptoMarketService;
    private final PortfolioService portfolioService;
    public void createAlert(String username, AlertRequest request) {


        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Alert alert = new Alert();

        alert.setUser(user);
        alert.setType(request.getType());
        alert.setSymbol(request.getSymbol() != null
                ? request.getSymbol().toUpperCase()
                : null);
        alert.setTargetValue(request.getTargetValue());
        alert.setTriggered(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    private void checkPriceAlert(Alert alert) {

        String symbol = alert.getSymbol();


        Double currentPrice =
                cryptoMarketService.getCurrentPrice(symbol);

        if (currentPrice >= alert.getTargetValue()) {

            alert.setTriggered(true);

            System.out.println("🔥 PRICE ALERT TRIGGERED: " + symbol);

            alertRepository.save(alert);
        }
    }

    private void checkProfitAlert(Alert alert) {

        String username = alert.getUser().getName();

        PortfolioValueResponse portfolio =
                portfolioService.getPortfolioValue(username);

        double profitPercent = portfolio.getProfitPercentage();

        if (profitPercent >= alert.getTargetValue()) {

            alert.setTriggered(true);

            System.out.println("🚀 PROFIT ALERT TRIGGERED: " + username);

            alertRepository.save(alert);
        }
    }

    public void processAlerts() {

        List<Alert> alerts = alertRepository.findByTriggeredFalse();

        for (Alert alert : alerts) {

            if ("PRICE".equals(alert.getType())) {
                checkPriceAlert(alert);
            }

            else if ("PROFIT".equals(alert.getType())) {
                checkProfitAlert(alert);
            }
        }
    }
}
