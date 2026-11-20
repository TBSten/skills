#!/usr/bin/env bash
# Claude Code plugin marketplace の定義 .claude-plugin/marketplace.json を生成する。
# 入力 (SSoT):
#   - scripts/marketplace/plugins.json  : marketplace 情報と、グループ → plugin の対応
#   - skills/<name>/SKILL.md frontmatter : metadata.group / metadata.status
# skill の追加・削除・group / status 変更をしたら必ず実行して、生成結果をコミットする。
#
# usage: generate.sh [--check]
#   --check : 書き込まず、現在の marketplace.json が生成結果と一致するかだけを検証する (CI 用)
# stdout: 成功時 "OK marketplace: plugins=N skills=M" (--check では "OK marketplace: up to date")
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG="$ROOT/scripts/marketplace/plugins.json"
OUT="$ROOT/.claude-plugin/marketplace.json"

die() {
  {
    echo "ERROR: $1"
    [ -n "${2:-}" ] && echo "  why: $2"
    [ -n "${3:-}" ] && echo "  fix: $3"
  } >&2
  exit 1
}

check=false
while [ $# -gt 0 ]; do
  case "$1" in
    --check) check=true; shift ;;
    -h | --help) sed -n '2,10p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done

command -v jq >/dev/null 2>&1 ||
  die "command 'jq' が見つからない" "plugins.json の読み込みと marketplace.json の生成に使う" "jq をインストールする (例: brew install jq / apt-get install jq)"
[ -f "$CONFIG" ] || die "$CONFIG が無い" "plugin の分け方の SSoT" "git で復元する"

# SKILL.md frontmatter (先頭の --- から次の --- まで) の 1 行スカラー値を出す。
#   fm_value <file> <key> <indent>  例: fm_value SKILL.md status 2 → "  status: X" の X
# metadata.group / metadata.status / name はどれも 1 行で書く規約 (contribute-skill.md) なので awk で足りる
fm_value() {
  awk -v key="$2" -v ind="$3" '
    NR == 1 { if ($0 != "---") exit; next }
    $0 == "---" { exit }
    {
      pre = sprintf("%" ind "s", "")
      if (index($0, pre key ":") == 1 && substr($0, ind + 1, 1) != " ") {
        v = substr($0, ind + length(key) + 2)
        sub(/^[ \t]+/, "", v); sub(/[ \t]+$/, "", v)
        gsub(/^["\x27]|["\x27]$/, "", v)
        print v; exit
      }
    }' "$1"
}

valid_status=" WIP Experimental Active Active-Prime Archived "

# skill 一覧を {name, group, status} の JSON 配列にする
skills_json="[]"
for f in "$ROOT"/skills/*/SKILL.md; do
  dir="$(basename "$(dirname "$f")")"
  name="$(fm_value "$f" name 0)"
  group="$(fm_value "$f" group 2)"
  status="$(fm_value "$f" status 2)"
  [ "$name" = "$dir" ] ||
    die "skills/$dir/SKILL.md の name '$name' がディレクトリ名と違う" "plugin の skill 名はディレクトリ名から決まる" "frontmatter の name を '$dir' にする"
  [[ "$valid_status" == *" $status "* ]] ||
    die "skills/$dir/SKILL.md の metadata.status '$status' が不正" "WIP / Experimental / Active / Active-Prime / Archived のいずれか" "frontmatter の metadata.status を直す"
  if [ -z "$group" ] && [[ "$dir" != contribute-* ]]; then
    die "skills/$dir/SKILL.md に metadata.group が無い" "group が無くてよいのは contribute-* だけ (CLAUDE.md の Group 節)" "CLAUDE.md の Group 表から選んで metadata.group に書く"
  fi
  skills_json="$(jq -c --arg n "$dir" --arg g "$group" --arg s "$status" '. + [{name: $n, group: $g, status: $s}]' <<<"$skills_json")"
done

config_json="$(jq -c . "$CONFIG")" || die "$CONFIG が JSON として読めない" "" "JSON の構文を直す"

# どの plugin にも対応しないグループを検出する
missing="$(jq -r --argjson c "$config_json" '
  [.[] | select(.status != "Archived") | .group] | unique
  | map(select(. as $g | ($c.plugins | map(.group // "") | index($g)) == null)) | .[]' <<<"$skills_json")"
[ -z "$missing" ] ||
  die "plugin が定義されていないグループがある: $(echo "$missing" | paste -sd, -)" \
    "全グループを 1 つずつ plugin に割り当てる設計" \
    "scripts/marketplace/plugins.json の plugins に { group, name, category, description } を追加する"

generated="$(jq -n --argjson c "$config_json" --argjson s "$skills_json" '
  {
    "$schema": "https://anthropic.com/claude-code/marketplace.schema.json",
    name: $c.marketplace.name,
    owner: $c.marketplace.owner,
    metadata: { description: $c.marketplace.description },
    plugins: [
      $c.plugins[] as $p
      | ($s | map(select(.status != "Archived" and .group == ($p.group // ""))) | map("./skills/" + .name) | sort) as $skills
      | select($skills | length > 0)
      | {
          name: $p.name,
          description: $p.description,
          source: "./",
          strict: false,
          category: $p.category,
          author: { name: $c.marketplace.owner.name },
          homepage: "https://github.com/TBSten/skills",
          skills: $skills
        }
    ]
  }')"

if $check; then
  [ -f "$OUT" ] || die ".claude-plugin/marketplace.json が無い" "生成結果がコミットされていない" "scripts/marketplace/generate.sh を実行してコミットする"
  if ! diff -u <(jq -S . "$OUT") <(jq -S . <<<"$generated") >&2; then
    die ".claude-plugin/marketplace.json が SKILL.md / plugins.json と一致しない" \
      "skill の追加・削除・group / status 変更の後に再生成していない (または手で編集した)" \
      "scripts/marketplace/generate.sh を実行して差分をコミットする"
  fi
  echo "OK marketplace: up to date"
  exit 0
fi

mkdir -p "$(dirname "$OUT")"
jq . <<<"$generated" >"$OUT"
echo "OK marketplace: plugins=$(jq '.plugins | length' "$OUT") skills=$(jq '[.plugins[].skills[]] | length' "$OUT")"
