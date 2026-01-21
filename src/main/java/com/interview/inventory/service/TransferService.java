package com.interview.inventory.service;

import com.interview.inventory.dto.TransferRequest;
import com.interview.inventory.dto.TransferResponse;
import com.interview.inventory.model.Inventory;
import com.interview.inventory.model.TransferLog;
import com.interview.inventory.repository.InventoryRepository;
import com.interview.inventory.repository.TransferLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final InventoryRepository inventoryRepository;
    private final TransferLogRepository transferLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        log.debug("Processing transfer: {} units of {} from {} to {}",
                request.getQuantity(), request.getSku(),
                request.getFromLocation(), request.getToLocation());

        TransferLog transferLog = TransferLog.builder()
                .sku(request.getSku())
                .fromLocation(request.getFromLocation())
                .toLocation(request.getToLocation())
                .quantity(request.getQuantity())
                .status(TransferLog.TransferStatus.PENDING)
                .build();
        transferLogRepository.save(transferLog);

        try {
            validateAndExecute(request, transferLog);

            transferLog.setStatus(TransferLog.TransferStatus.COMPLETED);
            transferLogRepository.save(transferLog);

            return TransferResponse.builder()
                    .transferId(transferLog.getId())
                    .sku(request.getSku())
                    .fromLocation(request.getFromLocation())
                    .toLocation(request.getToLocation())
                    .quantity(request.getQuantity())
                    .status("COMPLETED")
                    .message("Transfer completed successfully")
                    .build();

        } catch (Exception e) {
            transferLog.setStatus(TransferLog.TransferStatus.FAILED);
            transferLog.setErrorMessage(e.getMessage());
            transferLogRepository.save(transferLog);

            return TransferResponse.builder()
                    .transferId(transferLog.getId())
                    .sku(request.getSku())
                    .fromLocation(request.getFromLocation())
                    .toLocation(request.getToLocation())
                    .quantity(request.getQuantity())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }

    private void validateAndExecute(TransferRequest request, TransferLog transferLog) {
        EntityManager em = entityManager.getEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();

        try {
            Inventory sourceInventory = inventoryRepository
                    .findBySkuAndLocation(request.getSku(), request.getFromLocation())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No inventory found for SKU " + request.getSku() +
                            " at location " + request.getFromLocation()));

            if (request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Transfer quantity must be positive");
            }

            if (sourceInventory.getQuantity() < request.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available: " + sourceInventory.getQuantity() +
                        ", Requested: " + request.getQuantity());
            }

            sourceInventory.setQuantity(sourceInventory.getQuantity() - request.getQuantity());
            inventoryRepository.save(sourceInventory);

            Inventory destInventory = inventoryRepository
                    .findBySkuAndLocation(request.getSku(), request.getToLocation())
                    .orElseGet(() -> Inventory.builder()
                            .sku(request.getSku())
                            .location(request.getToLocation())
                            .quantity(0)
                            .build());

            destInventory.setQuantity(destInventory.getQuantity() + request.getQuantity());
            inventoryRepository.save(destInventory);

            em.getTransaction().commit();
            em.close();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }
}
