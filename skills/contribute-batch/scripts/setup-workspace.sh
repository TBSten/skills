#!/usr/bin/env bash
# contribute-batch: ワークスペース準備 script
#
# TBSten/skills (または指定リポジトリ) を clone し (clone 済みなら再利用)、
# 作業 branch `feat/contribute-batch-<slug>` を作成 (既存なら checkout) する。
# 成功時は stdout の末尾に 1 行 JSON を出力する:
#   {"ok":true,"cloneDir":"...","branch":"..."}
# 失敗時は stderr に「何が・なぜ・どう直すか」を出力し、非 0 で終了する。
#
# Usage:
#   setup-workspace.sh --slug <slug> [--repo <owner/repo|URL|local-path>] [--base-dir <dir>] [--dry-run]
#
# Options:
#   --slug <slug>      (必須) batch slug。kebab-case ([a-z0-9-])。branch 名と clone 先ディレクトリ名に使う
#   --repo <repo>      clone 元。owner/repo 形式 / URL / ローカル絶対パス (テスト用)。デフォルト: TBSten/skills
#   --base-dir <dir>   clone 先の親ディレクトリ。デフォルト: /tmp
#   --dry-run          実行計画の表示のみ。clone・branch 作成・ネットワークアクセスを行わない
#
# Exit codes: 0=成功 / 2=引数エラー / 10=preflight 失敗 / 20=clone 失敗 / 21=clone 先ディレクトリ衝突 / 30=branch 作成失敗
set -euo pipefail

DEFAULT_REPO="TBSten/skills"
DEFAULT_BASE_DIR="/tmp"

SLUG=""
REPO="$DEFAULT_REPO"
BASE_DIR="$DEFAULT_BASE_DIR"
DRY_RUN=0

log() { printf '[setup-workspace] %s\n' "$*" >&2; }

# die <exit-code> <何が> <なぜ> <どう直すか>
die() {
  local code="$1" what="$2" why="$3" fix="$4"
  {
    printf '[setup-workspace] ERROR: %s\n' "$what"
    printf '[setup-workspace]   原因: %s\n' "$why"
    printf '[setup-workspace]   対処: %s\n' "$fix"
  } >&2
  exit "$code"
}

usage() {
  sed -n '2,20p' "$0" >&2
}

json_escape() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'; }

# --- 引数パース ---
while [ $# -gt 0 ]; do
  case "$1" in
    --slug)
      [ $# -ge 2 ] || { usage; die 2 "--slug に値がありません" "引数不足" "--slug <slug> の形式で指定してください"; }
      SLUG="$2"; shift 2 ;;
    --repo)
      [ $# -ge 2 ] || { usage; die 2 "--repo に値がありません" "引数不足" "--repo <owner/repo> の形式で指定してください"; }
      REPO="$2"; shift 2 ;;
    --base-dir)
      [ $# -ge 2 ] || { usage; die 2 "--base-dir に値がありません" "引数不足" "--base-dir <dir> の形式で指定してください"; }
      BASE_DIR="$2"; shift 2 ;;
    --dry-run)
      DRY_RUN=1; shift ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      usage; die 2 "不明な引数: $1" "サポートされていないオプション" "上記 Usage のオプションだけを使ってください" ;;
  esac
done

[ -n "$SLUG" ] || { usage; die 2 "--slug が指定されていません" "必須引数の不足" "--slug <slug> (kebab-case) を指定してください"; }

case "$SLUG" in
  *[!a-z0-9-]*|-*|*-)
    die 2 "slug が不正です: $SLUG" "slug は kebab-case ([a-z0-9-]、先頭末尾は英数字) である必要があります" "例: --slug kmp-testing-knowledge" ;;
esac

# --- repo → clone URL の解決 ---
LOCAL_REPO=0
case "$REPO" in
  *://*|git@*:*) REPO_URL="$REPO" ;;
  /*)            REPO_URL="$REPO"; LOCAL_REPO=1 ;;  # ローカルパス (テスト・モック用)
  */*)           REPO_URL="https://github.com/$REPO.git" ;;
  *)
    die 2 "リポジトリ指定が不正です: $REPO" "owner/repo 形式・URL・ローカル絶対パスのいずれでもありません" "例: --repo TBSten/skills" ;;
esac

BRANCH="feat/contribute-batch-$SLUG"
CLONE_DIR="$BASE_DIR/contribute-batch-$SLUG"

# --- preflight ---
command -v git >/dev/null 2>&1 ||
  die 10 "git が見つかりません" "git がインストールされていない、または PATH に無い" "git をインストールしてください (https://git-scm.com/)"

