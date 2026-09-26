#!/usr/bin/env bash
#
# verify.sh — scaffold.sh で生成したプラグインのビルド確認を決まった順に実行する。
# 各タスクのログは <project-dir>/.local/tmp/<time>-<label>.log に保存する。
# 読解・書き換え・再実装せず、そのまま実行する。
#
# Usage:
#   bash verify.sh [--project-dir <dir>] [--fresh] [--with-verify-plugin]
#                  [--only <steps>] [--skip <steps>] [--gradle-args "<args>"]
#
#   --project-dir         対象プロジェクト (default: カレントディレクトリ)。scaffold.sh の --dest を渡す
#   --fresh               scaffold 直後用: updatePreview で golden (snapshots/preview) を初回生成してから verifyPreview を回す。
#                         golden を上書きするので、既存 golden の差分を人間が承認した後の焼き直しにも使う
#   --with-verify-plugin  verifyPlugin (Plugin Verifier) も実行する。推奨 IDE をダウンロードするので既定では実行しない
#   --only <steps>        カンマ区切りで実行する step を限定する (例: --only build,test)
#   --skip <steps>        カンマ区切りで step を除外する
#   --gradle-args         全 Gradle 実行に追加する引数 (空白区切り。例: "--project-cache-dir /tmp/pc --warning-mode=all")
#
# steps (この順に実行):
#   wrapper          gradlew の存在確認 (無ければ直し方を出して止まる)
#   build            ./gradlew buildPlugin      (初回は SDK のダウンロード込みで ~4〜5 分)
#   test             ./gradlew test
#   update-preview   ./gradlew updatePreview    (--fresh の時のみ)
#   verify-preview   ./gradlew verifyPreview    (golden が無ければ実行せず FAILED。初回は --fresh)
#   verify-plugin    ./gradlew verifyPlugin     (--with-verify-plugin の時のみ)
#
# 出力: 各 step の SUCCESS/FAILED/SKIPPED サマリ + 1 行 JSON {"ok":...,"passed":N,"failed":N,"skipped":N,"logsDir":"..."}

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

need_value() {
    [ $# -ge 2 ] || die "オプション $1 に値がない" "$1 は値を取るオプション" "例: $1 <value> の形で渡す"
}

PROJECT_DIR="."
FRESH=false
WITH_VERIFY_PLUGIN=false
ONLY=""
SKIP=""
GRADLE_EXTRA=""

while [ $# -gt 0 ]; do
    case "$1" in
        --project-dir)        need_value "$@"; PROJECT_DIR=$2; shift 2 ;;
        --fresh)              FRESH=true; shift ;;
        --with-verify-plugin) WITH_VERIFY_PLUGIN=true; shift ;;
        --only)               need_value "$@"; ONLY=$2; shift 2 ;;
        --skip)               need_value "$@"; SKIP=$2; shift 2 ;;
        --gradle-args)        need_value "$@"; GRADLE_EXTRA=$2; shift 2 ;;
        -h|--help)            usage; exit 0 ;;
        *) die "不明なオプション: $1" "このオプションは定義されていない" "verify.sh --help で使い方を確認する" ;;
    esac
done

ALL_STEPS="wrapper,build,test,update-preview,verify-preview,verify-plugin"
for s in ${ONLY//,/ } ${SKIP//,/ }; do
    case ",$ALL_STEPS," in
        *",$s,"*) ;;
        *) die "不明な step: $s" "step 名は決まった値だけを受け付ける" "$ALL_STEPS から選ぶ" ;;
    esac
done

[ -d "$PROJECT_DIR" ] || die "プロジェクトディレクトリが無い: $PROJECT_DIR" \
    "scaffold 済みのプロジェクトを対象にする" "--project-dir に scaffold.sh の --dest を渡す"
PROJECT_DIR=$(cd "$PROJECT_DIR" && pwd)
[ -f "$PROJECT_DIR/settings.gradle.kts" ] && [ -f "$PROJECT_DIR/src/main/resources/META-INF/plugin.xml" ] \
    || die "IntelliJ Platform プラグインのプロジェクトに見えない: $PROJECT_DIR" \
        "settings.gradle.kts と src/main/resources/META-INF/plugin.xml を前提にタスクを回す" \
        "scaffold.sh を先に実行し、その --dest を --project-dir に渡す"

