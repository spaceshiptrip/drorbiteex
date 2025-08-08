#!/bin/bash

# build.sh - Quick Maven build & run script for DrOrbiteex

set -e  # exit immediately if a command exits with non-zero status

case "$1" in
    --dev)
        echo "[DEV] Running incremental build and launch (skipping tests)..."
        mvn -q -DskipTests javafx:run
        ;;
    --release)
        echo "[RELEASE] Performing full clean build with release profile..."
        mvn clean install -Prelease -X -e
        ;;
    *)
        echo "Usage: $0 [--dev | --release]"
        echo "  --dev      Run development build (no clean, skip tests, run JavaFX)"
        echo "  --release  Full clean install with release profile"
        exit 1
        ;;
esac


