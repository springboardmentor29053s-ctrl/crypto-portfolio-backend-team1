package com.crypto.portfolio.model;

// depricated
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String coinId;

    private Double quantity;
    private Double price;

    private String type; // BUY or SELL

    private LocalDateTime createdAt;
}