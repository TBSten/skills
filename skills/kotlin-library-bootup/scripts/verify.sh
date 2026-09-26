#!/usr/bin/env bash
#
# verify.sh — scaffold 後のビルド確認を決まった順に実行する。
# 各タスクのログは <project-dir>/.local/tmp/<time>-<label>.log に保存する。
#
# Usage:
#   verify.sh [--project-dir <dir>] [--fresh] [--bootstrap-wrapper]
#             [--only <steps>] [--skip <steps>] [--test-tasks "<tasks>"]
#             [--project-cache-dir <dir>] [--gradle-args "<args>"]
#
#   --project-dir        対象プロジェクト (default: カレントディレクトリ)
#   --fresh              scaffold 直後用: ktlintFormat (名前置換で崩れた import 順を直す) と apiDump を先に実行する
#                        (api/ が 1 つも無い場合は --fresh 無しでも apiDump を先に実行する)
#   --bootstrap-wrapper  gradlew が無ければ生成する。gradle-wrapper.properties (同梱) の版を読み、
#                        PATH の gradle か ~/.gradle/wrapper/dists にある同版の Gradle で
#                        一時ディレクトリに wrapper を作り、gradlew / gradlew.bat / gradle-wrapper.jar だけをコピーする
#   --only <steps>       カンマ区切りで実行する step を限定する (例: --only wrapper, --only build,test)
#   --skip <steps>       カンマ区切りで step を除外する
#   --test-tasks         test step の Gradle タスク (default: KMP は allTests / JVM は test)
#   --project-cache-dir  Gradle の --project-cache-dir。複数 agent が同じツリーを並列ビルドする時に分ける
#                        (integrationTest には <dir>-integrationTest を渡す)
#   --gradle-args        全 Gradle 実行に追加する引数 (例: "-PdisableAppleTargets=true")
#
# steps (この順に実行):
#   wrapper           gradlew の存在確認 (--bootstrap-wrapper 時は生成)
#   ktlint-format     ./gradlew ktlintFormat        (--fresh の時のみ)
#   api-dump          ./gradlew apiDump            (--fresh または api/ が無い時のみ)
#   build             ./gradlew build
#   test              ./gradlew allTests | test     (--test-tasks で変更可)
#   api-check         ./gradlew apiCheck
#   ktlint            ./gradlew ktlintCheck
#   publish-local     ./gradlew publishToMavenLocal (署名はスキップされる)
#   api-docs          ./gradlew generateApiDocs
#   integration-test  ./gradlew -p integrationTest test (integrationTest/ がある時のみ)
#
# KMP プロジェクトで ANDROID_HOME / ANDROID_SDK_ROOT / local.properties の sdk.dir が無い時は、
# 標準の Android Studio SDK の場所 (~/Library/Android/sdk, ~/Android/Sdk) を ANDROID_HOME として渡す (ファイルは書かない)。
# どこにも無ければ Gradle を回す前にエラーで止まる (Android ターゲットの設定・ktlint が失敗するため)。
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
BOOTSTRAP=false
ONLY=""
SKIP=""
TEST_TASKS=""
PROJECT_CACHE_DIR=""
GRADLE_EXTRA=""

while [ $# -gt 0 ]; do
    case "$1" in
        --project-dir)       need_value "$@"; PROJECT_DIR=$2; shift 2 ;;
        --fresh)             FRESH=true; shift ;;
        --bootstrap-wrapper) BOOTSTRAP=true; shift ;;
        --only)              need_value "$@"; ONLY=$2; shift 2 ;;
        --skip)              need_value "$@"; SKIP=$2; shift 2 ;;
        --test-tasks)        need_value "$@"; TEST_TASKS=$2; shift 2 ;;
        --project-cache-dir) need_value "$@"; PROJECT_CACHE_DIR=$2; shift 2 ;;
        --gradle-args)       need_value "$@"; GRADLE_EXTRA=$2; shift 2 ;;
        -h|--help)           usage; exit 0 ;;
        *) die "不明なオプション: $1" "このオプションは定義されていない" "verify.sh --help で使い方を確認する" ;;
    esac
done

ALL_STEPS="wrapper,ktlint-format,api-dump,build,test,api-check,ktlint,publish-local,api-docs,integration-test"
for s in ${ONLY//,/ } ${SKIP//,/ }; do
    case ",$ALL_STEPS," in
        *",$s,"*) ;;
        *) die "不明な step: $s" "step 名は決まった値だけを受け付ける" "$ALL_STEPS から選ぶ" ;;
    esac
