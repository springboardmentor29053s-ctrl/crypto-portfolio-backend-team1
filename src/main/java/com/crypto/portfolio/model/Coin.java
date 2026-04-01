package com.crypto.portfolio.model;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "coins")
public class Coin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private String name;

    private String chain;

    @Column(name = "contract_address")
    private String contractAddress;

    @Column(name = "coingecko_id")
    private String coingeckoId;
}
