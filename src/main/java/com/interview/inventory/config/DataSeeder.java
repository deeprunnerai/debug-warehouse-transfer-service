package com.interview.inventory.config;

import com.interview.inventory.model.Inventory;
import com.interview.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) {
        if (inventoryRepository.count() > 0) {
            log.info("Database already seeded, skipping...");
            return;
        }

        log.info("Seeding initial inventory data...");

        List<Inventory> seedData = List.of(
            Inventory.builder().sku("SKU-001").location("WAREHOUSE-A").quantity(1000).build(),
            Inventory.builder().sku("SKU-001").location("WAREHOUSE-B").quantity(500).build(),
            Inventory.builder().sku("SKU-001").location("WAREHOUSE-C").quantity(300).build(),
            Inventory.builder().sku("SKU-002").location("WAREHOUSE-A").quantity(2000).build(),
            Inventory.builder().sku("SKU-002").location("WAREHOUSE-B").quantity(1500).build(),
            Inventory.builder().sku("SKU-003").location("WAREHOUSE-A").quantity(500).build(),
            Inventory.builder().sku("SKU-003").location("WAREHOUSE-C").quantity(200).build()
        );

        inventoryRepository.saveAll(seedData);
        log.info("Seeded {} inventory records", seedData.size());
    }
}
