#!/usr/bin/env bash
# detect-changes.sh が出した changed[] の各ライブラリについて、last_commit..HEAD の commit log と
# paths に絞った diff を Markdown にまとめる。diff は --max-bytes で切り、切ったことを明記する。
# clone は --work-dir に blobless bare clone でキャッシュする (再実行時は fetch だけ。冪等)。
#
# usage: collect-diff.sh --changes changes.json --out DIR [--work-dir DIR] [--max-bytes N] [--fallback-commits N]
#   --changes          : detect-changes.sh の出力 JSON
#   --out              : 出力先ディレクトリ。<owner>__<name>.md と index.json を書く (同名ファイルは上書き)
#   --work-dir         : clone のキャッシュ先 (既定: ${RUNNER_TEMP:-/tmp}/library-watch-clones)
#   --max-bytes        : 1 ライブラリあたりの diff 本文の上限 (既定: 60000)
#   --fallback-commits : last_commit が無い / 辿れない / forced の時に見る直近 commit 数 (既定: 20)
#
# stdout (JSON): [{repo, file, base, head, commits, diff_bytes, truncated, note}]
set -euo pipefail
# shellcheck source=scripts/library-watch/lib.sh
source "$(dirname "$0")/lib.sh"

changes="" out="" work_dir="${RUNNER_TEMP:-/tmp}/library-watch-clones" max_bytes=60000 fallback=20
while [ $# -gt 0 ]; do
  case "$1" in
    --changes) changes="${2:?}"; shift 2 ;;
    --out) out="${2:?}"; shift 2 ;;
    --work-dir) work_dir="${2:?}"; shift 2 ;;
    --max-bytes) max_bytes="${2:?}"; shift 2 ;;
    --fallback-commits) fallback="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,14p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
require_cmd jq git gh
[ -n "$changes" ] && [ -n "$out" ] || die "--changes と --out は必須" "" "例: collect-diff.sh --changes changes.json --out .local/library-watch/diffs"
require_file "$changes" "detect-changes.sh の出力"
[[ "$max_bytes" =~ ^[0-9]+$ && "$fallback" =~ ^[0-9]+$ ]] || die "--max-bytes / --fallback-commits は整数" "" "例: --max-bytes 60000"
mkdir -p "$out" "$work_dir"

g() { git -c credential.helper= -c 'credential.helper=!gh auth git-credential' "$@"; }
# 先頭 N 行だけ出す。head と違い入力を最後まで読むので pipefail 下でも SIGPIPE で落ちない
cap() { awk -v n="$1" 'NR <= n'; }

index='[]'
while IFS= read -r entry; do
  [ -n "$entry" ] || continue
  repo="$(jq -r '.repo' <<<"$entry")"
  require_repo_slug "$repo"
  head="$(jq -r '.head' <<<"$entry")"
  last="$(jq -r '.last_commit // ""' <<<"$entry")"
  forced="$(jq -r '.forced // false' <<<"$entry")"
  paths=()
  while IFS= read -r p; do [ -n "$p" ] && paths+=("$p"); done < <(jq -r '.paths // [] | .[]' <<<"$entry")
  [ "${#paths[@]}" -gt 0 ] || paths=(".")
  name="${repo//\//__}"
  clone="$work_dir/$name.git"

  if [ -d "$clone" ]; then
    g -C "$clone" fetch --quiet --filter=blob:none origin "+refs/heads/*:refs/heads/*" ||
      die "$repo の fetch に失敗" "ネットワークか認証の問題" "$clone を消して再実行する / private なら PAT を GH_TOKEN に渡す"
  else
    g clone --quiet --bare --filter=blob:none "https://github.com/$repo.git" "$clone" ||
      die "$repo の clone に失敗" "ネットワークか認証の問題" "private なら PAT を GH_TOKEN に渡す"
  fi
  g -C "$clone" cat-file -e "$head^{commit}" 2>/dev/null ||
    die "$repo の HEAD $head が clone に無い" "detect 後に push された / force-push された可能性" "detect-changes.sh からやり直す"

  note=""
  base="$last"
  if [ "$forced" = true ] || [ -z "$last" ] || ! g -C "$clone" cat-file -e "$last^{commit}" 2>/dev/null; then
    if [ "$forced" = true ]; then
      note="forced: 差分が無いため直近 $fallback commit を対象にした"
    elif [ -z "$last" ]; then
      note="last_commit 未設定のため直近 $fallback commit を対象にした"
    else
      note="last_commit $last が履歴に無い (force-push?) ため直近 $fallback commit を対象にした"
    fi
    base="$(g -C "$clone" rev-list --max-count="$((fallback + 1))" "$head" | tail -n 1)"
  fi

  commits="$(g -C "$clone" rev-list --count "$base..$head")"
  file="$out/$name.md"
  diff_tmp="$(mktemp)"
  g -C "$clone" diff --no-color "$base" "$head" -- "${paths[@]}" >"$diff_tmp"
  diff_bytes="$(wc -c <"$diff_tmp" | tr -d ' ')"
  truncated=false
  [ "$diff_bytes" -gt "$max_bytes" ] && truncated=true

  {
    echo "# $repo"
    echo
    echo "- range: \`$base..$head\` ($commits commits)"
    echo "- related skills: $(jq -r '.skills // [] | join(", ")' <<<"$entry")"
    echo "- watched paths: \`${paths[*]}\`"
    [ -n "$note" ] && echo "- note: $note"
    echo
    echo "## commits (新しい順、最大 200 件)"
    echo
    echo '```'
    g -C "$clone" log --no-color --no-merges --format='%h %ad %s' --date=short -n 200 "$base..$head"
    echo '```'
    echo
    echo "## watched paths の変更ファイル (最大 150 行)"
    echo
    echo '```'
    g -C "$clone" diff --no-color --stat=200 "$base" "$head" -- "${paths[@]}" | cap 150
    echo '```'
    echo
    echo "## watched paths 以外で変更されたファイル (名前のみ、最大 100 件)"
    echo
    echo '```'
    g -C "$clone" diff --no-color --name-only "$base" "$head" -- . "${paths[@]/#/:(exclude)}" | cap 100
    echo '```'
    echo
    if [ "$truncated" = true ]; then
      echo "## diff (watched paths。$diff_bytes bytes 中 先頭 $max_bytes bytes で切った。続きは clone で \`git diff $base $head -- <path>\` を見る)"
    else
      echo "## diff (watched paths)"
    fi
    echo
    echo '````diff'
    if [ "$truncated" = true ]; then
      # 行の途中 (マルチバイト文字の途中を含む) で切れないよう、最後の不完全な行を捨てる
      head -c "$max_bytes" "$diff_tmp" | sed '$d'
    else
      cat "$diff_tmp"
    fi
    echo
    echo '````'
  } >"$file"
  rm -f "$diff_tmp"

  index="$(jq --arg r "$repo" --arg f "$file" --arg b "$base" --arg h "$head" --argjson c "$commits" \
    --argjson d "$diff_bytes" --argjson t "$truncated" --arg n "$note" \
    '. += [{repo:$r, file:$f, base:$b, head:$h, commits:$c, diff_bytes:$d, truncated:$t, note:$n}]' <<<"$index")"
  echo "collect: $repo $commits commits, diff ${diff_bytes}B$([ "$truncated" = true ] && echo ' (truncated)')" >&2
done < <(jq -c '.changed // [] | .[]' "$changes")

jq . <<<"$index" | tee "$out/index.json"
