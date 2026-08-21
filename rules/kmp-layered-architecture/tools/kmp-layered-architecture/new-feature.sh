#!/usr/bin/env bash
#
# new-feature.sh — docs/architecture/templates/feature/ から新規 feature (画面) の
# 雛形を ui/feature/<feature>/ に生成する。
#
# Usage:
#   bash tools/kmp-layered-architecture/new-feature.sh <FeatureName> <package> [--force]
#
#   <FeatureName> ... PascalCase の feature 名 (例: Home, UserDetail)
#   <package>     ... 生成するファイルの package (例: com.example.app.ui.feature.home)
#   --force       ... 生成先に既存ファイルがあっても上書きする
#
# 生成物:
#   ui/feature/<feature>/src/commonMain/kotlin/<package-path>/
#     ├── <FeatureName>Screen.kt
#     ├── <FeatureName>ViewModel.kt
#     └── Navigation.kt
#
# script では追記できない既存ファイル (settings.gradle.kts / ui/navigation の
# Screen.kt / AppNavigator.kt / AppNavigation.kt / DI Providers) への変更は、
# 実行後に stdout へ印字されるスニペットに従って追記すること。

set -euo pipefail

die() {
    # die "<何が>" "<なぜ>" "<どう直すか>"
    {
        echo "ERROR: $1"
        [ $# -ge 2 ] && echo "  why: $2"
        [ $# -ge 3 ] && echo "  fix: $3"
    } >&2
    exit 1
}

usage() {
    sed -n '/^# new-feature\.sh/,/^# 実行後に/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

# ---------------------------------------------------------------- 引数パース
FEATURE="" PKG="" FORCE=false
for arg in "$@"; do
    case "$arg" in
        -h|--help) usage; exit 0 ;;
        --force)   FORCE=true ;;
        --*)       die "不明なオプション: $arg" "このオプションは定義されていない" "--help で使い方を確認する" ;;
        *)
            if [ -z "$FEATURE" ]; then FEATURE=$arg
            elif [ -z "$PKG" ]; then PKG=$arg
            else die "余分な引数: $arg" "位置引数は <FeatureName> <package> の 2 つだけ" "--help で使い方を確認する"
            fi
            ;;
    esac
done

# ---------------------------------------------------------------- preflight
[ -n "$FEATURE" ] || die "<FeatureName> がない" \
    "生成するファイル名・クラス名の元になる" \
    "例: bash tools/kmp-layered-architecture/new-feature.sh Home com.example.app.ui.feature.home"
[ -n "$PKG" ] || die "<package> がない" \
    "生成するファイルの package 宣言と配置パスに使う" \
    "例: bash tools/kmp-layered-architecture/new-feature.sh Home com.example.app.ui.feature.home"

echo "$FEATURE" | grep -Eq '^[A-Z][A-Za-z0-9]*$' \
    || die "<FeatureName> '$FEATURE' が PascalCase でない" \
        "クラス名 (\${FeatureName}Screen 等) の派生元になる" \
        "大文字始まりの英数字にする (例: Home, UserDetail)"
echo "$PKG" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$' \
    || die "<package> '$PKG' がパッケージ名として不正" \
        "Kotlin の package 宣言とディレクトリパスに使う" \
        "小文字ドット区切りにする (例: com.example.app.ui.feature.home)"

# テンプレートはカレントディレクトリ優先、無ければ script 位置から解決する
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
TEMPLATE_DIR="docs/architecture/templates/feature"
if [ ! -d "$TEMPLATE_DIR" ]; then
    TEMPLATE_DIR="$SCRIPT_DIR/../../docs/architecture/templates/feature"
fi
[ -d "$TEMPLATE_DIR" ] || die "テンプレートが見つからない: docs/architecture/templates/feature" \
    "kmp-layered-architecture rule のテンプレートが無いと雛形を生成できない" \
    "プロジェクトルートで実行しているか確認する。無ければ kmp-layered-architecture rule を再インストールする"

