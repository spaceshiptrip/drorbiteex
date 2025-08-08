#!/bin/bash

# Exit immediately on error
set -e

# Step 1: Create ~/.nisar-orbit if it doesn't exist
NISAR_DIR="$HOME/.nisar-orbit"
echo "Creating $NISAR_DIR..."
mkdir -p "$NISAR_DIR"

# Step 2: Copy orekit-data/ into ~/.nisar-orbit/
echo "Copying orekit-data/ into $NISAR_DIR..."
cp -r "orekit-data" "$NISAR_DIR/"

# Step 3: Copy contents of config/ into ~/.nisar-orbit/
echo "Copying config/* into $NISAR_DIR..."
cp -r config/* "$NISAR_DIR/"

echo "Setup complete."


