#!/usr/bin/env bash
# owner の公開 repo から「Kotlin・fork でない・archive でない・一覧 (libraries / ignored) に無い」ものを
# 一覧に足す候補として JSON で出す。採否は AI が判断する (この script は機械的な絞り込みだけ)。
# この skills リポジトリ自身 ($GITHUB_REPOSITORY、既定 TBSten/skills) も除く。
#
# usage: discover-candidates.sh [--libraries FILE] [--owner NAME] [--max N]
#   --owner : 既定 TBSten (library-watch の方針で TBSten の repo だけを候補にする)
#   --max   : 最近 push された順に最大 N 件 (既定: 15)。溢れた分は次回以降に回る
#
# stdout (JSON): [{repo, description, topics, pushed_at, stars, url}]
set -euo pipefail
# shellcheck source=scripts/library-watch/lib.sh
source "$(dirname "$0")/lib.sh"

libraries="$LW_DEFAULT_LIBRARIES" owner="TBSten" max=15 self="${GITHUB_REPOSITORY:-TBSten/skills}"
while [ $# -gt 0 ]; do
  case "$1" in
    --libraries) libraries="${2:?}"; shift 2 ;;
    --owner) owner="${2:?}"; shift 2 ;;
    --max) max="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,10p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
require_cmd yq jq gh
[[ "$max" =~ ^[0-9]+$ ]] || die "--max '$max' が整数でない" "" "例: --max 15"

known="$(libraries_json "$libraries" | jq -c --arg self "$self" '[(.libraries // [])[].repo, (.ignored // [])[].repo, $self] | map(ascii_downcase)')"

repos="$(gh api --paginate "users/$owner/repos?type=owner&per_page=100" --jq '.[]' 2>/dev/null | jq -s '.')" ||
  die "$owner の repo 一覧を取得できない" "gh api が失敗した (認証切れ / rate limit / owner 名の誤り)" "gh auth status を確認し、GH_TOKEN を渡して再実行する"

candidates="$(jq --argjson known "$known" --argjson max "$max" '
  map(select(.language == "Kotlin" and (.fork | not) and (.archived | not)
             and ((.full_name | ascii_downcase) as $n | $known | index($n) | not)))
  | sort_by(.pushed_at) | reverse | .[:$max]
  | map({repo: .full_name, description, topics, pushed_at, stars: .stargazers_count, url: .html_url})
' <<<"$repos")"

jq . <<<"$candidates"
jq -r 'length | "discover: \(.) candidates"' <<<"$candidates" >&2
