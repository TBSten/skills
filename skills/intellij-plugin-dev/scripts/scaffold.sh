#!/usr/bin/env bash
#
# scaffold.sh — intellij-plugin-dev skill の example/ (IntelliJ Platform plugin の独立 Gradle
# ビルド一式) を対象ディレクトリへ決定的に展開する。コピー / パッケージ・plugin ID・表示名の
# 置換 / ファイル rename の SSoT はこの script。読解・書き換え・再実装せず、そのまま実行する。
#
# Usage:
#   scaffold.sh --dest <dir> --package <pkg> --plugin-id <id> --plugin-name <name>
#               [--dry-run] [--force]
#
# 置換仕様 (長いキー優先の単一パス置換なので、置換結果が再置換されることはない):
#   <id>com.example.plugin</id> -> <id>{--plugin-id}</id>   (plugin.xml の ID のみ)
#   com.example.plugin          -> --package (パス形 com/example/plugin も)
#   example-plugin              -> --plugin-name の kebab-case (rootProject.name)
#   Example Plugin              -> --plugin-name (plugin.xml の表示名 / gallery タイトル)
#   Example                    -> --plugin-name の PascalCase (クラス名接頭辞 / tool window id。
#                                  ファイル名にも適用され ExampleToolWindowFactory.kt 等が rename される)
#
# 出力: 配置ファイル一覧 + 末尾 1 行 JSON {"ok":true,"files":N,"dest":"..."}
#
# 生成後: SKILL.md「example scaffold」の手順に従う (CUSTOMIZE を埋める → gradle wrapper →
# buildPlugin / test / updatePreview で golden 初回生成)。

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SKILL_DIR=$(dirname "$SCRIPT_DIR")
EXAMPLE_DIR="$SKILL_DIR/example"

usage() {
    sed -n '/^# scaffold\.sh/,/^# 生成後/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

die() {
    # die "<何が>" "<なぜ>" "<どう直すか>"
    {
        echo "ERROR: $1"
        [ $# -ge 2 ] && echo "  why: $2"
        [ $# -ge 3 ] && echo "  fix: $3"
    } >&2
    exit 1
}

# ---------------------------------------------------------------- 引数パース
DEST="" PKG="" PLUGIN_ID="" PLUGIN_NAME=""
DRY_RUN=false FORCE=false

need_value() {
    [ $# -ge 2 ] || die "オプション $1 に値がない" \
        "$1 は値を取るオプション" "例: $1 <value> の形で渡す"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --dest)        need_value "$@"; DEST=$2; shift 2 ;;
        --package)     need_value "$@"; PKG=$2; shift 2 ;;
        --plugin-id)   need_value "$@"; PLUGIN_ID=$2; shift 2 ;;
        --plugin-name) need_value "$@"; PLUGIN_NAME=$2; shift 2 ;;
        --dry-run)     DRY_RUN=true; shift ;;
        --force)       FORCE=true; shift ;;
        -h|--help)     usage; exit 0 ;;
        *) die "不明なオプション: $1" "このオプションは定義されていない" "scaffold.sh --help で使い方を確認する" ;;
    esac
done

# ---------------------------------------------------------------- preflight
[ -n "$DEST" ]        || die "--dest がない"        "生成先ディレクトリは必須" "--dest <dir> を渡す"
[ -n "$PKG" ]         || die "--package がない"     "ルートパッケージは必須" "--package com.acme.myplugin のように渡す"
[ -n "$PLUGIN_ID" ]   || die "--plugin-id がない"   "plugin.xml の <id> は必須" "--plugin-id com.acme.myplugin のように渡す"
[ -n "$PLUGIN_NAME" ] || die "--plugin-name がない" "plugin.xml の <name> (表示名) は必須" "--plugin-name \"My Plugin\" のように渡す"

echo "$PKG" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$' \
    || die "--package '$PKG' がパッケージ名として不正" \
        "Kotlin の package 宣言とディレクトリパスに使う" \
        "小文字ドット区切りにする (例: com.acme.myplugin)"
echo "$PLUGIN_ID" | grep -Eq '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_-]+)+$' \
    || die "--plugin-id '$PLUGIN_ID' が plugin ID として不正" \
        "Marketplace / IDE 内で一意な reverse-DNS 形式が必要" \
        "ドット区切りの英数字にする (例: com.acme.my-plugin)"

[ -d "$EXAMPLE_DIR" ] || die "example/ が見つからない: $EXAMPLE_DIR" \
    "skill のコピー元一式が無いと何も生成できない" \
    "skill を丸ごと (example/ を含めて) 取得しているか確認する"
command -v perl >/dev/null 2>&1 || die "perl が見つからない" \
    "プレースホルダー置換に perl を使う (macOS / Linux 両対応のため。BSD sed -i は使わない)" \
    "perl をインストールする"

# ---------------------------------------------------------------- 派生値
pascal_of_name() {
    # 表示名 -> PascalCase (非英数字区切り。"My Cool Plugin" -> MyCoolPlugin)
    local out="" part
    for part in $(printf '%s' "$1" | tr -cs '[:alnum:]' ' '); do
        out+=$(printf '%s' "${part:0:1}" | tr '[:lower:]' '[:upper:]')${part:1}
    done
    printf '%s' "$out"
}

kebab_of_name() {
    # 表示名 -> kebab-case ("My Cool Plugin" -> my-cool-plugin)
    local k
    k=$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | tr -cs '[:alnum:]' '-')
    k=${k#-}
    k=${k%-}
    printf '%s' "$k"
}

