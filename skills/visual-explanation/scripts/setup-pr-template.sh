#!/usr/bin/env bash
# PR テンプレートに「1 枚目に図を置く」枠を入れる。
#
# 使い方:
#   bash setup-pr-template.sh [<repo-root>]
#
# 出力 (1 行目):
#   CREATED <path>      テンプレートが無かったので雛形を作った
#   ALREADY <path>      既に visual-explanation の枠が入っている (何もしない)
#   NEEDS_MERGE <path>  既存テンプレートがある。続く snippet を AI が既存の節に合わせて手で足す
set -euo pipefail

fail() { echo "ERROR: $*" >&2; exit 1; }

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
template="$script_dir/../assets/pull_request_template.md"
[ -f "$template" ] || fail "雛形が見つからない: $template (skill のインストールが壊れている。再インストールする)"

root="${1:-}"
if [ -z "$root" ]; then
  root="$(git rev-parse --show-toplevel 2>/dev/null)" || fail "git リポジトリの中で実行するか、引数にリポジトリのルートを渡す"
fi
[ -d "$root" ] || fail "ディレクトリが無い: $root"

existing=""
for p in .github/pull_request_template.md .github/PULL_REQUEST_TEMPLATE.md PULL_REQUEST_TEMPLATE.md pull_request_template.md docs/pull_request_template.md docs/PULL_REQUEST_TEMPLATE.md; do
  if [ -f "$root/$p" ]; then existing="$p"; break; fi
done
if [ -z "$existing" ] && [ -d "$root/.github/PULL_REQUEST_TEMPLATE" ]; then
  existing=".github/PULL_REQUEST_TEMPLATE/"
fi

if [ -z "$existing" ]; then
  mkdir -p "$root/.github"
  cp "$template" "$root/.github/pull_request_template.md"
  echo "CREATED .github/pull_request_template.md"
  exit 0
fi

if grep -rqs "visual-explanation" "$root/$existing"; then
  echo "ALREADY $existing"
  exit 0
fi

echo "NEEDS_MERGE $existing"
echo "--- snippet: 「何をしたか」に当たる節の直下に足す。その節が無ければ節ごと足す ---"
echo "<!-- visual-explanation: 1 枚目に図・スクショ・動画を置く。見た目が無い変更は実際の出力を <details> に -->"
