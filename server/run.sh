#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Dedicated Server Startup Script
# Platform: Linux / NeoForge 1.21.1 (Java 21)
# ==============================================================================

# Ensure script executes in its own directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Memory allocation (Adjust according to available host RAM)
INIT_RAM="4G"
MAX_RAM="8G"

# Java binary path (Ensure Java 21 LTS is used)
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA="java"
else
    echo "ERROR: Java 21 not found. Please install Java 21 or set JAVA_HOME."
    exit 1
fi

# Verify Java version
JAVA_VER=$("$JAVA" -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
echo "Using Java binary: $JAVA (version $JAVA_VER)"

# NeoForge Unix argument file path
UNIX_ARGS="libraries/net/neoforged/neoforge/21.1.234/unix_args.txt"

if [ ! -f "$UNIX_ARGS" ]; then
    echo "WARNING: NeoForge unix_args.txt not found at $UNIX_ARGS."
    echo "Please ensure you have executed: java -jar neoforge-21.1.234-installer.jar --installServer"
fi

# Java 21 High-Performance G1GC Tuning Flags (Aikar's Flags for Java 21)
JVM_OPTS=(
    "-Xms${INIT_RAM}"
    "-Xmx${MAX_RAM}"
    "-XX:+UseG1GC"
    "-XX:+ParallelRefProcEnabled"
    "-XX:MaxGCPauseMillis=200"
    "-XX:+UnlockExperimentalVMOptions"
    "-XX:+DisableExplicitGC"
    "-XX:+AlwaysPreTouch"
    "-XX:G1NewSizePercent=30"
    "-XX:G1MaxNewSizePercent=40"
    "-XX:G1ReservePercent=20"
    "-XX:G1HeapWastePercent=5"
    "-XX:G1MixedGCCountTarget=4"
    "-XX:InitiatingHeapOccupancyPercent=15"
    "-XX:G1MixedGCLiveThresholdPercent=90"
    "-XX:G1RSetUpdatingPauseTimePercent=5"
    "-XX:SurvivorRatio=32"
    "-XX:+PerfDisableSharedMem"
    "-XX:MaxTenuringThreshold=1"
    "-Dusing.aikars.flags=https://mcflags.emc.gs"
    "-Daikars.new.flags=true"
)

echo "========================================================"
echo " Starting SMP Mod Suite Dedicated Server                "
echo " Memory: $INIT_RAM min / $MAX_RAM max                   "
echo " Press Ctrl+C within 5 seconds to abort auto-restart    "
echo "========================================================"

# Graceful shutdown trap
trap "echo 'Stopping server loop...'; exit 0" SIGINT SIGTERM

while true; do
    if [ -f "$UNIX_ARGS" ]; then
        "$JAVA" "${JVM_OPTS[@]}" @"$UNIX_ARGS" "$@"
    else
        echo "Attempting fallback launch with nogui..."
        "$JAVA" "${JVM_OPTS[@]}" -jar neoforge.jar nogui "$@"
    fi

    echo ""
    echo "Server process terminated. Restarting in 5 seconds (Press Ctrl+C to abort)..."
    sleep 5
done
