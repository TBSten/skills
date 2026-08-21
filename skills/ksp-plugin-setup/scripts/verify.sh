#!/usr/bin/env bash
#
# verify.sh — scaffold 後のビルド確認 (SKILL.md の 4 コマンド) を順に実行する。
# 各コマンドのログは <project-dir>/.local/tmp/<time>-<label>.log に保存する。
#
# Usage:
#   verify.sh [--project-dir <dir>] [--name <project-name>] [--fresh] [--allow-system-gradle]
#
#   --project-dir  対象プロジェクト (default: カレントディレクトリ)
#   --name         プロジェクト名。省略時は settings.gradle.kts の rootProject.name から推定
#   --fresh        scaffold 直後用: golden 記録 (snapshot.update) を先に実行してから通常 test を回す
#   --allow-system-gradle  gradlew が無い場合にシステムの gradle で代替する
#
# 実行コマンド (default 順):
#   1. ./gradlew :<name>-ksp:test                                    # kctfork + Konsist
#   2. ./gradlew :<name>-ksp:test -D<name>.snapshot.update=true      # golden 記録
#   3. ./gradlew jvmTest                                             # test モジュールの振る舞い検証
#   4. ./gradlew ktlintCheck
#
# 出力: 各コマンドの SUCCESS/FAILED サマリ + 1 行 JSON {"ok":...,"passed":N,"failed":N}

set -euo pipefail

usage() {
    sed -n '/^# verify\.sh/,/^# 出力/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

die() {
    {
        echo "ERROR: $1"
        [ $# -ge 2 ] && echo "  why: $2"
        [ $# -ge 3 ] && echo "  fix: $3"
    } >&2
    exit 1
}

PROJECT_DIR="."
NAME=""
FRESH=false
ALLOW_SYSTEM_GRADLE=false

while [ $# -gt 0 ]; do
    case "$1" in
        --project-dir) [ $# -ge 2 ] || die "--project-dir に値がない" "" "--project-dir <dir> の形で渡す"; PROJECT_DIR=$2; shift 2 ;;
        --name)        [ $# -ge 2 ] || die "--name に値がない" "" "--name <project-name> の形で渡す"; NAME=$2; shift 2 ;;
        --fresh)       FRESH=true; shift ;;
        --allow-system-gradle) ALLOW_SYSTEM_GRADLE=true; shift ;;
        -h|--help)     usage; exit 0 ;;
        *) die "不明なオプション: $1" "" "verify.sh --help で使い方を確認する" ;;
    esac
done

[ -d "$PROJECT_DIR" ] || die "プロジェクトディレクトリが無い: $PROJECT_DIR" \
    "scaffold 済みのプロジェクトを対象にする" "--project-dir に scaffold.sh の --dest を渡す"
PROJECT_DIR=$(cd "$PROJECT_DIR" && pwd)

[ -f "$PROJECT_DIR/settings.gradle.kts" ] || die "settings.gradle.kts が無い: $PROJECT_DIR" \
    "scaffold されたプロジェクトに見えない" "scaffold.sh を先に実行する"

if [ -z "$NAME" ]; then
    NAME=$(perl -ne 'print $1 if /^rootProject\.name = "([^"]+)"/' "$PROJECT_DIR/settings.gradle.kts")
    [ -n "$NAME" ] || die "プロジェクト名を推定できない" \
        "settings.gradle.kts に rootProject.name が見つからない" "--name <project-name> を渡す"
fi

if [ -x "$PROJECT_DIR/gradlew" ]; then
    GRADLE=("$PROJECT_DIR/gradlew" -p "$PROJECT_DIR")
elif $ALLOW_SYSTEM_GRADLE && command -v gradle >/dev/null 2>&1; then
    GRADLE=(gradle -p "$PROJECT_DIR")
else
    die "gradlew が無い: $PROJECT_DIR/gradlew" \
        "scaffold は wrapper を含まない (バージョンはプロジェクト側で決める)" \
        "プロジェクトで 'gradle wrapper' を実行して wrapper を生成する (または --allow-system-gradle)"
fi

LOG_DIR="$PROJECT_DIR/.local/tmp"
mkdir -p "$LOG_DIR"

LABELS=()
STATUSES=()
LOGS=()

run_task() {
    local label=$1
    shift
    local log="$LOG_DIR/$(date +%Y%m%d-%H%M%S)-$label.log"
    echo "==> $label: ${GRADLE[*]} $*"
    local status=SUCCESS
    if ! "${GRADLE[@]}" "$@" > "$log" 2>&1; then
        status=FAILED
        echo "---- $label failed; last 20 lines of $log ----"
        tail -20 "$log"
        echo "----"
    fi
    LABELS+=("$label")
    STATUSES+=("$status")
    LOGS+=("$log")
}

if $FRESH; then
    # 初回は golden が存在しないため、記録してから通常 test を回す
    run_task "golden-record" ":${NAME}-ksp:test" "-D${NAME}.snapshot.update=true"
    run_task "ksp-test" ":${NAME}-ksp:test"
else
    run_task "ksp-test" ":${NAME}-ksp:test"
    run_task "golden-record" ":${NAME}-ksp:test" "-D${NAME}.snapshot.update=true"
fi
run_task "jvm-test" "jvmTest"
run_task "ktlint" "ktlintCheck"

PASSED=0
FAILED=0
echo ""
echo "## ビルド確認サマリ ($NAME)"
for i in $(seq 0 $((${#LABELS[@]} - 1))); do
    echo "  ${STATUSES[$i]}  ${LABELS[$i]}  (log: ${LOGS[$i]})"
    if [ "${STATUSES[$i]}" = SUCCESS ]; then PASSED=$((PASSED + 1)); else FAILED=$((FAILED + 1)); fi
done
if [ "$FAILED" -eq 0 ]; then OK=true; else OK=false; fi
echo "{\"ok\":$OK,\"passed\":$PASSED,\"failed\":$FAILED,\"logsDir\":\"$LOG_DIR\"}"
$OK
