package com.crypto.portfolio.dto;

import lombok.Data;
import java.util.List;

@Data
public class CoinChartResponse {
    private List<List<Double>> prices;
}
