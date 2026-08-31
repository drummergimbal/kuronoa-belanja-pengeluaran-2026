#!/usr/bin/env bash
# Compile & run unit test module :core secara offline (tanpa Gradle/Android SDK),
# memakai kotlinc + JUnit yang sudah tersedia lokal. Dipakai utk verifikasi cepat
# logika inti (SyncMerger, CurrencyFormatter, dst) sebelum push ke GitHub Actions.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE_MAIN="$ROOT_DIR/core/src/main/kotlin"
CORE_TEST="$ROOT_DIR/core/src/test/kotlin"
OUT_DIR="$ROOT_DIR/.build-core-test"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

GRADLE_LIB="${GRADLE_LIB:-/opt/gradle-8.14.3/lib}"
if [ ! -d "$GRADLE_LIB" ]; then
  echo "GRADLE_LIB tidak ditemukan di $GRADLE_LIB — set env GRADLE_LIB ke lokasi lib Gradle yg berisi kotlin-compiler-embeddable." >&2
  exit 1
fi

KOTLINC_JARS=$(find "$GRADLE_LIB" -maxdepth 1 -iname "*.jar" | tr '\n' ':')
STDLIB=$(find "$GRADLE_LIB" -maxdepth 1 -iname "kotlin-stdlib-*.jar" ! -iname "*sources*" | head -1)
JUNIT=$(find "$GRADLE_LIB" -maxdepth 1 -iname "junit-4*.jar" | head -1)
HAMCREST=$(find / -xdev -iname "hamcrest-core*.jar" 2>/dev/null | head -1)

if [ -z "$STDLIB" ] || [ -z "$JUNIT" ] || [ -z "$HAMCREST" ]; then
  echo "Tidak menemukan kotlin-stdlib / junit / hamcrest jar. STDLIB=$STDLIB JUNIT=$JUNIT HAMCREST=$HAMCREST" >&2
  exit 1
fi

SOURCES=$(find "$CORE_MAIN" "$CORE_TEST" -name "*.kt")

echo ">> Compiling :core (main + test) ..."
COMPILE_LOG="$OUT_DIR/compile.log"
set +e
java -cp "$KOTLINC_JARS" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  $SOURCES \
  -d "$OUT_DIR/classes" \
  -cp "$STDLIB:$JUNIT:$HAMCREST" \
  -no-reflect > "$COMPILE_LOG" 2>&1
COMPILE_RC=$?
set -e
grep -v "JAVA_TOOL_OPTIONS" "$COMPILE_LOG" | grep -v "^warning: unable to find kotlin-" || true
if [ $COMPILE_RC -ne 0 ]; then
  echo "!! Kompilasi :core GAGAL (lihat log di atas)." >&2
  exit $COMPILE_RC
fi

echo ">> Discovering test classes ..."
TEST_CLASSES=$(cd "$OUT_DIR/classes" && find . -name "*Test.class" ! -name "*\$*" | sed 's|^\./||; s|/|.|g; s|\.class$||')

echo ">> Running JUnit tests ..."
set +e
java -cp "$OUT_DIR/classes:$STDLIB:$JUNIT:$HAMCREST" org.junit.runner.JUnitCore $TEST_CLASSES 2>&1 | grep -v "JAVA_TOOL_OPTIONS"
TEST_RC=${PIPESTATUS[0]}
set -e
exit $TEST_RC
