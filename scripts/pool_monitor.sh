#!/bin/bash

BASE_URL=${1:-"http://localhost:8080"}
INTERVAL=${2:-2}

echo "=== Connection Pool Monitor ==="
echo "Monitoring $BASE_URL every ${INTERVAL}s"
echo "Press Ctrl+C to stop"
echo ""
echo "Time                 | Active | Idle | Total | Waiting"
echo "---------------------|--------|------|-------|--------"

while true; do
    RESPONSE=$(curl -s "$BASE_URL/api/health" 2>/dev/null)

    if [ $? -eq 0 ] && [ -n "$RESPONSE" ]; then
        ACTIVE=$(echo "$RESPONSE" | jq -r '.connectionPool.activeConnections // "N/A"')
        IDLE=$(echo "$RESPONSE" | jq -r '.connectionPool.idleConnections // "N/A"')
        TOTAL=$(echo "$RESPONSE" | jq -r '.connectionPool.totalConnections // "N/A"')
        WAITING=$(echo "$RESPONSE" | jq -r '.connectionPool.threadsAwaitingConnection // "N/A"')

        printf "%-20s | %-6s | %-4s | %-5s | %-7s\n" \
            "$(date '+%Y-%m-%d %H:%M:%S')" \
            "$ACTIVE" "$IDLE" "$TOTAL" "$WAITING"

        # Alert if pool looks unhealthy
        if [ "$ACTIVE" != "N/A" ] && [ "$ACTIVE" -ge 5 ]; then
            echo "  ^ WARNING: Pool at capacity!"
        fi
        if [ "$WAITING" != "N/A" ] && [ "$WAITING" -gt 0 ]; then
            echo "  ^ WARNING: Threads waiting for connections!"
        fi
    else
        printf "%-20s | %-6s | %-4s | %-5s | %-7s\n" \
            "$(date '+%Y-%m-%d %H:%M:%S')" \
            "ERR" "ERR" "ERR" "ERR"
    fi

    sleep $INTERVAL
done
