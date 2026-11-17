@echo off
REM ==============================================================================
REM SMP Mod Suite - Dedicated Server Startup Script
REM Platform: Windows / NeoForge 1.21.1 (Java 21)
REM ==============================================================================

set INIT_RAM=4G
set MAX_RAM=8G
set WIN_ARGS=libraries\net\neoforged\neoforge\21.1.234\win_args.txt

echo ========================================================
echo  Starting SMP Mod Suite Dedicated Server
echo  Memory: %INIT_RAM% min / %MAX_RAM% max
echo ========================================================

java -Xms%INIT_RAM% -Xmx%MAX_RAM% ^
  -XX:+UseG1GC ^
  -XX:+ParallelRefProcEnabled ^
  -XX:MaxGCPauseMillis=200 ^
  -XX:+UnlockExperimentalVMOptions ^
  -XX:+DisableExplicitGC ^
  -XX:+AlwaysPreTouch ^
  -XX:G1NewSizePercent=30 ^
  -XX:G1MaxNewSizePercent=40 ^
  -XX:G1ReservePercent=20 ^
  -XX:G1HeapWastePercent=5 ^
  -XX:G1MixedGCCountTarget=4 ^
  -XX:InitiatingHeapOccupancyPercent=15 ^
  -XX:G1MixedGCLiveThresholdPercent=90 ^
  -XX:G1RSetUpdatingPauseTimePercent=5 ^
  -XX:SurvivorRatio=32 ^
  -XX:+PerfDisableSharedMem ^
  -XX:MaxTenuringThreshold=1 ^
  -Dusing.aikars.flags=https://mcflags.emc.gs ^
  -Daikars.new.flags=true ^
  @%WIN_ARGS% %*

pause
