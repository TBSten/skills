#!/usr/bin/env bash
#
# scaffold.sh — react-vite-supabase-starter skill の example/ を新規プロジェクトへ
# 決定的に展開する。ファイル配置 / プレースホルダー置換 / pnpm install /
# shadcn/ui コンポーネント生成 / .env.local 雛形作成の SSoT はこの script。
#
# Usage:
#   scaffold.sh --name <project> --app-name <表示名> [--primary <hex>]
#               [--dest <dir>] [--no-supabase] [--skip-install]
#               [--dry-run] [--force]
#
# オプション:
#   --name <project>    プロジェクト名 (kebab-case)。root package.json の name になる (必須)
#   --app-name <表示名>  UI に表示するアプリ名。header.tsx / index.html の <app-name> を置換 (必須)
#   --primary <hex>     プライマリカラー (#RRGGBB)。index.css の --primary / --ring を置換
#                       (省略時はデフォルト #8F5A3C のまま)
#   --dest <dir>        生成先ディレクトリ (省略時は ./<project>)
#   --no-supabase       .env.local を作らず、Supabase 関連コードの手動除去手順を出力する
#                       (v1 では配置自体は Supabase あり構成のまま。後述の ACTION_REQUIRED 参照)
#   --skip-install      ネットワークを使う Step 4 (pnpm install) / Step 5 (shadcn add) を
#                       スキップし、後で実行するコマンドを出力する
#   --dry-run           配置予定と実行予定コマンドを表示するだけで何も書き込まない
#   --force             生成先の既存ファイルを上書きする (デフォルトは上書き禁止)
#
# 置換仕様 (単一パス置換。置換結果が再置換されることはない):
#   <project-name>  -> --name       (root/package.json のみ)
#   <app-name>      -> --app-name   (web/index.html と src/components/layout/header.tsx のみ)
#   --primary / --ring の hex 値 -> --primary (src/index.css のみ)
#
# ディレクトリマッピング:
#   example/root/gitignore        -> <dest>/.gitignore
#   example/root/*                -> <dest>/*
#   example/web/env.local.example -> <dest>/apps/web/.env.local.example
#   example/web/*                 -> <dest>/apps/web/*
#   example/config/*              -> <dest>/apps/web/*
#   example/src/**                -> <dest>/apps/web/src/**
#   (追加生成) apps/web/.env.local     <- env.local.example のコピー (Supabase あり時のみ)
#   (追加生成) apps/web/src/components/ui/*  <- pnpm dlx shadcn@latest add (Step 5)
#
# 出力: ステップログ + 配置ファイル一覧 + 末尾 1 行 JSON
#   {"ok":true,"dest":"...","files":N,"install":"done|skipped","shadcn":"done|skipped","supabase":"enabled|manual-removal-required"}

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SKILL_DIR=$(dirname "$SCRIPT_DIR")
EXAMPLE_DIR="$SKILL_DIR/example"

SHADCN_COMPONENTS="button card input dialog table badge select label sonner"

usage() {
    sed -n '/^# scaffold\.sh/,/^#   {"ok"/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
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

step() {
    echo ""
    echo "== Step $1: $2"
}

# ---------------------------------------------------------------- 引数パース
NAME="" APP_NAME="" PRIMARY=""
DEST="" NO_SUPABASE=false SKIP_INSTALL=false DRY_RUN=false FORCE=false

need_value() {
    [ $# -ge 2 ] || die "オプション $1 に値がない" \
        "$1 は値を取るオプション" "例: $1 <value> の形で渡す"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --name)         need_value "$@"; NAME=$2; shift 2 ;;
        --app-name)     need_value "$@"; APP_NAME=$2; shift 2 ;;
        --primary)      need_value "$@"; PRIMARY=$2; shift 2 ;;
        --dest)         need_value "$@"; DEST=$2; shift 2 ;;
        --no-supabase)  NO_SUPABASE=true; shift ;;
        --skip-install) SKIP_INSTALL=true; shift ;;
        --dry-run)      DRY_RUN=true; shift ;;
        --force)        FORCE=true; shift ;;
        -h|--help)      usage; exit 0 ;;
        *) die "不明なオプション: $1" "このオプションは定義されていない" "scaffold.sh --help で使い方を確認する" ;;
    esac
