package com.crypto.portfoliotracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist")
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String assetSymbol;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public static WatchlistBuilder builder() { return new WatchlistBuilder(); }

    public static class WatchlistBuilder {
        private User user;
        private String assetSymbol;
        private String note;
        
        public WatchlistBuilder user(User user) { this.user = user; return this; }
        public WatchlistBuilder assetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; return this; }
        public WatchlistBuilder note(String note) { this.note = note; return this; }
        
        public Watchlist build() {
            Watchlist watchlist = new Watchlist();
            watchlist.user = this.user;
            watchlist.assetSymbol = this.assetSymbol;
            watchlist.note = this.note;
            watchlist.createdAt = LocalDateTime.now();
            return watchlist;
        }
    }
}
