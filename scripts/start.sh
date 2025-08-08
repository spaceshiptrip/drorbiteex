#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="$SCRIPT_DIR/jvm"
export PATH="$JAVA_HOME/bin:$PATH"

"$JAVA_HOME/bin/java" --module-path="bin" -m eu.dariolucia.drorbiteex/eu.dariolucia.drorbiteex.application.DrOrbiteex

