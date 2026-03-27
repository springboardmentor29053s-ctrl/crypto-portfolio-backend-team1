package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.entity.Trade;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.TradeRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.PnlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/pnl")
public class PnlController {

    private final PnlService       pnlService;
    private final TradeRepository  tradeRepository;
    private final UserRepository   userRepository;

    public PnlController(PnlService pnlService,
                         TradeRepository tradeRepository,
                         UserRepository userRepository) {
        this.pnlService      = pnlService;
        this.tradeRepository = tradeRepository;
        this.userRepository  = userRepository;
    }

    /**
     * GET /api/pnl/summary
     * Returns realized P&L per coin + totals.
     */
    @GetMapping("/summary")
    public ResponseEntity<PnlService.PnlSummaryResponse> getPnlSummary() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(pnlService.getPnlSummary(user.getId()));
    }

    /**
     * GET /api/pnl/export/csv
     * Downloads full trade history as a CSV file.
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        User user = getAuthenticatedUser();

        List<Trade> trades = tradeRepository.findByUserIdOrderByExecutedAtDesc(user.getId());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Symbol,Type,Quantity,Price (USD),Total (USD),Status\n");

        for (Trade trade : trades) {
            // BigDecimal fields - safe .doubleValue()
            double qty   = trade.getQuantity() != null ? trade.getQuantity().doubleValue() : 0.0;
            double price = trade.getPrice()    != null ? trade.getPrice().doubleValue()    : 0.0;
            double total = qty * price;

            String dateStr = trade.getExecutedAt() != null
                    ? trade.getExecutedAt().format(fmt) : "";

            // TradeSide enum - use .name() to get "BUY" or "SELL"
            String side = trade.getSide() != null ? trade.getSide().name() : "";

            csv.append(String.format("%s,%s,%s,%.8f,%.6f,%.2f,COMPLETED\n",
                    dateStr,
                    trade.getAssetSymbol(),
                    side,
                    qty,
                    price,
                    total
            ));
        }

        byte[] bytes = csv.toString().getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "trade_history.csv");
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // Helper
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + auth.getName()));
    }
}