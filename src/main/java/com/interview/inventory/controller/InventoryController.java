package com.interview.inventory.controller;

import com.interview.inventory.dto.InventoryResponse;
import com.interview.inventory.dto.TransferRequest;
import com.interview.inventory.dto.TransferResponse;
import com.interview.inventory.model.Inventory;
import com.interview.inventory.repository.InventoryRepository;
import com.interview.inventory.repository.TransferLogRepository;
import com.interview.inventory.service.TransferService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final TransferLogRepository transferLogRepository;
    private final TransferService transferService;
    private final DataSource dataSource;

    @GetMapping("/inventory/{sku}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String sku) {
        List<Inventory> inventories = inventoryRepository.findBySku(sku);

        if (inventories.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Integer total = inventoryRepository.getTotalQuantityBySku(sku);

        List<InventoryResponse.LocationStock> locations = inventories.stream()
                .map(inv -> InventoryResponse.LocationStock.builder()
                        .location(inv.getLocation())
                        .quantity(inv.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(InventoryResponse.builder()
                .sku(sku)
                .totalQuantity(total)
                .locations(locations)
                .build());
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.executeTransfer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");

        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            Map<String, Object> pool = new HashMap<>();
            pool.put("activeConnections", poolMXBean.getActiveConnections());
            pool.put("idleConnections", poolMXBean.getIdleConnections());
            pool.put("totalConnections", poolMXBean.getTotalConnections());
            pool.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
            health.put("connectionPool", pool);
        }

        health.put("totalInventory", inventoryRepository.getTotalInventoryCount());
        health.put("completedTransfers", transferLogRepository.countCompletedTransfers());
        health.put("failedTransfers", transferLogRepository.countFailedTransfers());

        return ResponseEntity.ok(health);
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryRepository.findAll());
    }
}
