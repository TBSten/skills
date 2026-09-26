#!/usr/bin/env bash
# library-watch の作業ブランチを用意する。skills リポジトリの checkout (fetch-depth: 0) の中で実行する。
#   - 固定ブランチ (既定 library-watch/auto) に open な PR があれば、そのブランチを base (既定 main) に rebase して続きから作業する
#     (前回の skill 更新と last_commit の記録を引き継ぐ)。rebase が衝突したら捨てて base から作り直す
#   - open な PR が無ければ base から作る
# 同じリモート状態なら何度実行しても同じ tree になる (detect / update / pr の各 job で同じ出発点を再現する)。
#
# usage: prepare-branch.sh [--branch NAME] [--base NAME]
# stdout: mode=continue|fresh|fresh-after-conflict と pr_number=<番号 or 空> (GITHUB_OUTPUT 形式)
set -euo pipefail
# shellcheck source=scripts/library-watch/lib.sh
source "$(dirname "$0")/lib.sh"

branch="library-watch/auto" base="main"
while [ $# -gt 0 ]; do
  case "$1" in
    --branch) branch="${2:?}"; shift 2 ;;
    --base) base="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,10p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
require_cmd git gh
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "git の作業ツリーの外で実行された" "" "skills リポジトリの checkout の中で実行する"
[ -z "$(git status --porcelain --untracked-files=no)" ] || die "作業ツリーに未コミットの変更がある" "checkout -B で変更を失わないよう止めた" "commit か退避してから実行する"

git fetch --quiet origin "+refs/heads/$base:refs/remotes/origin/$base" ||
  die "origin/$base を fetch できない" "" "remote 名と権限を確認する"
has_remote_branch=false
if git fetch --quiet origin "+refs/heads/$branch:refs/remotes/origin/$branch" 2>/dev/null; then
  has_remote_branch=true
fi

pr_number="$(gh pr list --head "$branch" --base "$base" --state open --json number --jq '.[0].number // empty')" ||
  die "open な PR を調べられない" "gh pr list が失敗した" "GH_TOKEN に pull-requests: read 以上の権限を与える"

mode="fresh"
if [ -n "$pr_number" ] && [ "$has_remote_branch" = true ]; then
  git checkout --quiet -B library-watch-work "origin/$branch"
  if git -c user.name=library-watch -c user.email=library-watch@users.noreply.github.com \
    rebase --quiet "origin/$base" >/dev/null 2>&1; then
    mode="continue"
  else
    git rebase --abort >/dev/null 2>&1 || true
    warn "PR #$pr_number のブランチが $base と衝突したため、$base から作り直す (前回分は再調査される)"
    mode="fresh-after-conflict"
  fi
fi
if [ "$mode" != continue ]; then
  git checkout --quiet -B library-watch-work "origin/$base"
fi

echo "prepare-branch: mode=$mode pr=${pr_number:-none} head=$(git rev-parse --short HEAD)" >&2
echo "mode=$mode"
echo "pr_number=$pr_number"
