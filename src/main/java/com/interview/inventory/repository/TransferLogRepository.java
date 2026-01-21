package com.interview.inventory.repository;

import com.interview.inventory.model.TransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferLogRepository extends JpaRepository<TransferLog, Long> {

    List<TransferLog> findBySkuOrderByCreatedAtDesc(String sku);

    List<TransferLog> findByStatus(TransferLog.TransferStatus status);

    @Query("SELECT COUNT(t) FROM TransferLog t WHERE t.status = 'COMPLETED'")
    Long countCompletedTransfers();

    @Query("SELECT COUNT(t) FROM TransferLog t WHERE t.status = 'FAILED'")
    Long countFailedTransfers();
}
