package com.crypto.portfolio.service;

import com.crypto.portfolio.model.Coin;
import com.crypto.portfolio.repository.CoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoinSyncService {

    private final CoinRepository coinRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public void syncCoins(){

        String url =
                "https://api.coingecko.com/api/v3/coins/list?include_platform=true";

        List<Map<String,Object>> coins =
                restTemplate.getForObject(url,List.class);

        for(Map<String,Object> c : coins){

            String symbol =
                    ((String)c.get("symbol")).toUpperCase();

            String name = (String)c.get("name");

            String id = (String)c.get("id");

            Map<String,String> platforms =
                    (Map<String,String>) c.get("platforms");

            if(platforms == null) continue;

            String ethAddress = platforms.get("ethereum");

            if(ethAddress == null || ethAddress.isEmpty())
                continue;

            Optional<Coin> existing =
                    coinRepository.findBySymbol(symbol);

            if(existing.isPresent()) continue;

            Coin coin = new Coin();

            coin.setSymbol(symbol);
            coin.setName(name);
            coin.setChain("ETH");
            coin.setContractAddress(ethAddress);
            coin.setCoingeckoId(id);

            coinRepository.save(coin);
        }
    }
}