step_enabled() {
    if [ -n "$ONLY" ]; then
        case ",$ONLY," in *",$1,"*) ;; *) return 1 ;; esac
    fi
    case ",$SKIP," in *",$1,"*) return 1 ;; esac
    return 0
}

LOG_DIR="$PROJECT_DIR/.local/tmp"
mkdir -p "$LOG_DIR"

LABELS=()
STATUSES=()
LOGS=()

record() {
    LABELS+=("$1")
    STATUSES+=("$2")
    LOGS+=("$3")
}

# ---------------------------------------------------------------- wrapper
# 以降の Gradle step は wrapper 前提なので、--skip wrapper でも存在確認はする
[ -x "$PROJECT_DIR/gradlew" ] && [ -f "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" ] \
    || die "Gradle wrapper が無い: $PROJECT_DIR/gradlew (+ gradle/wrapper/gradle-wrapper.jar)" \
        "scaffold.sh は wrapper を同梱しない (Gradle の版は利用先で決める)" \
        "既存 repo の gradlew と gradle/wrapper/ を $PROJECT_DIR にコピーする (確認済みの版は references/setup/basics.md)。または Gradle がある環境で 'gradle wrapper --gradle-version <版>' を実行する"
step_enabled wrapper && record wrapper SUCCESS "(existing gradlew)"

# ---------------------------------------------------------------- Gradle 実行
run_task() {
    # run_task <label> <gradle args...>
    local label=$1
    shift
    local log
    log="$LOG_DIR/$(date +%Y%m%d-%H%M%S)-$label.log"
    local cmd=("$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" --console=plain)
    # shellcheck disable=SC2206 # --gradle-args は空白区切りで展開する
    [ -n "$GRADLE_EXTRA" ] && cmd+=($GRADLE_EXTRA)
    cmd+=("$@")
    echo "==> $label: ${cmd[*]}"
    local status=SUCCESS
    if ! "${cmd[@]}" > "$log" 2>&1; then
        status=FAILED
        echo "---- $label failed; last 30 lines of $log ----"
        tail -30 "$log"
        echo "----"
    fi
    record "$label" "$status" "$log"
}

skip_step() {
    record "$1" SKIPPED "($2)"
}

has_golden() {
    ls "$PROJECT_DIR"/snapshots/preview/*.png >/dev/null 2>&1
}

step_enabled build && run_task build buildPlugin
step_enabled test && run_task test test
if step_enabled update-preview; then
    if $FRESH; then
        run_task update-preview updatePreview
    else
        skip_step update-preview "--fresh の時のみ (golden を上書きするため)"
    fi
fi
if step_enabled verify-preview; then
    if has_golden; then
        run_task verify-preview verifyPreview
    else
        echo "---- verify-preview: golden が無い ($PROJECT_DIR/snapshots/preview/*.png) ----"
        echo "  why: verifyPreview は golden との比較なので、golden が無いと検証にならない"
        echo "  fix: 初回は --fresh を付けて再実行し、生成された snapshots/preview を commit する"
        record verify-preview FAILED "(golden が無い。--fresh で初回生成する)"
    fi
fi
if step_enabled verify-plugin; then
    if $WITH_VERIFY_PLUGIN; then
        run_task verify-plugin verifyPlugin
    else
        skip_step verify-plugin "--with-verify-plugin の時のみ (推奨 IDE をダウンロードするため)"
    fi
fi

# ---------------------------------------------------------------- サマリ
PASSED=0 FAILED=0 SKIPPED=0
echo ""
echo "## ビルド確認サマリ ($PROJECT_DIR)"
for i in "${!LABELS[@]}"; do
    echo "  ${STATUSES[$i]}  ${LABELS[$i]}  (log: ${LOGS[$i]})"
    case "${STATUSES[$i]}" in
        SUCCESS) PASSED=$((PASSED + 1)) ;;
        FAILED)  FAILED=$((FAILED + 1)) ;;
        *)       SKIPPED=$((SKIPPED + 1)) ;;
    esac
done
if [ "$FAILED" -eq 0 ]; then OK=true; else OK=false; fi
if $FRESH && [ "$FAILED" -eq 0 ] && step_enabled update-preview; then
    echo "NOTE: golden を生成した — snapshots/preview の PNG を目視してから commit する"
fi
echo "{\"ok\":$OK,\"passed\":$PASSED,\"failed\":$FAILED,\"skipped\":$SKIPPED,\"logsDir\":\"$LOG_DIR\"}"
$OK
