package com.crypto.portfolio.model;

import jakarta.persistence.*;
import lombok.Data;
//depricated
@Data
@Entity
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String coinId;

    private Double quantity;
    private Double averageBuyPrice;
}