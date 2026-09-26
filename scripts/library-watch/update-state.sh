#!/usr/bin/env bash
# libraries.yml を書き換える。yq -i を使うのでコメントと並びは保たれる。どのサブコマンドも冪等。
#
# usage: update-state.sh [--libraries FILE] <subcommand> ...
#   record --changes changes.json
#       detect-changes.sh の結果を反映する: changed[] の last_commit を head に進め、
#       rename された repo 名を直し、archived[] のエントリを一覧から外す (ignored に理由付きで移す)
#   set-commit <owner/name> <sha>
#       1 件の last_commit を書き換える
#   add <owner/name> --skills a,b --paths p1,p2 [--commit SHA]
#       候補を一覧に足す (既にあれば skills / paths を上書き)。--commit 省略時は default branch の HEAD を記録する
#   ignore <owner/name> --reason TEXT
#       候補を採用しないと記録する (次回以降の候補から外れる)。既にあれば理由を上書き
#   remove <owner/name>
#       一覧から外す (無ければ何もしない)
set -euo pipefail
# shellcheck source=scripts/library-watch/lib.sh
source "$(dirname "$0")/lib.sh"

libraries="$LW_DEFAULT_LIBRARIES"
if [ "${1-}" = "--libraries" ]; then
  libraries="${2:?--libraries には値が必要}"
  shift 2
fi
cmd="${1-}"
[ -n "$cmd" ] || { sed -n '2,17p' "$0"; exit 1; }
shift
require_cmd yq jq
require_file "$libraries" "ライブラリ一覧 (SSoT)"

has_repo() { R="$1" yq -e '.libraries[] | select(.repo == strenv(R))' "$libraries" >/dev/null 2>&1; }

set_commit() {
  local repo="$1" sha="$2"
  require_repo_slug "$repo"
  [[ "$sha" =~ ^[0-9a-f]{40}$ ]] || die "commit '$sha' が 40 桁の SHA でない" "短縮 SHA だと次回の比較が常に不一致になる" "git rev-parse で完全な SHA を渡す"
  has_repo "$repo" || die "$repo が一覧に無い" "set-commit は既存エントリだけを更新する" "先に add サブコマンドで追加する"
  R="$repo" S="$sha" yq -i '(.libraries[] | select(.repo == strenv(R)) | .last_commit) = strenv(S)' "$libraries"
}

remove_repo() {
  R="$1" yq -i 'del(.libraries[] | select(.repo == strenv(R)))' "$libraries"
}

ignore_repo() {
  local repo="$1" reason="$2"
  [ -n "$reason" ] || die "ignore には --reason が必要" "次回以降に候補から外した理由を追えるようにする" "--reason '<理由>' を付ける"
  R="$repo" W="$reason" yq -i '
    .ignored = ((.ignored // []) | map(select(.repo != strenv(R)))) + [{"repo": strenv(R), "reason": strenv(W)}]
  ' "$libraries"
}

csv_to_json() { jq -Rc 'split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))' <<<"$1"; }

case "$cmd" in
  record)
    [ "${1-}" = "--changes" ] && [ -n "${2-}" ] || die "record には --changes <file> が必要" "" "例: update-state.sh record --changes changes.json"
    changes="$2"
    require_file "$changes" "detect-changes.sh の出力"
    while IFS=$'\t' read -r config_repo repo head; do
      if [ "$config_repo" != "$repo" ]; then
        F="$config_repo" T="$repo" yq -i '(.libraries[] | select(.repo == strenv(F)) | .repo) = strenv(T)' "$libraries"
      fi
      set_commit "$repo" "$head"
    done < <(jq -r '.changed // [] | .[] | [(.config_repo // .repo), .repo, .head] | @tsv' "$changes")
    while IFS= read -r repo; do
      [ -n "$repo" ] || continue
      remove_repo "$repo"
      ignore_repo "$repo" "archived (library-watch が自動で一覧から外した)"
    done < <(jq -r '.archived // [] | .[]' "$changes")
    echo "OK record: $(jq '.changed // [] | length' "$changes") updated, $(jq '.archived // [] | length' "$changes") archived removed"
    ;;
  set-commit)
    [ $# -eq 2 ] || die "set-commit の引数は <owner/name> <sha>" "" "例: update-state.sh set-commit TBSten/cream <40桁SHA>"
    set_commit "$1" "$2"
    echo "OK set-commit $1 $2"
    ;;
  add)
    repo="${1-}"; shift || true
    require_repo_slug "$repo"
    skills="" paths="" commit=""
    while [ $# -gt 0 ]; do
      case "$1" in
        --skills) skills="${2-}"; shift 2 ;;
        --paths) paths="${2-}"; shift 2 ;;
        --commit) commit="${2-}"; shift 2 ;;
        *) die "add の未知の引数 '$1'" "" "--skills / --paths / --commit のみ" ;;
      esac
    done
    [ -n "$skills" ] && [ -n "$paths" ] || die "add には --skills と --paths が必要" "どの skill に取り込み、どのパスを見るかが決まらないと差分を集められない" "例: --skills ksp-plugin-setup --paths '*.gradle.kts,gradle/,.github/'"
    for s in ${skills//,/ }; do
      [ -d "$LW_REPO_ROOT/skills/$s" ] || warn "skills/$s が無い (新規 skill の予定なら問題ない)"
    done
    if [ -z "$commit" ]; then
      require_cmd gh git
      branch="$(gh api "repos/$repo" --jq '.default_branch')" || die "$repo の情報を取れない" "" "repo 名と認証を確認する"
      commit="$(git -c credential.helper= -c 'credential.helper=!gh auth git-credential' ls-remote "https://github.com/$repo.git" "refs/heads/$branch" | awk '{print $1}')"
    fi
    [[ "$commit" =~ ^[0-9a-f]{40}$ ]] || die "$repo の commit を決められない ('$commit')" "" "--commit で 40 桁 SHA を渡す"
    R="$repo" SK="$(csv_to_json "$skills")" P="$(csv_to_json "$paths")" C="$commit" yq -i '
      .libraries = ((.libraries // []) | map(select(.repo != strenv(R))))
        + [{"repo": strenv(R), "skills": (strenv(SK) | from_json), "paths": (strenv(P) | from_json), "last_commit": strenv(C)}]
      | .ignored = ((.ignored // []) | map(select(.repo != strenv(R))))
      | (.libraries[] | select(.repo == strenv(R)) | (.skills, .paths)) style="flow"
    ' "$libraries"
    echo "OK add $repo (last_commit=$commit)"
    ;;
  ignore)
    repo="${1-}"; shift || true
    require_repo_slug "$repo"
    [ "${1-}" = "--reason" ] || die "ignore には --reason が必要" "" "例: update-state.sh ignore TBSten/foo --reason 'サンプルアプリで再利用できる技法が無い'"
    has_repo "$repo" && die "$repo は一覧に入っている" "採用済みを ignore にすると矛盾する" "外すなら remove を先に実行する"
    ignore_repo "$repo" "${2-}"
    echo "OK ignore $repo"
    ;;
  remove)
    [ $# -eq 1 ] || die "remove の引数は <owner/name>" "" "例: update-state.sh remove TBSten/foo"
    remove_repo "$1"
    echo "OK remove $1"
    ;;
  *) die "未知のサブコマンド '$cmd'" "" "record / set-commit / add / ignore / remove のいずれか" ;;
esac
