package com.interview.inventory;

import com.interview.inventory.dto.TransferRequest;
import com.interview.inventory.dto.TransferResponse;
import com.interview.inventory.model.Inventory;
import com.interview.inventory.repository.InventoryRepository;
import com.interview.inventory.repository.TransferLogRepository;
import com.interview.inventory.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TransferLogRepository transferLogRepository;

    @BeforeEach
    void setUp() {
        transferLogRepository.deleteAll();
        inventoryRepository.deleteAll();

        inventoryRepository.save(Inventory.builder()
                .sku("TEST-SKU")
                .location("LOC-A")
                .quantity(100)
                .build());

        inventoryRepository.save(Inventory.builder()
                .sku("TEST-SKU")
                .location("LOC-B")
                .quantity(50)
                .build());
    }

    @Test
    void shouldTransferInventorySuccessfully() {
        TransferRequest request = TransferRequest.builder()
                .sku("TEST-SKU")
                .fromLocation("LOC-A")
                .toLocation("LOC-B")
                .quantity(25)
                .build();

        TransferResponse response = transferService.executeTransfer(request);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals(25, response.getQuantity());

        Inventory sourceAfter = inventoryRepository.findBySkuAndLocation("TEST-SKU", "LOC-A").orElseThrow();
        Inventory destAfter = inventoryRepository.findBySkuAndLocation("TEST-SKU", "LOC-B").orElseThrow();

        assertEquals(75, sourceAfter.getQuantity());
        assertEquals(75, destAfter.getQuantity());
    }

    @Test
    void shouldFailWhenInsufficientStock() {
        TransferRequest request = TransferRequest.builder()
                .sku("TEST-SKU")
                .fromLocation("LOC-A")
                .toLocation("LOC-B")
                .quantity(200)
                .build();

        TransferResponse response = transferService.executeTransfer(request);

        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("Insufficient stock"));
    }

    @Test
    void shouldFailWhenSourceLocationNotFound() {
        TransferRequest request = TransferRequest.builder()
                .sku("TEST-SKU")
                .fromLocation("NON-EXISTENT")
                .toLocation("LOC-B")
                .quantity(10)
                .build();

        TransferResponse response = transferService.executeTransfer(request);

        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("No inventory found"));
    }

    @Test
    void shouldCreateDestinationIfNotExists() {
        TransferRequest request = TransferRequest.builder()
                .sku("TEST-SKU")
                .fromLocation("LOC-A")
                .toLocation("NEW-LOC")
                .quantity(10)
                .build();

        TransferResponse response = transferService.executeTransfer(request);

        assertEquals("COMPLETED", response.getStatus());

        Inventory newLoc = inventoryRepository.findBySkuAndLocation("TEST-SKU", "NEW-LOC").orElseThrow();
        assertEquals(10, newLoc.getQuantity());
    }

    @Test
    void shouldMaintainTotalQuantityAfterTransfer() {
        Integer totalBefore = inventoryRepository.getTotalQuantityBySku("TEST-SKU");

        TransferRequest request = TransferRequest.builder()
                .sku("TEST-SKU")
                .fromLocation("LOC-A")
                .toLocation("LOC-B")
                .quantity(30)
                .build();

        transferService.executeTransfer(request);

        Integer totalAfter = inventoryRepository.getTotalQuantityBySku("TEST-SKU");

        assertEquals(totalBefore, totalAfter, "Total quantity should remain constant after transfer");
    }
}
