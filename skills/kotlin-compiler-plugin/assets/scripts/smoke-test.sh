#!/usr/bin/env bash
# smoke-test.sh — publishToMavenLocal した compiler plugin を、独立した consumer プロジェクトで
#                 指定 Kotlin 版でビルドし、生成 bytecode に注入シンボルがある (または無い) ことを
#                 `javap -p -c` で検証する。kctfork の unit test では見えない「実 Gradle + 実 KGP +
#                 公開 artifact (shadow jar / plugin marker / POM)」経路の回帰を捕まえる。
#
# Usage:
#   scripts/smoke-test.sh --kotlin <version> --consumer <dir> --class <class-file> \
#                         --expect <sym>[,<sym>...] [--absent] [--tasks "<gradle tasks>"] [--skip-publish]
#
# Options:
#   --kotlin <v>       consumer に -Pintegration.kotlin=<v> で渡す Kotlin 版 (required)
#   --consumer <dir>   独立 Gradle プロジェクト (gradlew と mavenLocal() 解決を持つ)。repo root 相対 (required)
#   --class <path>     検証する .class (consumer dir 相対。例: build/classes/kotlin/jvm/main/example/Foo.class) (required)
#   --expect <syms>    javap 出力に含まれるべきシンボル (カンマ区切り) (required)
#   --absent           逆に「含まれない」ことを検証 (例: DSL で enabled=false にした時の zero-overhead 確認)
#   --tasks "<t>"      consumer で実行するタスク (default: "build")
#   --skip-publish     publishToMavenLocal を省略 (並列実行時に呼び出し側で 1 回だけ publish した場合)
#
# consumer 側の前提: settings.gradle.kts の pluginManagement で
#   `settings.providers.gradleProperty("integration.kotlin")` を Kotlin plugin 版に使い、
#   repositories に mavenLocal() を含めること。
# ログ: .local/tmp/smoke-<timestamp>-kotlin-<v>.*.log。成功時は最終行に `OK ...` を出す。
set -euo pipefail

die() { printf 'ERROR: %s\n' "$1" >&2; [ "${2:-}" ] && printf '  fix: %s\n' "$2" >&2; exit 1; }

KOTLIN="" CONSUMER="" CLASS_FILE="" EXPECT="" ABSENT=false TASKS="build" SKIP_PUBLISH=false
while [ $# -gt 0 ]; do
    case "$1" in
        --kotlin) KOTLIN="${2:-}"; shift 2 ;;
        --consumer) CONSUMER="${2:-}"; shift 2 ;;
        --class) CLASS_FILE="${2:-}"; shift 2 ;;
        --expect) EXPECT="${2:-}"; shift 2 ;;
        --absent) ABSENT=true; shift ;;
        --tasks) TASKS="${2:-}"; shift 2 ;;
        --skip-publish) SKIP_PUBLISH=true; shift ;;
        -h|--help) sed -n '2,27p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) die "unknown option: $1" "scripts/smoke-test.sh --help で使い方を確認する" ;;
    esac
done

[ "$KOTLIN" ] || die "--kotlin is required" "例: --kotlin 2.3.20"
[ "$CONSUMER" ] || die "--consumer is required" "例: --consumer integration-test/smoke"
[ "$CLASS_FILE" ] || die "--class is required" "例: --class build/classes/kotlin/jvm/main/example/Foo.class"
[ "$EXPECT" ] || die "--expect is required" "例: --expect injectedCall,generatedField"
command -v javap >/dev/null 2>&1 || die "javap not found" "JDK の bin を PATH に通す"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
[ -x "$CONSUMER/gradlew" ] || die "consumer has no executable gradlew: $CONSUMER" \
    "consumer を独立 Gradle プロジェクトにし、gradle wrapper を置く"

log_dir=".local/tmp"
mkdir -p "$log_dir"
log_prefix="$log_dir/smoke-$(date +%Y%m%d-%H%M%S)-kotlin-$KOTLIN"

if [ "$SKIP_PUBLISH" != true ]; then
    echo "--- publishToMavenLocal (log: $log_prefix.publish.log)"
    ./gradlew publishToMavenLocal --no-configuration-cache > "$log_prefix.publish.log" 2>&1 \
        || { tail -40 "$log_prefix.publish.log" >&2; die "publishToMavenLocal failed" "$log_prefix.publish.log を確認する"; }
fi

echo "--- $CONSUMER: $TASKS with Kotlin $KOTLIN (log: $log_prefix.consumer.log)"
# shellcheck disable=SC2086
(cd "$CONSUMER" && ./gradlew $TASKS "-Pintegration.kotlin=$KOTLIN" --refresh-dependencies --rerun-tasks) \
    > "$log_prefix.consumer.log" 2>&1 \
    || { tail -40 "$log_prefix.consumer.log" >&2; die "consumer build failed" "$log_prefix.consumer.log を確認する"; }

class_path="$CONSUMER/$CLASS_FILE"
[ -f "$class_path" ] || die "class file not found: $class_path" "--class のパス (consumer 相対) と --tasks を確認する"
javap -p -c "$class_path" > "$log_prefix.javap.txt" 2>&1 || die "javap failed" "$log_prefix.javap.txt を確認する"

bad=""
IFS=',' read -r -a symbols <<< "$EXPECT"
for sym in "${symbols[@]}"; do
    if grep -q -- "$sym" "$log_prefix.javap.txt"; then
        [ "$ABSENT" = true ] && bad="$bad $sym"
    else
        [ "$ABSENT" = true ] || bad="$bad $sym"
    fi
done
if [ "$bad" ]; then
    if [ "$ABSENT" = true ]; then
        die "symbols unexpectedly present:$bad" "disabled 時に注入が残っている。dump: $log_prefix.javap.txt"
    fi
    die "symbols missing:$bad" "compiler plugin が attach / 変換されていない。dump: $log_prefix.javap.txt"
fi
if [ "$ABSENT" = true ]; then
    echo "OK kotlin=$KOTLIN absent=$EXPECT"
else
    echo "OK kotlin=$KOTLIN present=$EXPECT"
fi