done

# ---------------------------------------------------------------- preflight
[ -n "$NAME" ]     || die "--name がない" "プロジェクト名 (kebab-case) は必須" "--name my-app のように渡す"
[ -n "$APP_NAME" ] || die "--app-name がない" "UI に表示するアプリ名は必須" "--app-name 'マイアプリ' のように渡す"

echo "$NAME" | grep -Eq '^[a-z][a-z0-9]*(-[a-z0-9]+)*$' \
    || die "--name '$NAME' が kebab-case でない" \
        "root package.json の name とディレクトリ名に使う" \
        "小文字英数字とハイフンのみの名前にする (例: my-app)"
case "$APP_NAME" in
    *'"'*|*'\'*)
        die "--app-name に \" または \\ が含まれている" \
            "header.tsx の TypeScript 文字列リテラルへそのまま埋め込むため壊れる" \
            "\" と \\ を含まない表示名にする" ;;
esac
if [ -n "$PRIMARY" ]; then
    echo "$PRIMARY" | grep -Eq '^#[0-9A-Fa-f]{6}$' \
        || die "--primary '$PRIMARY' が #RRGGBB 形式でない" \
            "index.css の --primary / --ring の hex 値を単純置換する" \
            "6 桁 hex で渡す (例: --primary '#8F5A3C')"
fi

for d in root web config src; do
    [ -d "$EXAMPLE_DIR/$d" ] || die "example/$d/ が見つからない: $EXAMPLE_DIR/$d" \
        "skill のコピー元一式が無いと何も生成できない" \
        "skill を丸ごと (example/ を含めて) 取得しているか確認する。prompt 経由なら sparse clone で skills/react-vite-supabase-starter 全体を取得する"
done

command -v perl >/dev/null 2>&1 || die "perl が見つからない" \
    "プレースホルダー置換に perl を使う (macOS / Linux 両対応のため)" "perl をインストールする"
command -v node >/dev/null 2>&1 || die "node が見つからない" \
    "Vite / TypeScript のビルドと pnpm の実行に必要" "Node.js 20 以上をインストールする (https://nodejs.org)"
NODE_MAJOR=$(node --version | perl -ne 'print $1 if /^v(\d+)/')
[ "${NODE_MAJOR:-0}" -ge 20 ] || die "node のバージョンが古い: $(node --version)" \
    "Vite / Tailwind v4 は Node.js 20 以上を要求する" "Node.js 20 以上へ更新する"
command -v pnpm >/dev/null 2>&1 || die "pnpm が見つからない" \
    "依存インストール (Step 4) と shadcn/ui 生成 (Step 5) に必要" \
    "corepack enable pnpm または https://pnpm.io/installation でインストールする"
PNPM_MAJOR=$(pnpm --version | perl -ne 'print $1 if /^(\d+)/')
[ "${PNPM_MAJOR:-0}" -ge 9 ] || die "pnpm のバージョンが古い: $(pnpm --version)" \
    "workspace / dlx の挙動を pnpm 9 以上で確認している" "pnpm 9 以上へ更新する"

[ -n "$DEST" ] || DEST="./$NAME"

# ---------------------------------------------------------------- 配置計画
# mode: copy | name (<project-name> 置換) | app (<app-name> 置換) | css (--primary 置換)
PLAN_SRC=()
PLAN_DST=()
PLAN_MODE=()

add_plan() {
    PLAN_SRC+=("$1")
    PLAN_DST+=("$2")
    PLAN_MODE+=("$3")
}

add_plan "$EXAMPLE_DIR/root/package.json"       "package.json"                 name
add_plan "$EXAMPLE_DIR/root/pnpm-workspace.yaml" "pnpm-workspace.yaml"         copy
add_plan "$EXAMPLE_DIR/root/gitignore"          ".gitignore"                   copy
add_plan "$EXAMPLE_DIR/web/package.json"        "apps/web/package.json"        copy
add_plan "$EXAMPLE_DIR/web/index.html"          "apps/web/index.html"          app
add_plan "$EXAMPLE_DIR/web/env.local.example"   "apps/web/.env.local.example"  copy