done

[ -d "$PROJECT_DIR" ] || die "プロジェクトディレクトリが無い: $PROJECT_DIR" \
    "scaffold 済みのプロジェクトを対象にする" "--project-dir に scaffold.sh の --dest を渡す"
PROJECT_DIR=$(cd "$PROJECT_DIR" && pwd)
[ -f "$PROJECT_DIR/settings.gradle.kts" ] || die "settings.gradle.kts が無い: $PROJECT_DIR" \
    "scaffold されたプロジェクトに見えない" "scaffold.sh を先に実行する"

step_enabled() {
    if [ -n "$ONLY" ]; then
        case ",$ONLY," in *",$1,"*) ;; *) return 1 ;; esac
    fi
    case ",$SKIP," in *",$1,"*) return 1 ;; esac
    return 0
}

IS_KMP=false
if ls "$PROJECT_DIR"/build-logic/src/main/kotlin/*.kmp.gradle.kts >/dev/null 2>&1; then
    IS_KMP=true
fi
if [ -z "$TEST_TASKS" ]; then
    if $IS_KMP; then TEST_TASKS="allTests"; else TEST_TASKS="test"; fi
fi

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
bootstrap_wrapper() {
    local props="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties"
    [ -f "$props" ] || die "gradle-wrapper.properties が無い: $props" \
        "wrapper の Gradle 版をここから読む" "scaffold.sh を再実行するか、properties を用意する"
    local version
    version=$(perl -ne 'print $1 if m{distributionUrl=.*gradle-([0-9][^-/]*)-(?:bin|all)\.zip}' "$props")
    [ -n "$version" ] || die "gradle-wrapper.properties から Gradle 版を読めない" \
        "distributionUrl が gradle-<version>-bin.zip 形式でない" "distributionUrl を確認する"

    local gradle_bin="" candidate
    if candidate=$(command -v gradle 2>/dev/null) && [ -x "$candidate" ]; then
        gradle_bin=$candidate
    else
        for candidate in "${GRADLE_USER_HOME:-$HOME/.gradle}"/wrapper/dists/gradle-"$version"-{bin,all}/*/gradle-"$version"/bin/gradle; do
            if [ -x "$candidate" ]; then gradle_bin=$candidate; break; fi
        done
    fi
    [ -n "$gradle_bin" ] || die "wrapper を生成できる Gradle が見つからない" \
        "PATH に gradle が無く、~/.gradle/wrapper/dists にも gradle-$version が無い" \
        "Gradle をインストールする (brew install gradle / sdk install gradle) か、同じ Gradle 版の別プロジェクトから gradlew / gradlew.bat / gradle/wrapper/gradle-wrapper.jar をコピーする"

    local tmp log
    tmp=$(mktemp -d "${TMPDIR:-/tmp}/kotlin-library-bootup-wrapper.XXXXXX")
    log="$LOG_DIR/$(date +%Y%m%d-%H%M%S)-wrapper.log"
    : > "$tmp/settings.gradle.kts"
    echo "==> wrapper: $gradle_bin wrapper --gradle-version $version (in $tmp)"
    if ! (cd "$tmp" && "$gradle_bin" wrapper --gradle-version "$version" --distribution-type bin --no-daemon) > "$log" 2>&1; then
        tail -20 "$log"
        rm -rf "$tmp"
        die "wrapper の生成に失敗した (log: $log)" "Gradle $version の起動に失敗した (JDK の版を確認)" "log を確認し、JDK 17+ で再実行する"
    fi
    mkdir -p "$PROJECT_DIR/gradle/wrapper"
    cp "$tmp/gradlew" "$tmp/gradlew.bat" "$PROJECT_DIR/"
    cp "$tmp/gradle/wrapper/gradle-wrapper.jar" "$PROJECT_DIR/gradle/wrapper/"
    chmod +x "$PROJECT_DIR/gradlew"
    rm -rf "$tmp"
    record wrapper SUCCESS "$log"
}

