#!/usr/bin/env bash
# 人間が読むドキュメント (README.md / README.ja.md と skills|rules|prompts/<name>.md / .ja.md) の機械検査をまとめて回す。
# 書き方の決まりは .claude/rules/docs-writing.md。CI (.github/workflows/docs-check.yml) も同じものを回す。
#
# usage: scripts/docs-check/check.sh [--fix] [--base <git-ref>] [--no-textlint]
#   --fix          textlint の prh (言い換え辞書) で直せるものを自動で直す
#   --base <ref>   <ref>...HEAD で日英の片方だけが変わった組も検出する (PR 用。例: --base origin/main)
#   --no-textlint  Node.js が無い環境用。textlint (日本語の文章の検査) を飛ばす
#
# 検査するもの:
#   1. structure  日英の組がそろっている・言語の切り替えリンク・見出しとコードブロックの構成が一致・相対リンクが切れていない・英語版に日本語が残っていない
#   2. readme     README の一覧表が SSoT (SKILL.md の metadata / 詳細ドキュメントの frontmatter) と一致・説明は 80 文字以内
#   3. pairs      (--base のとき) 日本語版だけ / 英語版だけを変えていない
#   4. textlint   日本語版の言い換え辞書 (prh.yml)・である調の統一・1 文 150 文字まで
# stdout: すべて通れば最後に "OK docs-check"
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"

die() {
  {
    echo "ERROR: $1"
    [ -n "${2:-}" ] && echo "  why: $2"
    [ -n "${3:-}" ] && echo "  fix: $3"
  } >&2
  exit 2
}

fix=false
base=""
textlint=true
while [ $# -gt 0 ]; do
  case "$1" in
    --fix) fix=true; shift ;;
    --base)
      [ $# -ge 2 ] || die "--base に git の ref が無い" "" "例: --base origin/main"
      base="$2"; shift 2 ;;
    --no-textlint) textlint=false; shift ;;
    -h | --help) sed -n '2,15p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done

command -v python3 >/dev/null 2>&1 || die "python3 が見つからない" "structure / readme の検査に使う" "python3 をインストールする"

failed=0
run() {
  echo "== $1"
  shift
  if ! "$@"; then failed=1; fi
}

run "structure" python3 "$HERE/docs_check.py" structure
run "readme" python3 "$HERE/docs_check.py" readme
if [ -n "$base" ]; then
  git -C "$ROOT" rev-parse --verify --quiet "$base" >/dev/null ||
    die "--base '$base' が git で解決できない" "日英の片方だけの変更を比べる起点" "git fetch origin <branch> してから渡す (CI は fetch-depth: 0)"
  run "pairs-changed" python3 "$HERE/docs_check.py" pairs-changed "$base"
fi

if $textlint; then
  command -v npm >/dev/null 2>&1 ||
    die "npm が見つからない" "日本語の文章の検査 (textlint) に使う" "Node.js 22 以上を入れる。入れられない環境なら --no-textlint で飛ばす (CI では必ず回る)"
  if [ ! -x "$HERE/node_modules/.bin/textlint" ]; then
    echo "== textlint をインストールする (scripts/docs-check/node_modules)"
    npm ci --prefix "$HERE" --silent --no-audit --no-fund
  fi
  mapfile -t ja_files < <(python3 "$HERE/docs_check.py" list-ja)
  args=(-c "$HERE/.textlintrc.json")
  $fix && args+=(--fix)
  echo "== textlint (prh.yml の言い換え・である調・1 文 150 文字まで)"
  if (cd "$ROOT" && "$HERE/node_modules/.bin/textlint" "${args[@]}" "${ja_files[@]}"); then
    echo "OK textlint"
  else
    failed=1
    $fix || echo "hint: 言い換え辞書の指摘は --fix で直せる。である調の指摘は手で直す (.claude/rules/docs-writing.md)" >&2
  fi
fi

if [ "$failed" -ne 0 ]; then
  echo "NG docs-check: 上の ERROR を直す (書き方の決まりは .claude/rules/docs-writing.md)" >&2
  exit 1
fi
echo "OK docs-check"
