#!/usr/bin/env sh
set -eu

build_dir="${TMPDIR:-/tmp}/learning-game-dashboard-classes"
mkdir -p "$build_dir"
find src/main/java src/test/java -name '*.java' -print | xargs javac -d "$build_dir"
java -cp "$build_dir" learningops.game.GameOperationsServiceTest
