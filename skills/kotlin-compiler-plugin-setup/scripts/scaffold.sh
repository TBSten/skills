#!/usr/bin/env bash
# scaffold.sh — Kotlin Compiler Plugin プロジェクトの scaffold を生成する。
#
# skill の example/ (実パッケージ com.example.compilerpluginsetup + `Example` クラス prefix で
# 書かれた完全なプロジェクト skeleton) をコピーし、プロジェクト固有の値に置換・rename する。
#
# Usage:
#   scaffold.sh --dest <dir> --name <kebab-name> --group-id <group-id> \
#               [--plugin-id <plugin-id>] [--package <package>] \
#               [--kotlin-version <version>] \
#               [--skip-gradle-plugin] [--skip-integration-test] [--skip-test] \
#               [--dry-run] [--force]
#
# Options:
#   --dest <dir>              (required) 生成先ディレクトリ。無ければ作成する
#   --name <kebab-name>       (required) rootProject.name (kebab-case)。クラス prefix
#                             (PascalCase) と gradle plugin 短縮名 (camelCase) もここから導出
#   --group-id <id>           (required) Maven groupId
#   --plugin-id <id>          compiler plugin ID (default: --group-id)
#   --package <pkg>           Kotlin パッケージ (default: --group-id から `-` を除去したもの)
#   --kotlin-version <v>      libs.versions.toml の kotlin バージョン (default: example のまま)
#   --skip-gradle-plugin      gradle-plugin モジュールを生成しない
#   --skip-integration-test   integration-test モジュールを生成しない
#   --skip-test               compiler-plugin/src/test を生成しない
#   --dry-run                 生成予定のファイル一覧のみ表示 (書き込みなし)
#   --force                   既存ファイルがあっても上書きする
#
# 最終行に結果 JSON を stdout に出力する。
set -euo pipefail

err() { printf 'ERROR: %s\n' "$*" >&2; }
die() {
    # die "<what>" "<why>" "<how to fix>"
    err "$1"
    if [ "${2:-}" ]; then err "  why: $2"; fi
    if [ "${3:-}" ]; then err "  fix: $3"; fi
    exit 1
}

usage() { sed -n '/^# scaffold\.sh/,/^set -euo/p' "$0" | grep '^#' | sed 's/^# \{0,1\}//'; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)/example"

OLD_PKG="com.example.compilerpluginsetup"
OLD_PKG_PATH="com/example/compilerpluginsetup"
OLD_PREFIX="Example"

DEST="" NAME="" GROUP_ID="" PLUGIN_ID="" PACKAGE="" KOTLIN_VERSION=""
SKIP_GRADLE_PLUGIN=false SKIP_INTEGRATION_TEST=false SKIP_TEST=false
DRY_RUN=false FORCE=false

while [ $# -gt 0 ]; do
    case "$1" in
        --dest) DEST="${2:-}"; shift 2 ;;
        --name) NAME="${2:-}"; shift 2 ;;
        --group-id) GROUP_ID="${2:-}"; shift 2 ;;
        --plugin-id) PLUGIN_ID="${2:-}"; shift 2 ;;
        --package) PACKAGE="${2:-}"; shift 2 ;;
        --kotlin-version) KOTLIN_VERSION="${2:-}"; shift 2 ;;
        --skip-gradle-plugin) SKIP_GRADLE_PLUGIN=true; shift ;;
        --skip-integration-test) SKIP_INTEGRATION_TEST=true; shift ;;
        --skip-test) SKIP_TEST=true; shift ;;
        --dry-run) DRY_RUN=true; shift ;;
        --force) FORCE=true; shift ;;
        -h|--help) usage; exit 0 ;;
        *) die "unknown option: $1" "サポートしていない引数" "scaffold.sh --help で使い方を確認する" ;;
    esac
done

# ---------- preflight ----------
command -v perl >/dev/null 2>&1 \
    || die "perl not found" "置換処理に perl を使用する (macOS/Linux 標準搭載)" "perl をインストールして再実行する"
[ -d "$EXAMPLE_DIR" ] \
    || die "example directory not found: $EXAMPLE_DIR" "skill のインストールが不完全" \
           "gh skill install tbsten/skills kotlin-compiler-plugin-setup で再インストール (prompt 利用時は sparse clone をやり直す)"
[ "$DEST" ] || die "--dest is required" "生成先が未指定" "--dest <dir> を指定する"
[ "$NAME" ] || die "--name is required" "プロジェクト名が未指定" "--name <kebab-name> を指定する"
[ "$GROUP_ID" ] || die "--group-id is required" "Maven groupId が未指定" "--group-id <id> を指定する"

printf '%s' "$NAME" | grep -Eq '^[a-z][a-z0-9]*(-[a-z0-9]+)*$' \
    || die "invalid --name: $NAME" "kebab-case (例: my-plugin) である必要がある" "--name を小文字英数字とハイフンのみで指定する"
