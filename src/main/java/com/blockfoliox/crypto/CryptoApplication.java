package com.blockfoliox.crypto;

import com.blockfoliox.crypto.model.Exchange;
import com.blockfoliox.crypto.repository.ExchangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoApplication {

    private static final Logger log = LoggerFactory.getLogger(CryptoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CryptoApplication.class, args);
    }

    @Bean
    CommandLineRunner seedExchange(ExchangeRepository exchangeRepository) {
        return args -> {
            if (exchangeRepository.findByName("Binance") == null) {
                Exchange exchange = new Exchange();
                exchange.setName("Binance");
                exchange.setBaseUrl("https://testnet.binance.vision");
                exchange.setActive(true);
                exchangeRepository.save(exchange);
                log.info(" Binance exchange seeded");
            } else {
                log.info(" Binance exchange already exists");
            }
        };
    }
}