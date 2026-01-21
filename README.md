# Inventory Transfer Service - Debug Challenge

## Background

You've just joined a warehouse management team. They've built a service to handle stock transfers between warehouse locations. The service has been running in development for weeks without issues, but now it's failing intermittently during load testing before the production release.

The team is stuck and needs your help to diagnose and fix the issues.

## Problem Statement

The inventory transfer service handles moving stock of products (identified by SKU) between warehouse locations. The business requirement is simple:

**The total quantity of each SKU across all locations must remain constant after any number of transfers.**

For example, if SKU-001 starts with:
- Warehouse A: 1000 units
- Warehouse B: 500 units
- Warehouse C: 300 units
- **Total: 1800 units**

After any number of valid transfers, the total should still be **1800 units**.

### What's Happening

- Unit tests pass consistently
- Manual testing works fine
- **Load testing before production release is failing intermittently**

The team hasn't been able to pinpoint the exact cause.

## Your Task

1. **Reproduce the issues** using the provided test scripts
2. **Identify the root cause(s)** - there may be more than one bug
3. **Explain why** the issues only manifest under load
4. **Propose fixes** with actual code changes

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Connection Pool | HikariCP |
| Build | Maven |
| Container | Docker Compose |

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for local development)
- `curl` and `jq` (for test scripts)

### Running the Service

```bash
# Start the entire stack
docker-compose up --build

# Or run locally (requires PostgreSQL)
./mvnw spring-boot:run
```

### Seeding Test Data

The database is automatically seeded with initial inventory on first startup.

| SKU | Location | Quantity |
|-----|----------|----------|
| SKU-001 | WAREHOUSE-A | 1000 |
| SKU-001 | WAREHOUSE-B | 500 |
| SKU-001 | WAREHOUSE-C | 300 |
| SKU-002 | WAREHOUSE-A | 2000 |
| SKU-002 | WAREHOUSE-B | 1500 |
| SKU-003 | WAREHOUSE-A | 500 |
| SKU-003 | WAREHOUSE-C | 200 |

## API Endpoints

### Get Inventory by SKU
```bash
GET /api/inventory/{sku}

# Example
curl http://localhost:8080/api/inventory/SKU-001
```

### Transfer Stock
```bash
POST /api/transfer
Content-Type: application/json

{
  "sku": "SKU-001",
  "fromLocation": "WAREHOUSE-A",
  "toLocation": "WAREHOUSE-B",
  "quantity": 100
}
```

### Health Check (includes pool stats)
```bash
GET /api/health

# Shows connection pool statistics
curl http://localhost:8080/api/health
```

## Test Scripts

Make the scripts executable first:
```bash
chmod +x scripts/*.sh
```

### 1. Consistency Check
Verifies inventory totals match expected values:
```bash
./scripts/consistency_check.sh
```

### 2. Burst Test
Simulates concurrent transfer requests:
```bash
# Default: 20 concurrent, 100 total requests
./scripts/burst_test.sh

# Custom: 50 concurrent, 200 total requests
./scripts/burst_test.sh 50 200
```

### 3. Pool Monitor
Watch connection pool in real-time:
```bash
./scripts/pool_monitor.sh
```

## Verifying the Problem Exists

### Symptom 1: Request Timeouts / 500 Errors
After repeated failed transfers, the service starts returning errors:
```bash
# Send several transfers that will fail (insufficient stock)
for i in {1..10}; do
  curl -s -X POST "http://localhost:8080/api/transfer" \
    -H "Content-Type: application/json" \
    -d '{"sku":"SKU-001","fromLocation":"WAREHOUSE-A","toLocation":"WAREHOUSE-B","quantity":5000}'
done

# Check pool health - look for high activeConnections
curl -s http://localhost:8080/api/health | jq '.connectionPool'
```

**What to look for:** `activeConnections` climbing toward `totalConnections` (max 5), and `threadsAwaitingConnection` > 0

### Symptom 2: Inventory Mismatch
After concurrent transfers, totals don't add up:
```bash
# Run burst test
./scripts/burst_test.sh 30 100

# Check consistency
./scripts/consistency_check.sh
```

**What to look for:** Total quantity differs from expected (1800 for SKU-001), or negative quantities appear.

### Viewing Logs
```bash
# All logs
./scripts/view_logs.sh

# Errors only
./scripts/view_logs.sh errors

# Connection pool activity
./scripts/view_logs.sh pool

# Follow live
./scripts/view_logs.sh follow
```

## Suggested Investigation Approach

1. Run the consistency check to establish a baseline
2. Start the pool monitor in a separate terminal
3. Run the burst test and observe what happens
4. Check the application logs: `./scripts/view_logs.sh errors`
5. Look at the code, starting with the transfer service

## Deliverables

When you've identified the issues, please provide:

1. **Root cause analysis** - What are the bugs and where are they?
2. **Reproduction steps** - How can you reliably trigger each issue?
3. **Explanation** - Why do these bugs only appear under load?
4. **Code fixes** - Actual code changes to resolve the issues
5. **Bonus** - What monitoring or safeguards would you add to prevent this in production?

## Time Budget

45-60 minutes

Good luck!
