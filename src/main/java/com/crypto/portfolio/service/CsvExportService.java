package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.PortfolioResponse;
import com.crypto.portfolio.model.Trade;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.TradeRepository;
import com.crypto.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    public ByteArrayInputStream exportPortfolio(String username) throws IOException {

        List<PortfolioResponse> portfolio =
                portfolioService.getPortfolio(username);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        CSVPrinter csvPrinter = new CSVPrinter(
                new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader(
                        "Symbol",
                        "Quantity",
                        "Avg Cost",
                        "Current Price",
                        "Value",
                        "Profit/Loss"
                )
        );

        for (PortfolioResponse p : portfolio) {

            csvPrinter.printRecord(
                    p.getAssetSymbol(),
                    p.getQuantity(),
                    p.getAvgCost(),
                    p.getCurrentPrice(),
                    p.getValue(),
                    p.getProfitLoss()
            );
        }

        csvPrinter.flush();

        return new ByteArrayInputStream(out.toByteArray());
    }

    private final TradeRepository tradeRepository;

    public ByteArrayInputStream exportTrades(String username) throws IOException {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Trade> trades =
                tradeRepository.findByUserOrderByExecutedAtDesc(user);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        CSVPrinter csvPrinter = new CSVPrinter(
                new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader(
                        "Symbol",
                        "Side",
                        "Quantity",
                        "Price",
                        "Date"
                )
        );

        for (Trade trade : trades) {

            csvPrinter.printRecord(
                    trade.getAssetSymbol(),
                    trade.getSide(),
                    trade.getQuantity(),
                    trade.getPrice(),
                    trade.getExecutedAt()
            );
        }

        csvPrinter.flush();

        return new ByteArrayInputStream(out.toByteArray());
    }
}