printf '%s' "$GROUP_ID" | grep -Eq '^[a-zA-Z][a-zA-Z0-9_.-]*$' \
    || die "invalid --group-id: $GROUP_ID" "Maven groupId として不正" "例: com.example.myplugin"

[ "$PLUGIN_ID" ] || PLUGIN_ID="$GROUP_ID"
[ "$PACKAGE" ] || PACKAGE="$(printf '%s' "$GROUP_ID" | tr -d '-')"
printf '%s' "$PACKAGE" | grep -Eq '^[a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*)*$' \
    || die "invalid --package: $PACKAGE" "Kotlin パッケージ名として不正 (group-id からの自動導出に失敗した可能性)" \
           "--package <pkg> を明示的に指定する (例: com.example.myplugin)"
if [ "$KOTLIN_VERSION" ]; then
    printf '%s' "$KOTLIN_VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)*$' \
        || die "invalid --kotlin-version: $KOTLIN_VERSION" "バージョン形式が不正" "例: 2.3.10, 2.4.0-Beta2"
fi

# ---------- derive names ----------
PASCAL=""
IFS='-' read -r -a _parts <<< "$NAME"
for _p in "${_parts[@]}"; do
    _head="$(printf '%s' "${_p:0:1}" | tr '[:lower:]' '[:upper:]')"
    PASCAL="${PASCAL}${_head}${_p:1}"
done
CAMEL="$(printf '%s' "${PASCAL:0:1}" | tr '[:upper:]' '[:lower:]')${PASCAL:1}"
PKG_PATH="$(printf '%s' "$PACKAGE" | tr '.' '/')"

# ---------- enumerate source files ----------
SRC_FILES="$( (cd "$EXAMPLE_DIR" && find . -type f ! -name '.DS_Store' | sed 's|^\./||' | LC_ALL=C sort) )"

