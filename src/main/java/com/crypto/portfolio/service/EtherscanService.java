package com.crypto.portfolio.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EtherscanService {

    @Value("${etherscan.api.key}")
    private String apiKey;
    private static final String BaseUrl = "https://api.etherscan.io/v2/api";

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String,Object> getContractInfo(String contractAddress){

        String url =
                BaseUrl
                        + "?chainid=1"
                        + "&module=contract"
                        + "&action=getsourcecode"
                        + "&address=" + contractAddress
                        + "&apikey=" + apiKey;

        System.out.println("Calling Etherscan: " + contractAddress);

        return restTemplate.getForObject(url, Map.class);
    }

}