while IFS= read -r src; do
    add_plan "$src" "apps/web/$(basename "$src")" copy
done < <(find "$EXAMPLE_DIR/config" -maxdepth 1 -type f ! -name '.DS_Store' | LC_ALL=C sort)

while IFS= read -r src; do
    rel=${src#"$EXAMPLE_DIR"/src/}
    mode=copy
    case "$rel" in
        components/layout/header.tsx) mode=app ;;
        index.css)                    mode=css ;;
    esac
    add_plan "$src" "apps/web/src/$rel" "$mode"
done < <(find "$EXAMPLE_DIR/src" -type f ! -name '.DS_Store' | LC_ALL=C sort)

TOTAL=${#PLAN_SRC[@]}

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

# ---------------------------------------------------------------- 置換
export K_NAME=$NAME K_APP_NAME=$APP_NAME K_PRIMARY=$PRIMARY

transform() {
    # $1: mode
    case "$1" in
        name) perl -0777 -pe 's/\Q<project-name>\E/$ENV{K_NAME}/g' ;;
        app)  perl -0777 -pe 's/\Q<app-name>\E/$ENV{K_APP_NAME}/g' ;;
        css)
            if [ -n "$PRIMARY" ]; then
                perl -pe 's/^(\s*--(?:primary|ring):\s*)#[0-9A-Fa-f]{6}\b/$1$ENV{K_PRIMARY}/'
            else
                cat
            fi
            ;;
        copy) cat ;;
    esac
}

# ---------------------------------------------------------------- dry-run
resolve_dest() {
    if [ -d "$DEST" ]; then (cd "$DEST" && pwd); else printf '%s' "$DEST"; fi
}

SUPABASE_STATE=enabled
$NO_SUPABASE && SUPABASE_STATE=manual-removal-required
INSTALL_STATE=done
SHADCN_STATE=done
$SKIP_INSTALL && INSTALL_STATE=skipped && SHADCN_STATE=skipped

