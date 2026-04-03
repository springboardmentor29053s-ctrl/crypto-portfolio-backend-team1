package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.TaxTransaction;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxTransactionRepository extends JpaRepository<TaxTransaction, Long> {
    
    List<TaxTransaction> findByUserOrderByTransactionDateDesc(User user);
    
    List<TaxTransaction> findByUserAndTaxYearOrderByTransactionDateDesc(User user, Integer taxYear);
    
    Optional<TaxTransaction> findByUserAndId(User user, Long id);
    
    @Query("SELECT t FROM TaxTransaction t WHERE t.user = :user AND t.taxYear = :taxYear AND t.gainLoss IS NOT NULL")
    List<TaxTransaction> findTaxableTransactionsByYear(@Param("user") User user, @Param("taxYear") Integer taxYear);
    
    @Query("SELECT t FROM TaxTransaction t WHERE t.user = :user AND t.taxYear = :taxYear AND t.gainLossType = :gainLossType")
    List<TaxTransaction> findByUserAndTaxYearAndGainLossType(@Param("user") User user, 
                                                             @Param("taxYear") Integer taxYear, 
                                                             @Param("gainLossType") String gainLossType);
    
    @Query("SELECT SUM(t.gainLoss) FROM TaxTransaction t WHERE t.user = :user AND t.taxYear = :taxYear AND t.gainLoss > 0")
    BigDecimal getTotalGainsByYear(@Param("user") User user, @Param("taxYear") Integer taxYear);
    
    @Query("SELECT SUM(t.gainLoss) FROM TaxTransaction t WHERE t.user = :user AND t.taxYear = :taxYear AND t.gainLoss < 0")
    BigDecimal getTotalLossesByYear(@Param("user") User user, @Param("taxYear") Integer taxYear);
    
    @Query("SELECT SUM(t.estimatedTax) FROM TaxTransaction t WHERE t.user = :user AND t.taxYear = :taxYear")
    BigDecimal getTotalEstimatedTaxByYear(@Param("user") User user, @Param("taxYear") Integer taxYear);
    
    @Query("SELECT COUNT(t) FROM TaxTransaction t WHERE t.user = :user AND t.taxYear = :taxYear")
    Long countTransactionsByYear(@Param("user") User user, @Param("taxYear") Integer taxYear);
    
    @Query("SELECT t FROM TaxTransaction t WHERE t.user = :user AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    List<TaxTransaction> findByUserAndDateRange(@Param("user") User user, 
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
}
