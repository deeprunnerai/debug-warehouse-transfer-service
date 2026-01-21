-- Seed data for inventory service
-- Total quantity per SKU should remain constant after transfers

INSERT INTO inventory (sku, location, quantity, updated_at, version) VALUES
    ('SKU-001', 'WAREHOUSE-A', 1000, NOW(), 0),
    ('SKU-001', 'WAREHOUSE-B', 500, NOW(), 0),
    ('SKU-001', 'WAREHOUSE-C', 300, NOW(), 0),
    ('SKU-002', 'WAREHOUSE-A', 2000, NOW(), 0),
    ('SKU-002', 'WAREHOUSE-B', 1500, NOW(), 0),
    ('SKU-003', 'WAREHOUSE-A', 500, NOW(), 0),
    ('SKU-003', 'WAREHOUSE-C', 200, NOW(), 0)
ON CONFLICT DO NOTHING;