command -v gh >/dev/null 2>&1 ||
  die 10 "gh CLI が見つかりません" "PR 作成に gh CLI が必要ですがインストールされていない" "gh をインストールしてください (https://cli.github.com/)"

if [ "$DRY_RUN" -eq 0 ] && [ "$LOCAL_REPO" -eq 0 ]; then
  gh auth status >/dev/null 2>&1 ||
    die 10 "gh CLI が未認証です" "GitHub への認証が済んでいないため clone 後の push・PR 作成ができない" "gh auth login を実行してから再実行してください"
fi

if [ "$LOCAL_REPO" -eq 1 ] && [ ! -e "$REPO_URL" ]; then
  die 10 "ローカルリポジトリが存在しません: $REPO_URL" "--repo に指定されたパスが見つからない" "パスを確認するか owner/repo 形式で指定してください"
fi

# --- dry-run ---
if [ "$DRY_RUN" -eq 1 ]; then
  log "dry-run: 以下を実行する計画です (実際には何も変更しません)"
  log "  clone 元: $REPO_URL"
  log "  clone 先: $CLONE_DIR (既存なら再利用)"
  log "  branch  : $BRANCH (既存なら checkout、無ければ作成)"
  printf '{"ok":true,"dryRun":true,"cloneDir":"%s","branch":"%s"}\n' \
    "$(json_escape "$CLONE_DIR")" "$(json_escape "$BRANCH")"
  exit 0
fi

# --- base dir の準備と絶対パス化 ---
mkdir -p "$BASE_DIR" ||
  die 20 "base dir を作成できません: $BASE_DIR" "権限不足またはパス不正" "--base-dir に書き込み可能なディレクトリを指定してください"
BASE_DIR="$(cd "$BASE_DIR" && pwd)"
CLONE_DIR="$BASE_DIR/contribute-batch-$SLUG"

# --- clone (既存なら再利用) ---
if [ -d "$CLONE_DIR/.git" ]; then
  ACTUAL_URL="$(git -C "$CLONE_DIR" remote get-url origin 2>/dev/null || true)"
  if [ "$ACTUAL_URL" != "$REPO_URL" ]; then
    die 21 "clone 先 $CLONE_DIR は別リポジトリの clone です (origin: ${ACTUAL_URL:-なし})" \
      "過去に別の用途で作られたディレクトリと slug が衝突している" \
      "別の --slug を使うか、このディレクトリを削除してから再実行してください"
  fi
  log "既存の clone を再利用します: $CLONE_DIR"
elif [ -e "$CLONE_DIR" ]; then
  die 21 "clone 先 $CLONE_DIR が既に存在しますが git リポジトリではありません" \
    "同名のファイル・ディレクトリが先に作られている" \
    "別の --slug を使うか、このパスを削除してから再実行してください"
else
  log "clone します: $REPO_URL -> $CLONE_DIR"
  git clone --depth 1 "$REPO_URL" "$CLONE_DIR" >&2 ||
    die 20 "clone に失敗しました: $REPO_URL" \
      "ネットワーク不通・リポジトリ名の誤り・アクセス権限不足のいずれか" \
      "ネットワーク接続とリポジトリ名を確認してください。private リポジトリなら gh auth login の認証スコープも確認してください"
fi

# --- 作業 branch の作成 (既存なら checkout) ---
if git -C "$CLONE_DIR" rev-parse --verify --quiet "refs/heads/$BRANCH" >/dev/null; then
  log "既存の branch を再利用します: $BRANCH"
  git -C "$CLONE_DIR" checkout --quiet "$BRANCH" ||
    die 30 "branch の checkout に失敗しました: $BRANCH" \
      "作業ツリーに競合する未コミット変更がある可能性" \
      "cd $CLONE_DIR && git status で状態を確認し、変更を退避 (git stash) してから再実行してください"
else
  log "branch を作成します: $BRANCH"
  git -C "$CLONE_DIR" checkout --quiet -b "$BRANCH" ||
    die 30 "branch の作成に失敗しました: $BRANCH" \
      "branch 名の不正または repository の状態異常" \
      "cd $CLONE_DIR && git status で状態を確認してください"
fi

log "準備完了: cloneDir=$CLONE_DIR branch=$BRANCH"
printf '{"ok":true,"cloneDir":"%s","branch":"%s"}\n' \
  "$(json_escape "$CLONE_DIR")" "$(json_escape "$BRANCH")"