# ---------------------------------------------------------------- 派生値
FEATURE_LOWER=$(printf '%s' "$FEATURE" | tr '[:upper:]' '[:lower:]')       # ディレクトリ名用 (userdetail)
FEATURE_CAMEL=$(printf '%s' "${FEATURE:0:1}" | tr '[:upper:]' '[:lower:]')${FEATURE:1}  # 識別子用 (userDetail)
PKG_PATH=$(printf '%s' "$PKG" | tr '.' '/')
OUT_DIR="ui/feature/$FEATURE_LOWER/src/commonMain/kotlin/$PKG_PATH"

# ---------------------------------------------------------------- 冪等性チェック
SRC_FILES=("__Feature__Screen.kt" "__Feature__ViewModel.kt" "Navigation.kt")
DST_FILES=("${FEATURE}Screen.kt" "${FEATURE}ViewModel.kt" "Navigation.kt")

conflicts=()
for dst in "${DST_FILES[@]}"; do
    [ -e "$OUT_DIR/$dst" ] && conflicts+=("$OUT_DIR/$dst")
done
if [ ${#conflicts[@]} -gt 0 ] && ! $FORCE; then
    {
        echo "ERROR: 生成先に既存ファイルが ${#conflicts[@]} 件ある (上書きしない)"
        echo "  why: 冪等性のため、明示しない限り既存ファイルを壊さない"
        echo "  fix: 内容を確認して --force で上書きするか、既存ファイルを退避する"
        echo "  conflicts:"
        for c in "${conflicts[@]}"; do
            echo "    - $c"
        done
    } >&2
    exit 1
fi

# ---------------------------------------------------------------- 生成
render() {
    # __Feature__ / __feature__ / __package__ を置換する (in-place 置換なし・BSD/GNU sed 共通構文のみ)
    sed -e "s/__Feature__/$FEATURE/g" \
        -e "s/__feature__/$FEATURE_CAMEL/g" \
        -e "s/__package__/$PKG/g" \
        "$1"
}

mkdir -p "$OUT_DIR"
for i in 0 1 2; do
    render "$TEMPLATE_DIR/${SRC_FILES[$i]}" > "$OUT_DIR/${DST_FILES[$i]}"
done

echo "## 生成ファイル (3 files)"
for dst in "${DST_FILES[@]}"; do
    echo "  $OUT_DIR/$dst"
done
echo
echo "## 次にやること: 以下のスニペットを既存ファイルに追記する (script では追記できない)"
echo
echo "--- 1. settings.gradle.kts ---"
echo "include(\":ui:feature:$FEATURE_LOWER\")"
echo
echo "    (build.gradle.kts は既存の feature モジュールからコピーして作成する)"
echo
echo "--- 2. ui/navigation の Screen.kt --- (sealed interface Screen に追加)"
echo "@Serializable"
echo "data object $FEATURE : Screen"
echo
echo "--- 3. ui/navigation の AppNavigator.kt ---"
echo "// (a) interface AppNavigator の親リストに追加:"
echo "//     interface AppNavigator : ..., ${FEATURE}Navigator {"
echo "// (b) AppNavigatorImpl に ${FEATURE}Navigator のメソッドを実装 (onBack は既存実装を共有):"
echo "// (c) この画面へ遷移する側の Navigator (例: HomeNavigator) に to$FEATURE() を追加して実装:"
echo "override fun to$FEATURE() { backstack.add($FEATURE) }"
echo
echo "--- 4. ui/navigation の AppNavigation.kt --- (entryProvider に追加)"
echo "entry<$FEATURE> {"
echo "    ${FEATURE}Screen()"
echo "}"
echo
echo "--- 5. AppNavigator の DI Providers --- (@Binds を追加)"
echo "@Binds"
echo "val AppNavigator.binds${FEATURE}Navigator: ${FEATURE}Navigator"
echo
echo "{\"ok\":true,\"files\":3,\"feature\":\"$FEATURE\",\"outDir\":\"$OUT_DIR\"}"
