#!/bin/bash

BASE_URL=${1:-"http://localhost:8080"}

echo "=== Inventory Consistency Check ==="
echo ""

ERRORS=0

check_sku() {
    local SKU=$1
    local EXPECTED=$2
    ACTUAL=$(curl -s "$BASE_URL/api/inventory/$SKU" | jq '.totalQuantity')

    if [ "$ACTUAL" == "$EXPECTED" ]; then
        echo "[OK] $SKU: $ACTUAL (expected $EXPECTED)"
    else
        echo "[FAIL] $SKU: $ACTUAL (expected $EXPECTED) - Difference: $((ACTUAL - EXPECTED))"
        ERRORS=$((ERRORS + 1))
    fi
}

check_sku "SKU-001" 1800
check_sku "SKU-002" 3500
check_sku "SKU-003" 700

echo ""

# Check for negative quantities
echo "Checking for negative quantities..."
INVENTORY=$(curl -s "$BASE_URL/api/inventory")
NEGATIVE_COUNT=$(echo "$INVENTORY" | jq '[.[] | select(.quantity < 0)] | length')

if [ "$NEGATIVE_COUNT" -gt 0 ]; then
    echo "[FAIL] Found $NEGATIVE_COUNT items with negative quantity:"
    echo "$INVENTORY" | jq '[.[] | select(.quantity < 0)]'
    ERRORS=$((ERRORS + 1))
else
    echo "[OK] No negative quantities found"
fi

echo ""

# Check connection pool
echo "Connection pool status:"
curl -s "$BASE_URL/api/health" | jq '.connectionPool'

echo ""

if [ $ERRORS -eq 0 ]; then
    echo "=== All checks passed ==="
    exit 0
else
    echo "=== $ERRORS check(s) failed ==="
    exit 1
fi
