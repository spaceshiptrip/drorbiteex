#!/bin/bash

# build.sh - Quick Maven build & run script for DrOrbiteex

set -e  # Exit immediately if a command exits with non-zero status

case "$1" in
    --dev)
        echo "[DEV] Running incremental build and launch (skipping tests)..."
        mvn -q -DskipTests javafx:run
        ;;
    --fast)
        echo "[FAST] Compiling only changed files and launching (skip tests)..."
        mvn -q -DskipTests compile javafx:run
        ;;
    --release)
        echo "[RELEASE] Performing full clean build with release profile..."
        mvn clean install -Prelease -X -e
        ;;
    *)
        echo "Usage: $0 [--fast | --dev | --release]"
        echo "  --fast     Compile only changed files, skip tests, run JavaFX"
        echo "  --dev      Run development build (no clean, skip tests, run JavaFX)"
        echo "  --release  Full clean install with release profile"
        exit 1
        ;;
esac

