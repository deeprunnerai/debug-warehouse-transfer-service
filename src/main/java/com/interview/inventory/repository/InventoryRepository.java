package com.interview.inventory.repository;

import com.interview.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findBySkuAndLocation(String sku, String location);

    List<Inventory> findBySku(String sku);

    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.sku = :sku")
    Integer getTotalQuantityBySku(@Param("sku") String sku);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i")
    Long getTotalInventoryCount();
}
