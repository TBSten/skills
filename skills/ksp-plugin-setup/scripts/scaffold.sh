#!/usr/bin/env bash
#
# scaffold.sh — ksp-plugin-setup skill の example/ / assets/rules/ を対象プロジェクトへ
# 決定的に展開する。コピー / ディレクトリ再マッピング / プレースホルダー置換 / rename /
# META-INF services 配置 / .claude/rules 配置の SSoT はこの script。
#
# Usage:
#   scaffold.sh --dest <dir> --name <kebab-case> --package <pkg> --annotation <PascalCase>
#               [--group-id <id>] [--owner <owner>] [--repo <repo>]
#               [--kotlin-version <v>] [--ksp-version <kotlin>-<ksp>]
#               [--skip-ci] [--skip-rules] [--skip-test-module]
#               [--dry-run] [--force]
#
# 置換仕様 (単一パス置換なので、置換結果が再置換されることはない):
#   com.example.ksppluginsetup   -> --package (パス形 com/example/ksppluginsetup も)
#   ksppluginsetup               -> --name    (KSP option / snapshot プロパティ / {{...}} トークンの接頭辞)
#   <project-name>               -> --name    (libs.versions.<project-name> アクセサだけは kebab -> dotted)
#   <group-id>                   -> --group-id (省略時は --package)
#   <owner> / <repo>             -> --owner / --repo (省略時は dest の git remote origin から推定)
#   <year>                       -> date +%Y
#   Example                      -> --name の PascalCase
#   Greeting / greeting / GREETING -> --annotation の PascalCase / camelCase / SCREAMING_SNAKE
#   (greetingFun は greeting -> camelCase 置換で <camel>Fun になる)
#
# ディレクトリ再マッピング:
#   runtime/*.kt                 -> <name>-runtime/src/commonMain/kotlin/<pkg-path>/
#   runtime/build.gradle.kts     -> <name>-runtime/build.gradle.kts
#   ksp/main/**                  -> <name>-ksp/src/main/kotlin/<pkg-path>/ksp/**
#   ksp/test/**                  -> <name>-ksp/src/test/kotlin/<pkg-path>/ksp/**
#   ksp/build.gradle.kts         -> <name>-ksp/build.gradle.kts
#   ksp/META-INF-services.txt    -> (最終行の FQN を) <name>-ksp/src/main/resources/META-INF/services/...
#   test/*TestData.kt            -> test/src/commonMain/kotlin/<pkg-path>/test/<annotCamel>/
#   test/*.kt                    -> test/src/commonTest/kotlin/<pkg-path>/test/<annotCamel>/
#   それ以外 (build ファイル / CI) -> 同じ相対パス
#   assets/rules/*.md            -> <dest>/.claude/rules/ (<project-name> のみ置換)
#
# 出力: 配置ファイル一覧 + 1 行 JSON {"ok":true,"files":N,"dest":"..."}

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SKILL_DIR=$(dirname "$SCRIPT_DIR")
EXAMPLE_DIR="$SKILL_DIR/example"
RULES_DIR="$SKILL_DIR/assets/rules"

usage() {
    sed -n '/^# scaffold\.sh/,/^# 出力/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
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
DEST="" NAME="" PKG="" ANNOT=""
GROUP_ID="" OWNER="" REPO="" KOTLIN_VERSION="" KSP_VERSION=""
SKIP_CI=false SKIP_RULES=false SKIP_TEST=false DRY_RUN=false FORCE=false

need_value() {
    [ $# -ge 2 ] || die "オプション $1 に値がない" \
        "$1 は値を取るオプション" "例: $1 <value> の形で渡す"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --dest)             need_value "$@"; DEST=$2; shift 2 ;;
        --name)             need_value "$@"; NAME=$2; shift 2 ;;
        --package)          need_value "$@"; PKG=$2; shift 2 ;;
        --annotation)       need_value "$@"; ANNOT=$2; shift 2 ;;
        --group-id)         need_value "$@"; GROUP_ID=$2; shift 2 ;;
        --owner)            need_value "$@"; OWNER=$2; shift 2 ;;
        --repo)             need_value "$@"; REPO=$2; shift 2 ;;
        --kotlin-version)   need_value "$@"; KOTLIN_VERSION=$2; shift 2 ;;
        --ksp-version)      need_value "$@"; KSP_VERSION=$2; shift 2 ;;
        --skip-ci)          SKIP_CI=true; shift ;;
        --skip-rules)       SKIP_RULES=true; shift ;;
        --skip-test-module) SKIP_TEST=true; shift ;;
        --dry-run)          DRY_RUN=true; shift ;;
        --force)            FORCE=true; shift ;;
        -h|--help)          usage; exit 0 ;;
        *) die "不明なオプション: $1" "このオプションは定義されていない" "scaffold.sh --help で使い方を確認する" ;;
    esac