included() {
    case "$1" in
        gradle-plugin/*) [ "$SKIP_GRADLE_PLUGIN" = true ] && return 1 ;;
        integration-test/*) [ "$SKIP_INTEGRATION_TEST" = true ] && return 1 ;;
        compiler-plugin/src/test/*) [ "$SKIP_TEST" = true ] && return 1 ;;
    esac
    return 0
}

map_dest() {
    # 相対パスを生成先の相対パスへ変換 (パッケージディレクトリ remap + Example prefix rename)
    local rel="$1" dir base
    rel="$(printf '%s' "$rel" | sed "s|$OLD_PKG_PATH|$PKG_PATH|")"
    # libs.versions.toml はプロジェクトでは gradle/ 配下に置く
    [ "$rel" = "libs.versions.toml" ] && rel="gradle/libs.versions.toml"
    dir="$(dirname "$rel")"
    base="$(basename "$rel")"
    case "$base" in
        "$OLD_PREFIX"*) base="${PASCAL}${base#"$OLD_PREFIX"}" ;;
    esac
    if [ "$dir" = "." ]; then printf '%s' "$base"; else printf '%s/%s' "$dir" "$base"; fi
}

# ---------- collision check (冪等性: --force なしで上書きしない) ----------
COLLISIONS=""
FILE_COUNT=0
PLAN=""
while IFS= read -r rel; do
    [ "$rel" ] || continue
    included "$rel" || continue
    dest_rel="$(map_dest "$rel")"
    PLAN="${PLAN}${rel}	${dest_rel}
"
    FILE_COUNT=$((FILE_COUNT + 1))
    if [ -e "$DEST/$dest_rel" ] && [ "$FORCE" != true ]; then
        COLLISIONS="${COLLISIONS}  $DEST/$dest_rel
"
    fi
done <<< "$SRC_FILES"

if [ "$COLLISIONS" ] && [ "$DRY_RUN" != true ]; then
    err "destination files already exist:"
    printf '%s' "$COLLISIONS" >&2
    die "refusing to overwrite existing files" "冪等性のため既存ファイルは上書きしない" \
        "--force を付けて再実行するか、--dest に空のディレクトリを指定する"
fi

# ---------- dry-run ----------
if [ "$DRY_RUN" = true ]; then
    printf '%s' "$PLAN" | while IFS=$(printf '\t') read -r src dst; do
        [ "$src" ] && printf 'PLAN %s -> %s/%s\n' "$src" "$DEST" "$dst"
    done
    printf '{"status":"ok","dryRun":true,"dest":"%s","files":%d}\n' "$DEST" "$FILE_COUNT"
    exit 0
fi

# ---------- copy + replace ----------
mkdir -p "$DEST"
export SC_GROUP="$GROUP_ID" SC_PLUGIN_ID="$PLUGIN_ID" SC_PKG="$PACKAGE" \
       SC_PASCAL="$PASCAL" SC_CAMEL="$CAMEL" SC_NAME="$NAME"

printf '%s' "$PLAN" > "${TMPDIR:-/tmp}/scaffold-plan.$$"
while IFS=$(printf '\t') read -r src dst; do
    [ "$src" ] || continue
    mkdir -p "$DEST/$(dirname "$dst")"
    cp "$EXAMPLE_DIR/$src" "$DEST/$dst"
    # 置換は文脈の狭い順に適用する:
    #   1. "group:artifact:..." 依存表記 → group-id
    #   2. groupId = "..." → group-id
    #   3. 残りの "com.example.compilerpluginsetup" 単独文字列 → plugin-id
    #   4. その他 (package 宣言 / import / FQN) → package
    #   5. クラス名 prefix Example → PascalCase 名
    #   6. gradle plugin 短縮名 examplePlugin → camelCase 名
    #   7. rootProject.name "example-plugin" → kebab 名
    perl -pi -e '
        s/"\Qcom.example.compilerpluginsetup\E:/"$ENV{SC_GROUP}:/g;
        s/(groupId\s*=\s*)"\Qcom.example.compilerpluginsetup\E"/$1"$ENV{SC_GROUP}"/g;
        s/"\Qcom.example.compilerpluginsetup\E"/"$ENV{SC_PLUGIN_ID}"/g;
        s/\Qcom.example.compilerpluginsetup\E/$ENV{SC_PKG}/g;
        s/\bExample(?=[A-Z])/$ENV{SC_PASCAL}/g;
        s/\bexamplePlugin\b/$ENV{SC_CAMEL}/g;
        s/"example-plugin"/"$ENV{SC_NAME}"/g;
    ' "$DEST/$dst"
done < "${TMPDIR:-/tmp}/scaffold-plan.$$"
rm -f "${TMPDIR:-/tmp}/scaffold-plan.$$"

# ---------- kotlin version ----------
if [ "$KOTLIN_VERSION" ] && [ -f "$DEST/gradle/libs.versions.toml" ]; then
    SC_KOTLIN="$KOTLIN_VERSION" perl -pi -e 's/^kotlin = ".*"/kotlin = "$ENV{SC_KOTLIN}"/' \
        "$DEST/gradle/libs.versions.toml"
fi

# ---------- skip したモジュールの include を settings から除去 ----------
SKIPPED=""
if [ "$SKIP_GRADLE_PLUGIN" = true ]; then
    perl -ni -e 'print unless /include\(":gradle-plugin"\)/' "$DEST/settings.gradle.kts"
    SKIPPED="${SKIPPED}\"gradle-plugin\","
fi
if [ "$SKIP_INTEGRATION_TEST" = true ]; then
    perl -ni -e 'print unless /include\(":integration-test:/' "$DEST/settings.gradle.kts"
    SKIPPED="${SKIPPED}\"integration-test\","
fi
if [ "$SKIP_TEST" = true ]; then
    SKIPPED="${SKIPPED}\"test\","
fi
SKIPPED="${SKIPPED%,}"

# ---------- verify: 置換漏れが無いこと ----------
# ユーザー指定値が example の値そのものの場合は該当パターンをチェック対象から除外する
LEFT_PATTERNS=""
add_pattern() {
    if [ "$LEFT_PATTERNS" ]; then LEFT_PATTERNS="$LEFT_PATTERNS|$1"; else LEFT_PATTERNS="$1"; fi
}
if [ "$PACKAGE" != "$OLD_PKG" ] && [ "$PLUGIN_ID" != "$OLD_PKG" ] && [ "$GROUP_ID" != "$OLD_PKG" ]; then
    add_pattern 'com\.example\.compilerpluginsetup'
fi
if [ "$NAME" != "example-plugin" ]; then add_pattern 'example-plugin'; fi
if [ "$PASCAL" != "$OLD_PREFIX" ]; then add_pattern 'Example[A-Z]'; fi
LEFTOVER=""
if [ "$LEFT_PATTERNS" ]; then
    LEFTOVER="$(grep -rlE "$LEFT_PATTERNS" "$DEST" 2>/dev/null || true)"
fi
if [ "$LEFTOVER" ]; then
    err "replacement leftovers detected in:"
    printf '%s\n' "$LEFTOVER" >&2
    die "scaffold finished with unreplaced placeholders" "置換ルールが網羅できていない" \
        "上記ファイルを手動で修正し、TBSten/skills に issue 報告する"
fi

printf 'Scaffolded Kotlin Compiler Plugin project:\n'
printf '  dest:      %s\n' "$DEST"
printf '  name:      %s (class prefix: %s, gradle plugin name: %s)\n' "$NAME" "$PASCAL" "$CAMEL"
printf '  group-id:  %s\n' "$GROUP_ID"
printf '  plugin-id: %s\n' "$PLUGIN_ID"
printf '  package:   %s\n' "$PACKAGE"
printf '  files:     %d\n' "$FILE_COUNT"
printf '{"status":"ok","dryRun":false,"dest":"%s","files":%d,"name":"%s","groupId":"%s","pluginId":"%s","package":"%s","skipped":[%s]}\n' \
    "$DEST" "$FILE_COUNT" "$NAME" "$GROUP_ID" "$PLUGIN_ID" "$PACKAGE" "$SKIPPED"
