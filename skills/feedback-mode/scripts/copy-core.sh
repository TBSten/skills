#!/usr/bin/env bash
# copy-core.sh — feedback-mode のコア（Sink の契約・採番・redact・文の組み立て・3 つの Sink・テスト）と HTTP の受け口を導入先にコピーし、node:test が通ることを確認する。
# 使い方: bash copy-core.sh <導入先ディレクトリ> [--force]
#   導入先: dev 専用のディレクトリ（本番ビルドから除外しやすい場所。例: dev/feedback）。無ければ作る。
#   配置: <導入先>/core/*.mjs（コア・Sink・テスト）と <導入先>/web/feedback-dev-server.mjs（契約テストが HTTP の受け口を使うため。相対 import を保つ）。
#   --force: 既存のファイルを上書きする（既定は 1 つでも既存があればコピーせず中断する）。
# 終了コード: 0 = コピーしてテストが通った / 1 = テスト失敗・コピー先に既存ファイルあり / 2 = 使い方の誤り・前提（node）が無い
# 出力: 成功時は最後に OK 行（コピーしたファイル数とテスト結果）。
set -euo pipefail

SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="${SKILL_DIR}/assets/templates"

usage() { echo "usage: bash $0 <dest-dir> [--force]" >&2; exit 2; }
[ $# -ge 1 ] && [ $# -le 2 ] || usage
DEST="$1"; FORCE=0
if [ $# -eq 2 ]; then [ "$2" = "--force" ] && FORCE=1 || usage; fi

command -v node >/dev/null 2>&1 || { echo "error: node が見つかりません。Node 18 以上を入れてください（Node が無い環境は references/concepts.md の規則を移植して同等のテストを書く）" >&2; exit 2; }
[ -d "${SRC}/core" ] || { echo "error: テンプレートが見つかりません: ${SRC}/core（skill の配置が壊れています。再インストールしてください）" >&2; exit 2; }

files=()
for f in "${SRC}"/core/*.mjs; do files+=("core/$(basename "$f")"); done
files+=("web/feedback-dev-server.mjs")

if [ "$FORCE" -eq 0 ]; then
  conflicts=()
  for rel in "${files[@]}"; do [ -e "${DEST}/${rel}" ] && conflicts+=("${rel}"); done
  if [ ${#conflicts[@]} -gt 0 ]; then
    echo "error: コピー先に既存のファイルがあります: ${conflicts[*]} （上書きするなら --force。差分は diff で確認してください）" >&2
    exit 1
  fi
fi

mkdir -p "${DEST}/core" "${DEST}/web"
for rel in "${files[@]}"; do cp "${SRC}/${rel}" "${DEST}/${rel}"; done

# ディレクトリを渡す形（node --test core/）は新しい Node（v25 で確認）で「Cannot find module」になるので、ファイルを並べて渡す
if ! out=$(cd "${DEST}" && node --test core/*.test.mjs 2>&1); then
  printf '%s\n' "$out" >&2
  echo "error: node --test が失敗しました（コピー先: ${DEST}）。上の出力で失敗したテストを確認してください" >&2
  exit 1
fi
pass=$(printf '%s\n' "$out" | awk '/^ℹ pass|^# pass/ {print $NF}')
echo "OK: ${#files[@]} files copied to ${DEST}, node --test pass=${pass:-?}"