done

# ---------------------------------------------------------------- preflight
[ -n "$DEST" ]  || die "--dest がない"  "生成先ディレクトリは必須" "--dest <dir> を渡す"
[ -n "$NAME" ]  || die "--name がない"  "プロジェクト名 (kebab-case) は必須" "--name my-plugin のように渡す"
[ -n "$PKG" ]   || die "--package がない" "ルートパッケージは必須" "--package com.example.myplugin のように渡す"
[ -n "$ANNOT" ] || die "--annotation がない" "最初のアノテーション名 (PascalCase) は必須" "--annotation Greeting のように渡す"

echo "$NAME" | grep -Eq '^[a-z][a-z0-9]*(-[a-z0-9]+)*$' \
    || die "--name '$NAME' が kebab-case でない" \
        "モジュール名 / version catalog キー / KSP option 接頭辞に使うため 'my-plugin' 形式が必要" \
        "小文字英数字とハイフンのみの名前にする (例: my-plugin)"
echo "$PKG" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$' \
    || die "--package '$PKG' がパッケージ名として不正" \
        "Kotlin の package 宣言とディレクトリパスに使う" \
        "小文字ドット区切りにする (例: com.example.myplugin)"
echo "$ANNOT" | grep -Eq '^[A-Z][A-Za-z0-9]*$' \
    || die "--annotation '$ANNOT' が PascalCase でない" \
        "クラス名 / ファイル名 / ディレクトリ名の派生元になる" \
        "大文字始まりの英数字にする (例: Greeting, MyAnnot)"

[ -d "$EXAMPLE_DIR" ] || die "example/ が見つからない: $EXAMPLE_DIR" \
    "skill のコピー元一式が無いと何も生成できない" \
    "skill を丸ごと (example/ を含めて) 取得しているか確認する。prompt 経由なら sparse clone で skills/ksp-plugin-setup 全体を取得する"
if ! $SKIP_RULES; then
    [ -d "$RULES_DIR" ] || die "assets/rules/ が見つからない: $RULES_DIR" \
        "生成先の .claude/rules/ に配置するテンプレートが無い" \
        "skill を丸ごと取得するか、rules が不要なら --skip-rules を付ける"
fi
command -v perl >/dev/null 2>&1 || die "perl が見つからない" \
    "プレースホルダー置換に perl を使う (macOS / Linux 両対応のため)" "perl をインストールする"

# ---------------------------------------------------------------- 派生値
pascal_of() {
    # kebab-case -> PascalCase (my-plugin -> MyPlugin)
    local out="" part
    local IFS='-'
    for part in $1; do
        out+=$(printf '%s' "${part:0:1}" | tr '[:lower:]' '[:upper:]')${part:1}
    done
    printf '%s' "$out"
}

