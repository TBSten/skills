#!/usr/bin/env bash
# libraries.yml の各ライブラリについて、default branch の HEAD が last_commit から進んだかを調べ JSON で出す。
# archive / 削除 / rename もここで判定する。ネットワークは使うが副作用は無い (何度実行してもよい)。
#
# usage: detect-changes.sh [--libraries FILE] [--only owner/a,owner/b] [--force]
#   --libraries : 一覧ファイル (既定: .github/library-watch/libraries.yml)
#   --only      : 対象をこの repo に絞る (カンマ区切り)
#   --force     : HEAD が last_commit と同じでも changed に入れる (collect-diff.sh が直近 commit を見る)
#
# stdout (JSON):
#   { "changed":   [{repo (GitHub 上の現在の名前), config_repo (一覧上の名前), head, last_commit,
#                    default_branch, skills, paths, forced}],
#     "unchanged": [repo], "archived": [repo], "missing": [repo],
#     "renamed":   [{from, to}] }
# 認証: gh の認証 (GH_TOKEN / GITHUB_TOKEN / gh auth login) を git の credential helper にも使う。
set -euo pipefail
# shellcheck source=scripts/library-watch/lib.sh
source "$(dirname "$0")/lib.sh"

libraries="$LW_DEFAULT_LIBRARIES"
only=""
force=false
while [ $# -gt 0 ]; do
  case "$1" in
    --libraries) libraries="${2:?--libraries には値が必要}"; shift 2 ;;
    --only) only="${2-}"; shift 2 ;;
    --force) force=true; shift ;;
    -h | --help) sed -n '2,15p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
require_cmd yq jq gh git

entries="$(libraries_json "$libraries" | jq -c '.libraries // [] | .[]')"
[ -n "$entries" ] || die "$libraries に libraries[] が無い" "調べる対象が 0 件" "libraries: の下にエントリを追加する"

if [ -n "$only" ]; then
  known="$(libraries_json "$libraries" | jq -r '.libraries[].repo')"
  IFS=',' read -r -a only_list <<<"$only"
  for r in "${only_list[@]}"; do
    grep -qxF "$r" <<<"$known" || die "--only の '$r' が一覧に無い" "一覧外の repo は調べられない" "$libraries の repo と同じ綴り (owner/name) で指定する"
  done
fi

ls_remote_head() {
  git -c credential.helper= -c 'credential.helper=!gh auth git-credential' \
    ls-remote "https://github.com/$1.git" "refs/heads/$2" | awk '{print $1}'
}

result='{"changed":[],"unchanged":[],"archived":[],"missing":[],"renamed":[]}'
while IFS= read -r entry; do
  repo="$(jq -r '.repo' <<<"$entry")"
  require_repo_slug "$repo"
  if [ -n "$only" ] && ! grep -qxF "$repo" < <(tr ',' '\n' <<<"$only"); then
    continue
  fi

  if ! meta="$(gh api "repos/$repo" 2>/dev/null)"; then
    warn "$repo: GitHub から取得できない (削除 / private 化 / 権限不足)"
    result="$(jq --arg r "$repo" '.missing += [$r]' <<<"$result")"
    continue
  fi
  full_name="$(jq -r '.full_name' <<<"$meta")"
  if [ "$full_name" != "$repo" ]; then
    warn "$repo: $full_name に rename されている。一覧の repo を直すこと"
    result="$(jq --arg f "$repo" --arg t "$full_name" '.renamed += [{from:$f, to:$t}]' <<<"$result")"
  fi
  if [ "$(jq -r '.archived' <<<"$meta")" = "true" ]; then
    result="$(jq --arg r "$repo" '.archived += [$r]' <<<"$result")"
    continue
  fi

  branch="$(jq -r '.default_branch' <<<"$meta")"
  head="$(ls_remote_head "$full_name" "$branch")"
  [ -n "$head" ] || die "$full_name の $branch の HEAD が取れない" "git ls-remote が空を返した" "branch 名と認証 (private なら PAT) を確認する"

  last="$(jq -r '.last_commit // ""' <<<"$entry")"
  if [ "$head" = "$last" ] && [ "$force" != true ]; then
    result="$(jq --arg r "$repo" '.unchanged += [$r]' <<<"$result")"
    continue
  fi
  forced=false
  [ "$head" = "$last" ] && forced=true
  result="$(jq --argjson e "$entry" --arg h "$head" --arg b "$branch" --arg fn "$full_name" --argjson f "$forced" \
    '.changed += [$e + {repo:$fn, config_repo:$e.repo, head:$h, default_branch:$b, forced:$f}]' <<<"$result")"
done <<<"$entries"

jq . <<<"$result"
jq -r '"detect: changed=\(.changed|length) unchanged=\(.unchanged|length) archived=\(.archived|length) missing=\(.missing|length) renamed=\(.renamed|length)"' <<<"$result" >&2