if $DRY_RUN; then
    echo "## DRY RUN: 配置予定 ($TOTAL files) — 書き込み・ネットワークアクセスは行わない"
    for i in $(seq 0 $((TOTAL - 1))); do
        marker=""
        [ -e "$DEST/${PLAN_DST[$i]}" ] && marker="  [exists]"
        echo "  ${PLAN_DST[$i]}  <=  ${PLAN_SRC[$i]#"$SKILL_DIR"/} (${PLAN_MODE[$i]})$marker"
    done
    if ! $NO_SUPABASE; then
        echo "  apps/web/.env.local  <=  example/web/env.local.example (生成)"
    fi
    echo "## 実行予定ステップ"
    echo "  Step 1: root ファイル配置 (package.json / pnpm-workspace.yaml / .gitignore)"
    echo "  Step 2: apps/web 配置 (package.json / index.html / config 8 ファイル)"
    echo "  Step 3: src 配置 + 置換 (app-name / primary)"
    if $SKIP_INSTALL; then
        echo "  Step 4: pnpm install (--skip-install によりスキップ)"
        echo "  Step 5: shadcn add (--skip-install によりスキップ)"
    else
        echo "  Step 4: pnpm install (ネットワーク必要)"
        echo "  Step 5: pnpm dlx shadcn@latest add $SHADCN_COMPONENTS (ネットワーク必要)"
    fi
    if $NO_SUPABASE; then
        echo "  Step 6: .env.local 生成 (--no-supabase によりスキップ / 手動除去手順を出力)"
    else
        echo "  Step 6: .env.local 生成"
    fi
    [ ${#conflicts[@]} -gt 0 ] && echo "NOTE: [exists] の ${#conflicts[@]} 件は実行時に --force が必要"
    echo "{\"ok\":true,\"dryRun\":true,\"dest\":\"$(resolve_dest)\",\"files\":$TOTAL,\"install\":\"$INSTALL_STATE\",\"shadcn\":\"$SHADCN_STATE\",\"supabase\":\"$SUPABASE_STATE\"}"
    exit 0
fi

# ---------------------------------------------------------------- Step 1-3: ファイル配置
mkdir -p "$DEST"
DEST_ABS=$(resolve_dest)
APP_DIR="$DEST_ABS/apps/web"

step 1 "root ファイル配置 -> $DEST_ABS"
step 2 "apps/web 配置"
step 3 "src 配置 + プレースホルダー置換"
for i in $(seq 0 $((TOTAL - 1))); do
    dst="$DEST_ABS/${PLAN_DST[$i]}"
    mkdir -p "$(dirname "$dst")"
    transform "${PLAN_MODE[$i]}" < "${PLAN_SRC[$i]}" > "$dst"
    echo "  ${PLAN_DST[$i]}"
done

# ---------------------------------------------------------------- Step 4: pnpm install
if $SKIP_INSTALL; then
    step 4 "pnpm install (スキップ)"
else
    step 4 "pnpm install"
    (cd "$DEST_ABS" && pnpm install) || die "Step 4 (pnpm install) で失敗" \
        "レジストリへ到達できないか、依存解決に失敗した (直前のログ参照)" \
        "ネットワークを確認して cd $DEST_ABS && pnpm install を再実行し、続けて Step 5 を手動実行する: cd $APP_DIR && pnpm dlx shadcn@latest add $SHADCN_COMPONENTS --yes"
fi

# ---------------------------------------------------------------- Step 5: shadcn/ui
if $SKIP_INSTALL; then
    step 5 "shadcn add (スキップ)"
else
    step 5 "shadcn/ui コンポーネント生成 ($SHADCN_COMPONENTS)"
    # components.json は配置済みなので `shadcn init` は不要。add だけで生成できる
    # shellcheck disable=SC2086
    (cd "$APP_DIR" && pnpm dlx shadcn@latest add $SHADCN_COMPONENTS --yes) \
        || die "Step 5 (shadcn add) で失敗" \
            "レジストリへ到達できないか、shadcn CLI がコンポーネント取得に失敗した (直前のログ参照)" \
            "ネットワークを確認して cd $APP_DIR && pnpm dlx shadcn@latest add $SHADCN_COMPONENTS --yes を再実行する"
fi

# ---------------------------------------------------------------- Step 6: .env.local
if $NO_SUPABASE; then
    step 6 ".env.local 生成 (スキップ: --no-supabase)"
else
    step 6 ".env.local 生成"
    if [ -e "$APP_DIR/.env.local" ]; then
        echo "  apps/web/.env.local は既存のため保持 (上書きしない)"
    else
        cp "$EXAMPLE_DIR/web/env.local.example" "$APP_DIR/.env.local"
        echo "  apps/web/.env.local (VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY を実値に書き換えること)"
    fi
fi

# ---------------------------------------------------------------- 結果出力
echo ""
echo "## 完了: $TOTAL files -> $DEST_ABS"
if $SKIP_INSTALL; then
    echo "ACTION_REQUIRED: --skip-install のため、後で次を実行する:"
    echo "  cd $DEST_ABS && pnpm install"
    echo "  cd $APP_DIR && pnpm dlx shadcn@latest add $SHADCN_COMPONENTS --yes"
fi
if $NO_SUPABASE; then
    echo "ACTION_REQUIRED: --no-supabase のため、Supabase 関連コードを手動で除去する"
    echo "  (SKILL.md の「Supabase を使わない場合」の手順に従う。概要:"
    echo "   1. 削除: src/lib/supabase.ts, src/lib/api.ts, src/auth/, src/data/, src/pages/login.tsx, .env.local.example"
    echo "   2. 編集: App.tsx (AuthProvider 除去), router.tsx (認証ガード・login ルート除去),"
    echo "      header.tsx (Sign out 除去), home.tsx (useAuth / useUserProfile 除去), vite-env.d.ts (VITE_SUPABASE_* 除去)"
    echo "   3. cd $APP_DIR && pnpm remove @supabase/supabase-js"
    echo "   4. pnpm test && pnpm build で確認)"
fi
if ! $SKIP_INSTALL && ! $NO_SUPABASE; then
    echo "次の確認: cd $DEST_ABS && pnpm test && pnpm build"
fi
echo "{\"ok\":true,\"dest\":\"$DEST_ABS\",\"files\":$TOTAL,\"install\":\"$INSTALL_STATE\",\"shadcn\":\"$SHADCN_STATE\",\"supabase\":\"$SUPABASE_STATE\"}"
