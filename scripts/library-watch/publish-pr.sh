#!/usr/bin/env bash
# update job が作った patch を prepare-branch.sh 済みの作業ブランチに commit し、固定ブランチへ force-push して
# PR を作る / 更新する (1 回の実行で PR 1 本)。patch が空なら何もしない。
#
# usage: publish-pr.sh --patch FILE --body FILE [--branch NAME] [--base NAME] [--title TEXT] [--dry-run]
#   --patch   : `git diff --binary` 形式の patch (update job の成果物)
#   --body    : PR 本文 (Markdown。AI が書いたもの)
#   --dry-run : commit まではするが push / PR 操作をしない (ローカル確認用)
# 前提: prepare-branch.sh の直後で作業ツリーがきれいなこと。GH_TOKEN に contents / pull-requests の write 権限。
set -euo pipefail
# shellcheck source=scripts/library-watch/lib.sh
source "$(dirname "$0")/lib.sh"

patch="" body="" branch="library-watch/auto" base="main" title="" dry_run=false
while [ $# -gt 0 ]; do
  case "$1" in
    --patch) patch="${2:?}"; shift 2 ;;
    --body) body="${2:?}"; shift 2 ;;
    --branch) branch="${2:?}"; shift 2 ;;
    --base) base="${2:?}"; shift 2 ;;
    --title) title="${2:?}"; shift 2 ;;
    --dry-run) dry_run=true; shift ;;
    -h | --help) sed -n '2,9p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
require_cmd git gh
[ -n "$patch" ] && [ -n "$body" ] || die "--patch と --body は必須" "" "例: publish-pr.sh --patch changes.patch --body pr-body.md"
require_file "$patch" "update job の成果物"
require_file "$body" "AI が書いた PR 本文"
[ -n "$title" ] || title="library-watch: Kotlin ライブラリの差分を skill に反映"

if [ ! -s "$patch" ]; then
  echo "OK publish: patch が空なので何もしない"
  exit 0
fi

git apply --index --whitespace=nowarn "$patch" ||
  die "patch を適用できない" "update job と pr job で出発点 (prepare-branch.sh の結果) がずれた" "workflow を再実行する (その間に main か PR ブランチが動いた可能性)"
git -c user.name="github-actions[bot]" -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
  commit --quiet -m "chore(library-watch): $(date -u +%F) の差分を skill と一覧に反映"
echo "publish: committed $(git rev-parse --short HEAD)" >&2

if [ "$dry_run" = true ]; then
  echo "OK publish (dry-run): push と PR 操作はしていない"
  exit 0
fi

git push --quiet --force origin "HEAD:refs/heads/$branch"

pr_number="$(gh pr list --head "$branch" --base "$base" --state open --json number --jq '.[0].number // empty')"
if [ -n "$pr_number" ]; then
  merged_body="$(mktemp)"
  {
    cat "$body"
    echo
    echo "<details><summary>以前の実行の記録</summary>"
    echo
    gh pr view "$pr_number" --json body --jq '.body'
    echo
    echo "</details>"
  } >"$merged_body"
  gh pr edit "$pr_number" --title "$title" --body-file "$merged_body" >/dev/null
  echo "OK publish: PR #$pr_number を更新した"
else
  url="$(gh pr create --base "$base" --head "$branch" --title "$title" --body-file "$body")" ||
    die "PR を作れない" "GITHUB_TOKEN で PR を作る権限が無い可能性" "Settings > Actions > General で 'Allow GitHub Actions to create and approve pull requests' を有効にする"
  echo "OK publish: $url を作成した"
fi
