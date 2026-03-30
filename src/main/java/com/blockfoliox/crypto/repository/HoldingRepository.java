package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.Holding;
import com.blockfoliox.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUser(User user);
    Optional<Holding> findByUserAndAssetSymbol(User user, String assetSymbol);
}