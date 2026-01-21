#!/bin/bash

MODE=${1:-"all"}

echo "=== Inventory Service Logs ==="
echo "Mode: $MODE"
echo ""

case $MODE in
    "errors")
        echo "Showing errors only..."
        docker-compose logs app 2>&1 | grep -i "error\|exception\|failed" | tail -50
        ;;
    "pool")
        echo "Showing connection pool activity..."
        docker-compose logs app 2>&1 | grep -i "hikari\|pool\|connection" | tail -50
        ;;
    "transfers")
        echo "Showing transfer activity..."
        docker-compose logs app 2>&1 | grep -i "transfer\|Processing\|COMPLETED\|FAILED" | tail -50
        ;;
    "follow")
        echo "Following live logs (Ctrl+C to stop)..."
        docker-compose logs -f app
        ;;
    *)
        echo "Showing last 100 lines..."
        docker-compose logs app 2>&1 | tail -100
        ;;
esac

echo ""
echo "---"
echo "Usage: ./scripts/view_logs.sh [mode]"
echo "Modes: all (default), errors, pool, transfers, follow"
