package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
    
    List<PriceSnapshot> findByAssetSymbolOrderByCapturedAtDesc(String assetSymbol, 
        org.springframework.data.domain.Pageable pageable);
    
    Optional<PriceSnapshot> findTopByAssetSymbolOrderByCapturedAtDesc(String assetSymbol);
    
    @Query("SELECT ps FROM PriceSnapshot ps WHERE ps.capturedAt >= :since ORDER BY ps.capturedAt DESC")
    List<PriceSnapshot> findSnapshotsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT DISTINCT ps.assetSymbol FROM PriceSnapshot ps")
    List<String> findAllDistinctSymbols();
    
    @Query("SELECT ps FROM PriceSnapshot ps WHERE ps.assetSymbol = :symbol AND ps.capturedAt >= :since ORDER BY ps.capturedAt ASC")
    List<PriceSnapshot> findHistoricalData(@Param("symbol") String symbol, @Param("since") LocalDateTime since);
    
    @Query("SELECT ps FROM PriceSnapshot ps WHERE ps.capturedAt >= :threshold ORDER BY ps.capturedAt DESC")
    List<PriceSnapshot> findLatestPrices(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT ps FROM PriceSnapshot ps WHERE ps.assetSymbol IN :symbols AND ps.capturedAt >= :since ORDER BY ps.capturedAt DESC")
    List<PriceSnapshot> findSnapshotsForSymbols(@Param("symbols") List<String> symbols, @Param("since") LocalDateTime since);
    
    @Query("SELECT ps FROM PriceSnapshot ps WHERE ps.assetSymbol = :symbol AND ps.capturedAt > :after ORDER BY ps.capturedAt DESC")
    List<PriceSnapshot> findByAssetSymbolAndCapturedAtAfter(@Param("symbol") String symbol, @Param("after") LocalDateTime after);
    
    void deleteByCapturedAtBefore(LocalDateTime cutoff);
}