PASCAL=$(pascal_of_name "$PLUGIN_NAME")
KEBAB=$(kebab_of_name "$PLUGIN_NAME")
echo "$PASCAL" | grep -Eq '^[A-Za-z][A-Za-z0-9]*$' \
    || die "--plugin-name '$PLUGIN_NAME' からクラス名接頭辞を派生できない (派生結果: '$PASCAL')" \
        "表示名の英数字部分から PascalCase (クラス名 / tool window id) を作るため、ASCII 英字始まりが必要" \
        "ASCII 英字を含む表示名にする (例: --plugin-name \"My Plugin\")"

PKG_PATH=${PKG//./\/}

# ---------------------------------------------------------------- 置換
export S_PKG=$PKG S_PKG_PATH=$PKG_PATH S_PLUGIN_ID=$PLUGIN_ID S_PLUGIN_NAME=$PLUGIN_NAME \
    S_PASCAL=$PASCAL S_KEBAB=$KEBAB

# 全置換を長いキー優先の単一パスで行う。s///g は置換結果を再走査しないので、
# 置換後の文字列に別のキーが含まれていても壊れない。
transform_full() {
    perl -0777 -pe '
        BEGIN {
            %m = (
                "<id>com.example.plugin</id>" => "<id>$ENV{S_PLUGIN_ID}</id>",
                "com.example.plugin"          => $ENV{S_PKG},
                "com/example/plugin"          => $ENV{S_PKG_PATH},
                "example-plugin"              => $ENV{S_KEBAB},
                "Example Plugin"              => $ENV{S_PLUGIN_NAME},
                "Example"                     => $ENV{S_PASCAL},
            );
            $re = join "|", map { quotemeta } sort { length($b) <=> length($a) } keys %m;
        }
        s/($re)/$m{$1}/g;
    '
}

transform_path() {
    printf '%s' "$1" | transform_full
}

# ---------------------------------------------------------------- 配置計画
PLAN_SRC=()
PLAN_DST=()

while IFS= read -r src; do
    rel=${src#"$EXAMPLE_DIR"/}
    PLAN_SRC+=("$src")
    PLAN_DST+=("$(transform_path "$rel")") # com/example/plugin と Example* ファイルの rename
done < <(find "$EXAMPLE_DIR" -type f ! -name '.DS_Store' | LC_ALL=C sort)

TOTAL=${#PLAN_SRC[@]}
[ "$TOTAL" -gt 0 ] || die "配置対象が 0 件" "example/ が空" "skill を丸ごと取得し直す"

# ---------------------------------------------------------------- 冪等性チェック
conflicts=()
for i in $(seq 0 $((TOTAL - 1))); do
    [ -e "$DEST/${PLAN_DST[$i]}" ] && conflicts+=("${PLAN_DST[$i]}")
done

if [ ${#conflicts[@]} -gt 0 ] && ! $FORCE && ! $DRY_RUN; then
    {
        echo "ERROR: 生成先に既存ファイルが ${#conflicts[@]} 件ある (上書きしない)"
        echo "  why: 冪等性のため、明示しない限り既存ファイルを壊さない"
        echo "  fix: 内容を確認して --force で上書きするか、別の --dest を使う"
        echo "  conflicts:"
        n=0
        for c in "${conflicts[@]}"; do
            echo "    - $c"
            n=$((n + 1))
            if [ "$n" -ge 20 ]; then
                echo "    ... ほか $((${#conflicts[@]} - 20)) 件"
                break
            fi
        done
    } >&2
    exit 1
fi

# ---------------------------------------------------------------- dry-run
resolve_dest() {
    if [ -d "$DEST" ]; then (cd "$DEST" && pwd); else printf '%s' "$DEST"; fi
}

if $DRY_RUN; then
    echo "## DRY RUN: 配置予定 ($TOTAL files) — 書き込みは行わない"
    for i in $(seq 0 $((TOTAL - 1))); do
        marker=""
        [ -e "$DEST/${PLAN_DST[$i]}" ] && marker="  [exists]"
        echo "  ${PLAN_DST[$i]}  <=  example/${PLAN_SRC[$i]#"$EXAMPLE_DIR"/}$marker"
    done
    [ ${#conflicts[@]} -gt 0 ] && echo "NOTE: [exists] の ${#conflicts[@]} 件は実行時に --force が必要"
    echo "{\"ok\":true,\"dryRun\":true,\"files\":$TOTAL,\"dest\":\"$(resolve_dest)\"}"
    exit 0
fi

# ---------------------------------------------------------------- 書き込み
mkdir -p "$DEST"
DEST_ABS=$(resolve_dest)

for i in $(seq 0 $((TOTAL - 1))); do
    src=${PLAN_SRC[$i]}
    dst="$DEST_ABS/${PLAN_DST[$i]}"
    mkdir -p "$(dirname "$dst")"
    transform_full < "$src" > "$dst"
done

# ---------------------------------------------------------------- 結果出力
echo "## 配置ファイル ($TOTAL files) -> $DEST_ABS"
for i in $(seq 0 $((TOTAL - 1))); do
    echo "  ${PLAN_DST[$i]}"
done
echo "NOTE: Gradle wrapper は同梱していない — $DEST_ABS で既存 wrapper を使うか 'gradle wrapper' で生成する"
echo "NOTE: 初回 './gradlew buildPlugin' は SDK DL 込みで ~4〜5 分 (references/setup/basics.md)"
echo "NOTE: golden は空 — './gradlew updatePreview' で初回生成して snapshots/preview を commit する"
echo "{\"ok\":true,\"files\":$TOTAL,\"dest\":\"$DEST_ABS\",\"package\":\"$PKG\",\"pluginId\":\"$PLUGIN_ID\"}"
