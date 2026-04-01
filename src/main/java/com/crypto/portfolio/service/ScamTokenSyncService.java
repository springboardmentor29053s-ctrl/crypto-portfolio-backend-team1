package com.crypto.portfolio.service;

import com.crypto.portfolio.model.Coin;
import com.crypto.portfolio.model.Holding;
import com.crypto.portfolio.model.ScamToken;
import com.crypto.portfolio.repository.CoinRepository;
import com.crypto.portfolio.repository.HoldingRepository;
import com.crypto.portfolio.repository.ScamTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
 //
@Service
@RequiredArgsConstructor
public class ScamTokenSyncService {

     private final ScamTokenRepository scamTokenRepository;
     private final CoinRepository coinRepository;
     private final HoldingRepository holdingRepository;


     private final RestTemplate restTemplate = new RestTemplate();

     public void syncScamTokens() {


         List<Holding> holdings = holdingRepository.findAll();

         for (Holding holding : holdings) {

             String symbol = holding.getAssetSymbol();


             Optional<Coin> coinOpt = coinRepository.findBySymbol(symbol);

             if (coinOpt.isEmpty()) {
                 continue;
             }

             Coin coin = coinOpt.get();

             String contract = coin.getContractAddress();

             if (contract == null || contract.isEmpty()) {
                 continue;
             }

             Optional<ScamToken> existing =
                     scamTokenRepository.findByContractAddress(contract);

             if (existing.isPresent()) {
                 continue;
             }

             ScamToken token = new ScamToken();
             token.setChain("ETH");
             token.setContractAddress(contract);
             token.setRiskLevel(ScamToken.RiskLevel.valueOf("HIGH"));
             token.setSource("Scanner");
             token.setLastSeen(LocalDateTime.now());

             scamTokenRepository.save(token);
         }
     }
 }