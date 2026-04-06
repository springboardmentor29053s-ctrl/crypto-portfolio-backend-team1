package com.crypto.portfolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; 

    private String symbol; 

    private Double targetValue;

    private Boolean triggered = false;
    private LocalDateTime triggeredAt;

    private LocalDateTime createdAt;
    @Column(name = "seen")
    private Boolean seen = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}