#!/usr/bin/env bash
set -euo pipefail

# --- Paths ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_HOME="${JAVA_HOME:-$SCRIPT_DIR/jvm}"
APP_MAIN_MODULE="eu.dariolucia.drorbiteex/eu.dariolucia.drorbiteex.application.DrOrbiteex"
MODULE_PATH="${MODULE_PATH:-bin}"

# Config directory (default ~/.nisar-orbit). Override via --config-dir or $NISAR_CONFIG_DIR
CONFIG_DIR_DEFAULT="${HOME}/.nisar-orbit"
CONFIG_DIR="${NISAR_CONFIG_DIR:-$CONFIG_DIR_DEFAULT}"
if [[ "${1:-}" == "--config-dir" ]]; then
  shift
  CONFIG_DIR="${1:-$CONFIG_DIR_DEFAULT}"
  shift || true
fi

OREKIT_DIR_NAME="orekit-data"   # This matches DrOrbiteex.java
OREKIT_DIR="${CONFIG_DIR}/${OREKIT_DIR_NAME}"
ORBIT_FILE="${CONFIG_DIR}/orbits.xml"
GS_FILE="${CONFIG_DIR}/groundstations.xml"

# --- Helpers ---
need_config=0
[[ -d "$OREKIT_DIR" ]] || need_config=1
[[ -f "$ORBIT_FILE" ]] || need_config=1
[[ -f "$GS_FILE"   ]] || need_config=1

if (( need_config == 1 )); then
  echo "[setup] Missing required config in '$CONFIG_DIR'. Running config.sh..."
  if [[ ! -x "$SCRIPT_DIR/config.sh" ]]; then
    echo "[error] config.sh not found or not executable at: $SCRIPT_DIR/config.sh" >&2
    exit 1
  fi
  # Pass along the desired config dir in case your config.sh supports it later
  "$SCRIPT_DIR/config.sh" --config-dir "$CONFIG_DIR" || {
    echo "[error] config.sh failed. Aborting." >&2
    exit 1
  }
  echo "[setup] Configuration completed."
fi

# --- Java runtime check ---
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "[warn] JAVA_HOME does not point to a JRE/JDK with 'java' at $JAVA_HOME/bin/java"
  echo "       Falling back to system 'java' on PATH."
  JAVA_BIN="java"
else
  JAVA_BIN="$JAVA_HOME/bin/java"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# --- Launch ---
echo "[run] Using config dir: $CONFIG_DIR"
echo "[run] Orekit data:      $OREKIT_DIR"
echo "[run] Orbits file:      $ORBIT_FILE"
echo "[run] Groundstations:   $GS_FILE"

exec "$JAVA_BIN" \
  -Ddrorbiteex.config="$CONFIG_DIR" \
  --module-path="$MODULE_PATH" \
  -m "$APP_MAIN_MODULE"

