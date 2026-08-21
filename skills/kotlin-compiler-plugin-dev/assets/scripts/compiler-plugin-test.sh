#!/usr/bin/env bash
# compiler-plugin-test.sh — Run the compiler-plugin tests against one Kotlin version,
# or against every version listed in the SSOT.
#
# Usage:
#   ./scripts/compiler-plugin-test.sh <kotlin-version>   # e.g. 2.3.21, 2.4.0-Beta2
#   ./scripts/compiler-plugin-test.sh --all              # loop over scripts/supported-kotlin-versions.txt
#
# --all は SSOT (scripts/supported-kotlin-versions.txt) の全バージョンを順に実行し、
# 失敗したバージョンの一覧を末尾に出力する (1 つでも失敗すれば非 0 終了)。
#
# Gradle 側の前提: compiler-plugin/build.gradle.kts が -Ptest.kotlin を読んで
# test classpath の kotlin-compiler-embeddable を resolutionStrategy.force で
# 差し替えること (kotlin-compiler-plugin-dev skill の references/ci-matrix.md 参照)。
#
# 最終行に結果 JSON を stdout に出力する。
set -euo pipefail

err() { printf '%s\n' "$*" >&2; }

usage() {
    err "Usage: $0 <kotlin-version> | --all"
    err "  <kotlin-version>  e.g. 2.3.21, 2.4.0-Beta2"
    err "  --all             test every version in scripts/supported-kotlin-versions.txt"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

SSOT="$SCRIPT_DIR/supported-kotlin-versions.txt"
LOG_DIR=".local/tmp"

# ---------- preflight ----------
if [ $# -ne 1 ] || [ -z "$1" ]; then
    usage
    exit 1
fi
if [ ! -x ./gradlew ]; then
    err "ERROR: ./gradlew not found (or not executable) in $ROOT_DIR"
    err "  why: this script assumes it lives in <project-root>/scripts/ of a Gradle project"
    err "  fix: copy it to <project-root>/scripts/compiler-plugin-test.sh and run chmod +x"
    exit 1
fi
mkdir -p "$LOG_DIR"

run_one() {
    # 1 バージョン分のテスト。
    # --rerun-tasks 必須: Gradle build cache は -Ptest.kotlin を input として認識しない
    # ため、これが無いと 2 バージョン目以降が UP-TO-DATE で skip される。
    version="$1"
    log="$LOG_DIR/compiler-plugin-test-${version}-$(date +%s).log"
    echo "[compiler-plugin-test] === Kotlin $version ==="
    echo "[compiler-plugin-test] log: $log"
    ./gradlew \
        :compiler-plugin:test \
        --rerun-tasks \
        -Ptest.kotlin="$version" \
        --continue 2>&1 | tee "$log"
}

to_json_array() {
    out=""
    for v in "$@"; do out="${out}\"${v}\","; done
    printf '[%s]' "${out%,}"
}

if [ "$1" != "--all" ]; then
    # ---------- single version ----------
    VERSION="$1"
    if run_one "$VERSION"; then
        echo "[compiler-plugin-test] $VERSION OK"
        printf '{"status":"ok","mode":"single","version":"%s"}\n' "$VERSION"
    else
        err "ERROR: compiler-plugin tests failed for Kotlin $VERSION"
        err "  why: see the test log under $LOG_DIR/ for the failing tests"
        err "  fix: NoSuchMethodError などの API 断絶なら compat module / reflection shim / capability flag を検討 (kotlin-compiler-plugin-dev skill の references/troubleshooting.md)"
        printf '{"status":"failed","mode":"single","version":"%s"}\n' "$VERSION"
        exit 1
    fi
    exit 0
fi

# ---------- --all: SSOT の全バージョンをループ ----------
if [ ! -f "$SSOT" ]; then
    err "ERROR: SSOT not found: $SSOT"
    err "  why: --all は scripts/supported-kotlin-versions.txt を読む"
    err "  fix: kotlin-compiler-plugin-dev skill の assets/scripts/supported-kotlin-versions.txt をコピーし、実際のサポートバージョンに合わせて編集する"
    exit 1
fi

VERSIONS=()
while IFS= read -r line; do
    VERSIONS+=("$line")
done < <(grep -vE '^[[:space:]]*(#|$)' "$SSOT")

if [ "${#VERSIONS[@]}" -eq 0 ]; then
    err "ERROR: no versions listed in $SSOT"
    err "  why: 空行とコメント行を除くと 1 行も残らなかった"
    err "  fix: サポートする Kotlin バージョンを 1 行 1 バージョンで追記する"
    exit 1
fi

PASSED=()
FAILED=()
for v in "${VERSIONS[@]}"; do
    if run_one "$v"; then
        echo "[compiler-plugin-test] $v OK"
        PASSED+=("$v")
    else
        echo "[compiler-plugin-test] $v FAILED"
        FAILED+=("$v")
    fi
done

echo ""
echo "[compiler-plugin-test] === Summary (${#VERSIONS[@]} versions) ==="
echo "[compiler-plugin-test] passed: ${#PASSED[@]}, failed: ${#FAILED[@]}"

if [ "${#FAILED[@]}" -gt 0 ]; then
    echo "[compiler-plugin-test] FAILED versions:"
    for v in "${FAILED[@]}"; do
        echo "  - $v"
    done
    err "ERROR: compiler-plugin tests failed for ${#FAILED[@]} version(s): ${FAILED[*]}"
    err "  why: see the per-version logs under $LOG_DIR/"
    err "  fix: 失敗バージョンを ./scripts/compiler-plugin-test.sh <version> で個別再現し、API 断絶なら compat module / reflection shim / capability flag を検討"
    printf '{"status":"failed","mode":"all","passed":%s,"failed":%s}\n' \
        "$(to_json_array ${PASSED[@]+"${PASSED[@]}"})" \
        "$(to_json_array ${FAILED[@]+"${FAILED[@]}"})"
    exit 1
fi

printf '{"status":"ok","mode":"all","passed":%s,"failed":[]}\n' \
    "$(to_json_array ${PASSED[@]+"${PASSED[@]}"})"
