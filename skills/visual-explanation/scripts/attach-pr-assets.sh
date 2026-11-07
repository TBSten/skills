#!/usr/bin/env bash
# 画像を PR ブランチの .github/pr-assets/ に一度コミットし、次のコミットで消す。
# 本文に貼る SHA 固定 URL を Markdown で出す。最終ツリーには画像が残らない。
#
# 使い方:
#   bash attach-pr-assets.sh [--visibility=auto|public|private] [--push] <file>...
#
#   --visibility  URL の形を決める。auto は gh repo view で判定する (既定: auto)
#   --push        2 つのコミットを origin に push する (push しないと URL は表示されない)
#
# 出力: 1 行目に OK、続けて SHA、各画像の Markdown。
set -euo pipefail

fail() { echo "ERROR: $*" >&2; exit 1; }

visibility=auto
push=false
files=()
for a in "$@"; do
  case "$a" in
    --visibility=*) visibility="${a#--visibility=}" ;;
    --push) push=true ;;
    --*) fail "不明なオプション: $a (使えるのは --visibility= / --push)" ;;
    *) files+=("$a") ;;
  esac
done

[ "${#files[@]}" -gt 0 ] || fail "画像ファイルを 1 つ以上渡す。使い方: bash attach-pr-assets.sh [--visibility=auto|public|private] [--push] <file>..."
case "$visibility" in auto|public|private) ;; *) fail "--visibility は auto / public / private のどれか: $visibility" ;; esac

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "git リポジトリの中で実行する"
root="$(git rev-parse --show-toplevel)"

branch="$(git symbolic-ref --short -q HEAD || true)"
[ -n "$branch" ] || fail "detached HEAD になっている。PR ブランチを checkout してから実行する"
case "$branch" in
  main|master|develop) fail "既定ブランチ ($branch) には画像をコミットしない。PR ブランチで実行する" ;;
esac

if ! git diff --cached --quiet; then
  fail "ステージ済みの変更がある。画像のコミットに混ざるので、先にコミットするか git restore --staged で外す"
fi

seen=" "
abs_files=()
for f in "${files[@]}"; do
  [ -f "$f" ] || fail "ファイルが見つからない: $f"
  name="$(basename "$f")"
  # URL にそのまま入れるので、エンコードが要らない名前だけを受け付ける
  printf '%s' "$name" | grep -Eq '^[A-Za-z0-9._-]+$' \
    || fail "ファイル名に使えない文字がある: $name (英数字と . _ - だけにリネームしてから渡す)"
  case "$seen" in *" $name "*) fail "同じファイル名が 2 回渡された: $name (別ディレクトリの同名ファイルは上書きされるので、どちらかをリネームする)" ;; esac
  seen="$seen$name "
  abs_files+=("$(cd "$(dirname "$f")" && pwd)/$name")
done
files=("${abs_files[@]}")

remote_url="$(git remote get-url origin 2>/dev/null)" || fail "origin リモートが無い。URL の owner/repo を決められない"
slug="$(printf '%s' "$remote_url" | sed -E 's#^(git@[^:]+:|ssh://git@[^/]+/|https?://([^@/]+@)?[^/]+/)##; s#\.git$##')"
case "$slug" in */*) ;; *) fail "origin の URL から owner/repo を読み取れない: $remote_url" ;; esac

if [ "$visibility" = auto ]; then
  command -v gh >/dev/null 2>&1 || fail "gh が無いので公開 / 非公開を判定できない。--visibility=public か --visibility=private を付ける"
  is_private="$(gh repo view "$slug" --json isPrivate -q .isPrivate 2>/dev/null)" \
    || fail "gh repo view $slug に失敗した。gh auth login するか、--visibility=public / private を付ける"
  if [ "$is_private" = true ]; then visibility=private; else visibility=public; fi
fi

dir=".github/pr-assets"
cd "$root"
# 既存ファイルを上書き・削除しないよう、コピー前に衝突を調べる (追跡済みで作業ツリーから消えたものも含む)
for f in "${files[@]}"; do
  rel="$dir/$(basename "$f")"
  if [ -e "$rel" ] || [ -n "$(git ls-files -- "$rel")" ]; then
    fail "$rel が既にある。既存ファイルを消さないよう中断した。別のファイル名にリネームしてから渡す"
  fi
done

mkdir -p "$dir"
rels=()
for f in "${files[@]}"; do
  rel="$dir/$(basename "$f")"
  cp "$f" "$rel"
  rels+=("$rel")
done

git add -f -- "${rels[@]}"
if git diff --cached --quiet; then
  fail "コミットする差分が無い (同じ画像が既に .github/pr-assets/ にある)。ファイル名を変えて再実行する"
fi
git commit -q -m "chore: add PR assets (removed in next commit)" -- "${rels[@]}"
sha="$(git rev-parse HEAD)"

git rm -q -- "${rels[@]}"
git commit -q -m "chore: remove PR assets"
rmdir "$dir" 2>/dev/null || true

if [ "$push" = true ]; then
  git push -q -u origin "$branch" || fail "push に失敗した。原因を直して git push -u origin $branch を実行する (コミットは作成済み)"
fi

echo "OK"
echo "sha: $sha"
echo "visibility: $visibility"
for r in "${rels[@]}"; do
  if [ "$visibility" = public ]; then
    url="https://raw.githubusercontent.com/$slug/$sha/$r"
  else
    url="https://github.com/$slug/blob/$sha/$r?raw=true"
  fi
  echo "![$(basename "$r")]($url)"
done
if [ "$push" = false ]; then
  echo "WARN: push していない。git push -u origin $branch するまで URL は表示されない" >&2
fi