if step_enabled wrapper; then
    if [ -x "$PROJECT_DIR/gradlew" ] && [ -f "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
        record wrapper SUCCESS "(existing gradlew)"
    elif $BOOTSTRAP; then
        bootstrap_wrapper
    fi
fi
[ -x "$PROJECT_DIR/gradlew" ] || die "gradlew が無い: $PROJECT_DIR/gradlew" \
    "scaffold は wrapper の jar / スクリプトを同梱しない (properties のみ)" \
    "verify.sh --bootstrap-wrapper を付けて再実行する (または Gradle がある環境で 'gradle wrapper --gradle-version <版>')"

# ---------------------------------------------------------------- Android SDK
if $IS_KMP && [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ] \
    && ! grep -qs '^sdk.dir=' "$PROJECT_DIR/local.properties"; then
    for sdk in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        if [ -d "$sdk" ]; then
            export ANDROID_HOME=$sdk
            echo "NOTE: ANDROID_HOME=$sdk を Gradle に渡す (環境変数 / local.properties が無いため)"
            break
        fi
    done
    [ -n "${ANDROID_HOME:-}" ] || die "Android SDK が見つからない" \
        "KMP (standard / full) は com.android.kotlin.multiplatform.library を使うため、SDK が無いと Android の設定・ビルド・ktlint が失敗する" \
        "ANDROID_HOME を設定するか、local.properties に sdk.dir=<SDK のパス> を書く (Android Studio の SDK Manager で導入できる)"
fi

# ---------------------------------------------------------------- Gradle 実行
run_task() {
    # run_task <label> <build-dir> <gradle args...>
    local label=$1 build_dir=$2
    shift 2
    local log cmd
    log="$LOG_DIR/$(date +%Y%m%d-%H%M%S)-$label.log"
    cmd=("$PROJECT_DIR/gradlew" -p "$build_dir" --console=plain)
    if [ -n "$PROJECT_CACHE_DIR" ]; then
        if [ "$build_dir" = "$PROJECT_DIR" ]; then
            cmd+=(--project-cache-dir "$PROJECT_CACHE_DIR")
        else
            cmd+=(--project-cache-dir "$PROJECT_CACHE_DIR-$(basename "$build_dir")")
        fi
    fi
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

if step_enabled ktlint-format; then
    if $FRESH; then
        run_task ktlint-format "$PROJECT_DIR" ktlintFormat
    else
        skip_step ktlint-format "--fresh の時のみ"
    fi
fi
if step_enabled api-dump; then
    if $FRESH || ! ls -d "$PROJECT_DIR"/*/api >/dev/null 2>&1; then
        run_task api-dump "$PROJECT_DIR" apiDump
    else
        skip_step api-dump "api/ が既にある。更新するなら --fresh"
    fi
fi
step_enabled build         && run_task build "$PROJECT_DIR" build
# shellcheck disable=SC2086 # TEST_TASKS は空白区切りのタスク列
step_enabled test          && run_task test "$PROJECT_DIR" $TEST_TASKS
step_enabled api-check     && run_task api-check "$PROJECT_DIR" apiCheck
step_enabled ktlint        && run_task ktlint "$PROJECT_DIR" ktlintCheck
step_enabled publish-local && run_task publish-local "$PROJECT_DIR" publishToMavenLocal
step_enabled api-docs      && run_task api-docs "$PROJECT_DIR" generateApiDocs
if step_enabled integration-test; then
    if [ -f "$PROJECT_DIR/integrationTest/settings.gradle.kts" ]; then
        run_task integration-test "$PROJECT_DIR/integrationTest" test
    else
        skip_step integration-test "integrationTest/ が無い"
    fi
fi

# ---------------------------------------------------------------- サマリ
PASSED=0 FAILED=0 SKIPPED=0
echo ""
echo "## ビルド確認サマリ ($PROJECT_DIR, kmp=$IS_KMP)"
for i in "${!LABELS[@]}"; do
    echo "  ${STATUSES[$i]}  ${LABELS[$i]}  (log: ${LOGS[$i]})"
    case "${STATUSES[$i]}" in
        SUCCESS) PASSED=$((PASSED + 1)) ;;
        FAILED)  FAILED=$((FAILED + 1)) ;;
        *)       SKIPPED=$((SKIPPED + 1)) ;;
    esac
done
if [ "$FAILED" -eq 0 ]; then OK=true; else OK=false; fi
echo "{\"ok\":$OK,\"passed\":$PASSED,\"failed\":$FAILED,\"skipped\":$SKIPPED,\"kmp\":$IS_KMP,\"logsDir\":\"$LOG_DIR\"}"
$OK
