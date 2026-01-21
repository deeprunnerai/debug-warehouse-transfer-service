#!/bin/bash

CONCURRENCY=${1:-20}
TOTAL_REQUESTS=${2:-100}
BASE_URL=${3:-"http://localhost:8080"}

echo "=== Inventory Transfer Burst Test ==="
echo "Concurrency: $CONCURRENCY"
echo "Total Requests: $TOTAL_REQUESTS"
echo ""

# Check initial state
echo "Initial inventory state:"
curl -s "$BASE_URL/api/inventory/SKU-001" | jq .
echo ""

echo "Initial pool state:"
curl -s "$BASE_URL/api/health" | jq '.connectionPool'
echo ""

# Record initial total
INITIAL_TOTAL=$(curl -s "$BASE_URL/api/inventory/SKU-001" | jq '.totalQuantity')
echo "Initial total quantity for SKU-001: $INITIAL_TOTAL"
echo ""

echo "Running burst test..."
echo ""

# Function to send transfer request
send_transfer() {
    local FROM=$1
    local TO=$2
    local QTY=$3
    curl -s -X POST "$BASE_URL/api/transfer" \
        -H "Content-Type: application/json" \
        -d "{\"sku\":\"SKU-001\",\"fromLocation\":\"$FROM\",\"toLocation\":\"$TO\",\"quantity\":$QTY}" > /dev/null 2>&1
}

export -f send_transfer
export BASE_URL

# Stage 1: Valid transfers (should work)
echo "Stage 1: Executing valid transfers..."
for i in $(seq 1 $((TOTAL_REQUESTS / 4))); do
    send_transfer "WAREHOUSE-A" "WAREHOUSE-B" 1 &
    if [ $((i % CONCURRENCY)) -eq 0 ]; then
        wait
    fi
done
wait
echo "Done"

# Stage 2: Invalid transfers (quantity=0 - will trigger connection leak)
echo "Stage 2: Executing invalid transfers (quantity=0)..."
for i in $(seq 1 $((TOTAL_REQUESTS / 4))); do
    send_transfer "WAREHOUSE-A" "WAREHOUSE-B" 0 &
    if [ $((i % CONCURRENCY)) -eq 0 ]; then
        wait
    fi
done
wait
echo "Done"

# Stage 3: Concurrent transfers on same SKU (race condition)
echo "Stage 3: Executing concurrent transfers (race condition test)..."
for i in $(seq 1 $((TOTAL_REQUESTS / 2))); do
    send_transfer "WAREHOUSE-A" "WAREHOUSE-C" 5 &
    if [ $((i % CONCURRENCY)) -eq 0 ]; then
        wait
    fi
done
wait
echo "Done"

sleep 2

# Check final state
echo ""
echo "=== Results ==="
echo ""
echo "Final inventory state:"
curl -s "$BASE_URL/api/inventory/SKU-001" | jq .
echo ""

echo "Final pool state:"
curl -s "$BASE_URL/api/health" | jq '.connectionPool'
echo ""

FINAL_TOTAL=$(curl -s "$BASE_URL/api/inventory/SKU-001" | jq '.totalQuantity')
echo "Final total quantity for SKU-001: $FINAL_TOTAL"
echo "Expected total: $INITIAL_TOTAL"

if [ "$FINAL_TOTAL" != "$INITIAL_TOTAL" ]; then
    echo ""
    echo "WARNING: Inventory mismatch detected!"
    echo "Difference: $((FINAL_TOTAL - INITIAL_TOTAL))"
fi

# Check for negative quantities
NEGATIVE=$(curl -s "$BASE_URL/api/inventory" | jq '[.[] | select(.quantity < 0)] | length')
if [ "$NEGATIVE" -gt 0 ]; then
    echo ""
    echo "ERROR: Negative inventory quantities detected!"
    curl -s "$BASE_URL/api/inventory" | jq '[.[] | select(.quantity < 0)]'
fi
