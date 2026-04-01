package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.RiskAlertResponse;
import com.crypto.portfolio.model.*;
import com.crypto.portfolio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RiskService {

    private final HoldingRepository holdingRepository;
    private final ScamTokenRepository scamTokenRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final UserRepository userRepository;
    private final CoinRepository coinRepository;

    public void scanAllUsers() {

        List<User> users = userRepository.findAll();

        // 🔥 Load once
        Set<String> scamContracts = scamTokenRepository.findAll()
                .stream()
                .map(ScamToken::getContractAddress)
                .collect(Collectors.toSet());

        Map<String, Coin> coinMap = coinRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getSymbol().toUpperCase(),
                        c -> c
                ));

        for (User user : users) {

            List<Holding> holdings = holdingRepository.findByUser(user);

            if (holdings.isEmpty()) continue;

            Set<String> existingAlerts = riskAlertRepository.findByUser(user)
                    .stream()
                    .map(RiskAlert::getAssetSymbol)
                    .collect(Collectors.toSet());

            for (Holding holding : holdings) {

                String symbol = holding.getAssetSymbol().toUpperCase();

                Coin coin = coinMap.get(symbol);

                if (coin == null) continue;

                String contractAddress = coin.getContractAddress();

                if (contractAddress == null) continue;

                if (!scamContracts.contains(contractAddress)) continue;

                if (existingAlerts.contains(symbol)) continue;

                RiskAlert alert = new RiskAlert();
                alert.setUser(user);
                alert.setAssetSymbol(symbol);
                alert.setAlertType("SCAM_TOKEN");
                alert.setDetails("Token flagged as scam");
                alert.setCreatedAt(LocalDateTime.now());

                riskAlertRepository.save(alert);
            }
        }

        System.out.println("Risk scan completed (optimized)");
    }

    public List<RiskAlertResponse> getUserAlerts(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<RiskAlert> alerts = riskAlertRepository.findByUser(user);

        return alerts.stream()
                .map(alert -> new RiskAlertResponse(
                        alert.getAssetSymbol(),
                        alert.getAlertType(),
                        alert.getDetails(),
                        alert.getCreatedAt()
                ))
                .toList();
    }
    public void scanUserHoldings(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔥 Load shared data once
        Set<String> scamContracts = scamTokenRepository.findAll()
                .stream()
                .map(ScamToken::getContractAddress)
                .collect(Collectors.toSet());

        Map<String, Coin> coinMap = coinRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getSymbol().toUpperCase(),
                        c -> c
                ));

        List<Holding> holdings = holdingRepository.findByUser(user);

        if (holdings.isEmpty()) return;

        Set<String> existingAlerts = riskAlertRepository.findByUser(user)
                .stream()
                .map(RiskAlert::getAssetSymbol)
                .collect(Collectors.toSet());

        for (Holding holding : holdings) {

            String symbol = holding.getAssetSymbol().toUpperCase();

            Coin coin = coinMap.get(symbol);
            if (coin == null) continue;

            String contractAddress = coin.getContractAddress();
            if (contractAddress == null) continue;

            if (!scamContracts.contains(contractAddress)) continue;
            if (existingAlerts.contains(symbol)) continue;

            RiskAlert alert = new RiskAlert();
            alert.setUser(user);
            alert.setAssetSymbol(symbol);
            alert.setAlertType("SCAM_TOKEN");
            alert.setDetails("Token flagged as scam");
            alert.setCreatedAt(LocalDateTime.now());

            riskAlertRepository.save(alert);
        }
    }
}