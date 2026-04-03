package com.crypto.portfoliotracker;

import com.crypto.portfoliotracker.entity.Exchange;
import com.crypto.portfoliotracker.repository.ExchangeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoPortfolioTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoPortfolioTrackerApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDefaultExchange(ExchangeRepository exchangeRepository) {
        return args -> {
            // Create default exchange if none exists
            if (exchangeRepository.count() == 0) {
                Exchange defaultExchange = new Exchange();
                defaultExchange.setName("Default Exchange");
                defaultExchange.setBaseUrl("https://api.default.com");
                exchangeRepository.save(defaultExchange);
                System.out.println("Default exchange created");
            }
        };
    }
}
