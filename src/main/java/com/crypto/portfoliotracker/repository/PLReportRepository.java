package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.PLReport;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PLReportRepository extends JpaRepository<PLReport, Long> {
    
    List<PLReport> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<PLReport> findByUserAndId(User user, Long id);
    
    List<PLReport> findByUserAndReportTypeOrderByCreatedAtDesc(User user, String reportType);
    
    @Query("SELECT p FROM PLReport p WHERE p.user = :user AND p.startDate >= :startDate AND p.endDate <= :endDate")
    List<PLReport> findByUserAndDateRange(@Param("user") User user, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM PLReport p WHERE p.user = :user AND p.reportType = :reportType AND p.createdAt >= :since")
    List<PLReport> findByUserAndReportTypeSince(@Param("user") User user, 
                                                @Param("reportType") String reportType, 
                                                @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(p) FROM PLReport p WHERE p.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT p FROM PLReport p WHERE p.user = :user ORDER BY p.createdAt DESC LIMIT 1")
    Optional<PLReport> findLatestByUser(@Param("user") User user);
}
