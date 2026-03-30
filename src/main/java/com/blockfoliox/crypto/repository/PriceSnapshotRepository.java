package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
    List<PriceSnapshot> findTop24ByAssetSymbolOrderByCapturedAtDesc(String assetSymbol);
    List<PriceSnapshot> findByAssetSymbolOrderByCapturedAtAsc(String assetSymbol);
}