PASCAL=$(pascal_of "$NAME")
NAME_DOTTED=${NAME//-/.}
PKG_PATH=${PKG//./\/}
ANNOT_CAMEL=$(printf '%s' "${ANNOT:0:1}" | tr '[:upper:]' '[:lower:]')${ANNOT:1}
ANNOT_UPPER=$(printf '%s' "$ANNOT" | perl -pe 's/(?<!^)([A-Z])/_$1/g; $_ = uc($_)')
YEAR=$(date +%Y)
[ -n "$GROUP_ID" ] || GROUP_ID=$PKG

# owner/repo: 未指定なら dest の git remote origin から推定
if [ -z "$OWNER" ] || [ -z "$REPO" ]; then
    remote_url=""
    if [ -d "$DEST" ]; then
        remote_url=$(git -C "$DEST" remote get-url origin 2>/dev/null || true)
    fi
    if [ -n "$remote_url" ]; then
        case "$remote_url" in
            *github.com*)
                rest=${remote_url#*github.com}
                rest=${rest#[:/]}
                rest=${rest%.git}
                guess_owner=${rest%%/*}
                guess_repo=${rest#*/}
                guess_repo=${guess_repo%%/*}
                [ -n "$OWNER" ] || OWNER=$guess_owner
                [ -n "$REPO" ]  || REPO=$guess_repo
                ;;
        esac
    fi
fi
if [ -z "$OWNER" ] || [ -z "$REPO" ]; then
    die "GitHub の owner/repo を決定できない" \
        "publish 設定 (POM の url/scm) と issue リンクに <owner>/<repo> を埋め込むが、--owner/--repo が無く、--dest の git remote origin からも推定できなかった" \
        "--owner <owner> --repo <repo> を渡す (または dest を GitHub リポジトリの clone にする)"
fi

# ---------------------------------------------------------------- 置換
export K_PKG=$PKG K_PKG_PATH=$PKG_PATH K_NAME=$NAME K_NAME_DOTTED=$NAME_DOTTED \
    K_GROUP_ID=$GROUP_ID K_OWNER=$OWNER K_REPO=$REPO K_YEAR=$YEAR \
    K_PASCAL=$PASCAL K_ANNOT=$ANNOT K_ANNOT_CAMEL=$ANNOT_CAMEL K_ANNOT_UPPER=$ANNOT_UPPER

# 全置換を長いキー優先の単一パスで行う。s///g は置換結果を再走査しないので、
# 置換後の文字列に別のキーが含まれていても壊れない。
transform_full() {
    perl -0777 -pe '
        BEGIN {
            %m = (
                "libs.versions.<project-name>" => "libs.versions.$ENV{K_NAME_DOTTED}",
                "com.example.ksppluginsetup"   => $ENV{K_PKG},
                "com/example/ksppluginsetup"   => $ENV{K_PKG_PATH},
                "ksppluginsetup"               => $ENV{K_NAME},
                "<project-name>"               => $ENV{K_NAME},
                "<group-id>"                   => $ENV{K_GROUP_ID},
                "<owner>"                      => $ENV{K_OWNER},
                "<repo>"                       => $ENV{K_REPO},
                "<year>"                       => $ENV{K_YEAR},
                "GREETING"                     => $ENV{K_ANNOT_UPPER},
                "Greeting"                     => $ENV{K_ANNOT},
                "greeting"                     => $ENV{K_ANNOT_CAMEL},
                "Example"                      => $ENV{K_PASCAL},
            );
            $re = join "|", map { quotemeta } sort { length($b) <=> length($a) } keys %m;
        }
        s/($re)/$m{$1}/g;
    '
}

transform_rules() {
    # .claude/rules/ は <project-name> のみ置換 (SKILL.md Step: 規約の常設)
    perl -0777 -pe 's/\Q<project-name>\E/$ENV{K_NAME}/g'
}

transform_path() {
    printf '%s' "$1" | transform_full
}

apply_versions_toml() {
    # --kotlin-version / --ksp-version を libs.versions.toml に反映。
    # ksp は `<kotlin>-<ksp>` 形式: --ksp-version はフル文字列で渡す。
    # --kotlin-version のみ指定時は ksp の kotlin 部分 (最後のハイフンより前) を追従させる。
    local content
    content=$(cat)
    if [ -n "$KOTLIN_VERSION" ]; then
        content=$(printf '%s\n' "$content" | K_V=$KOTLIN_VERSION perl -pe 's/^kotlin = "[^"]*"/kotlin = "$ENV{K_V}"/')
        if [ -z "$KSP_VERSION" ]; then
            content=$(printf '%s\n' "$content" | K_V=$KOTLIN_VERSION perl -pe 's/^ksp = ".*-([^-"]+)"/ksp = "$ENV{K_V}-$1"/')
        fi
    fi
    if [ -n "$KSP_VERSION" ]; then
        content=$(printf '%s\n' "$content" | K_V=$KSP_VERSION perl -pe 's/^ksp = "[^"]*"/ksp = "$ENV{K_V}"/')
    fi
    printf '%s\n' "$content"
}

# ---------------------------------------------------------------- 配置計画
PLAN_SRC=()
PLAN_DST=()
PLAN_MODE=() # full | meta | rules

add_plan() {
    PLAN_SRC+=("$1")
    PLAN_DST+=("$2")
    PLAN_MODE+=("$3")
}

while IFS= read -r src; do
    rel=${src#"$EXAMPLE_DIR"/}
    mode=full
    case "$rel" in
        .github/*)
            $SKIP_CI && continue
            dstrel=$rel ;;
        runtime/build.gradle.kts)
            dstrel="$NAME-runtime/build.gradle.kts" ;;
        runtime/*.kt)
            dstrel="$NAME-runtime/src/commonMain/kotlin/$PKG_PATH/$(basename "$rel")" ;;
        ksp/META-INF-services.txt)
            dstrel="$NAME-ksp/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"
            mode=meta ;;
        ksp/build.gradle.kts)
            dstrel="$NAME-ksp/build.gradle.kts" ;;
        ksp/main/*)
            dstrel="$NAME-ksp/src/main/kotlin/$PKG_PATH/ksp/${rel#ksp/main/}" ;;
        ksp/test/*)
            dstrel="$NAME-ksp/src/test/kotlin/$PKG_PATH/ksp/${rel#ksp/test/}" ;;
        test/build.gradle.kts)
            $SKIP_TEST && continue
            dstrel="test/build.gradle.kts" ;;
        test/*TestData.kt)
            $SKIP_TEST && continue
            dstrel="test/src/commonMain/kotlin/$PKG_PATH/test/$ANNOT_CAMEL/$(basename "$rel")" ;;
        test/*.kt)
            $SKIP_TEST && continue
            dstrel="test/src/commonTest/kotlin/$PKG_PATH/test/$ANNOT_CAMEL/$(basename "$rel")" ;;
        test/*)
            $SKIP_TEST && continue
            dstrel=$rel ;;
        *)
            dstrel=$rel ;;
    esac
    dstrel=$(transform_path "$dstrel") # Greeting/Example 系ファイル・ディレクトリの rename
    add_plan "$src" "$dstrel" "$mode"
done < <(find "$EXAMPLE_DIR" -type f ! -name '.DS_Store' | LC_ALL=C sort)

if ! $SKIP_RULES; then
    while IFS= read -r src; do
        add_plan "$src" ".claude/rules/$(basename "$src")" rules
    done < <(find "$RULES_DIR" -maxdepth 1 -type f -name '*.md' | LC_ALL=C sort)
fi

TOTAL=${#PLAN_SRC[@]}
[ "$TOTAL" -gt 0 ] || die "配置対象が 0 件" "example/ が空か、skip オプションで全て除外された" "example/ の中身と --skip-* の組み合わせを確認する"

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
        echo "  ${PLAN_DST[$i]}  <=  ${PLAN_SRC[$i]#"$SKILL_DIR"/}$marker"
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
    dstrel=${PLAN_DST[$i]}
    mode=${PLAN_MODE[$i]}
    dst="$DEST_ABS/$dstrel"
    mkdir -p "$(dirname "$dst")"
    case "$mode" in
        meta)
            # META-INF-services.txt の最終行 (provider FQN) だけを置換して配置する
            awk 'NF { line = $0 } END { print line }' "$src" | transform_full > "$dst"
            ;;
        rules)
            transform_rules < "$src" > "$dst"
            ;;
        full)
            content=$(transform_full < "$src")
            if [ "$dstrel" = "settings.gradle.kts" ] && $SKIP_TEST; then
                content=$(printf '%s\n' "$content" | grep -v '^include(":test")$')
            fi
            if [ "$dstrel" = "gradle/libs.versions.toml" ]; then
                content=$(printf '%s\n' "$content" | apply_versions_toml)
            fi
            printf '%s\n' "$content" > "$dst"
            ;;
    esac
done

# ---------------------------------------------------------------- 結果出力
echo "## 配置ファイル ($TOTAL files) -> $DEST_ABS"
for i in $(seq 0 $((TOTAL - 1))); do
    echo "  ${PLAN_DST[$i]}"
done
$SKIP_CI   && echo "  (skip: CI workflows)"
$SKIP_RULES && echo "  (skip: .claude/rules)"
$SKIP_TEST && echo "  (skip: test モジュール — settings.gradle.kts の include(\":test\") も除去済み)"
echo "{\"ok\":true,\"files\":$TOTAL,\"dest\":\"$DEST_ABS\"}"
