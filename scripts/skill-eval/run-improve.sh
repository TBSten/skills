#!/usr/bin/env bash
# Improve: skill-creator の run_loop.py で SKILL.md の description を最適化する (発火テスト evals/trigger-eval.json を使う)。
# eval セットを train / held-out test に分け、description 案を何度か作り直し、test の正解数が最も良い案を選ぶ。
# --apply を付けると、選ばれた案が元の description より test で「真に良い」時だけ SKILL.md に書き込む (同点なら書かない)。
# 実際にモデルを呼ぶ (課金・使用量がかかる)。発火テストは skill-creator の流儀どおり、空のプロジェクトで行う。
#
# usage: run-improve.sh --skill NAME --skill-creator DIR --out DIR --model M
#                       [--max-iterations N (既定 3)] [--runs-per-query N (既定 2)] [--apply]
#   --skill-creator : anthropics/skills の skills/skill-creator を置いたディレクトリ (scripts/run_loop.py がある所)
# 出力: DIR/improve.json (run_loop.py の生結果) / DIR/improve-summary.json (下の形式) / DIR/improve.log
#   {"before": {"train","test"}, "after": {"train","test"}, "improved", "applied", "original_description", "best_description"}
#   train / test は "正解数/件数"
# stdout: "OK improve: <name> test <before> -> <after> applied=<true|false>"
set -euo pipefail
# shellcheck source=scripts/skill-eval/lib.sh
source "$(dirname "$0")/lib.sh"

skill="" sc="" out="" model="" max_iter=3 runs=2 apply=false
while [ $# -gt 0 ]; do
  case "$1" in
    --skill) skill="${2:?}"; shift 2 ;;
    --skill-creator) sc="${2:?}"; shift 2 ;;
    --out) out="${2:?}"; shift 2 ;;
    --model) model="${2:?}"; shift 2 ;;
    --max-iterations) max_iter="${2:?}"; shift 2 ;;
    --runs-per-query) runs="${2:?}"; shift 2 ;;
    --apply) apply=true; shift ;;
    -h | --help) sed -n '2,13p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
[ -n "$skill" ] && [ -n "$sc" ] && [ -n "$out" ] && [ -n "$model" ] ||
  die "--skill / --skill-creator / --out / --model は必須" "" "--help で usage を確認する"
require_cmd claude jq yq python3
dir="$(skill_dir "$skill")"
[ -f "$sc/scripts/run_loop.py" ] || die "$sc/scripts/run_loop.py が無い" "--skill-creator は anthropics/skills の skills/skill-creator を指す" "workflow の skill-creator checkout (SE_SKILL_CREATOR_REF) を確認する"
sc="$(cd "$sc" && pwd)"
eval_set="$dir/evals/trigger-eval.json"
[ -f "$eval_set" ] || die "skills/$skill/evals/trigger-eval.json が無い" "Improve は発火テストを使う" "先に eval を作り、check-evals.sh を通す"
mkdir -p "$out"
out="$(cd "$out" && pwd)"

# 空のプロジェクト (.claude/ だけ) で発火テストを行う。リポジトリの CLAUDE.md や他の skill に結果を左右させない。
# run_eval.py は問い合わせごとに同じ description の一時 command を同じ .claude/commands/ に置くので、並列にすると
# 他の worker の command が選ばれて「発火しなかった」と数えられる。worker は 1 に固定する
project="$(mktemp -d)"
trap 'rm -rf "$project"' EXIT
mkdir -p "$project/.claude"

if ! (cd "$project" && env -u CLAUDECODE PYTHONPATH="$sc" python3 -m scripts.run_loop \
  --eval-set "$eval_set" --skill-path "$dir" --model "$model" \
  --max-iterations "$max_iter" --runs-per-query "$runs" --num-workers 1 \
  --report none --verbose >"$out/improve.json" 2>"$out/improve.log"); then
  tail -n 20 "$out/improve.log" >&2
  die "run_loop.py が失敗した" "認証切れ・モデル名の誤り・eval セットの形式のどれか" "$out/improve.log を見る"
fi
jq -e '.history | length > 0' "$out/improve.json" >/dev/null 2>&1 ||
  die "run_loop.py の出力に history が無い" "" "$out/improve.log と $out/improve.json を見る"

jq '
  def score(p; t): if t == null or t == 0 then null else "\(p)/\(t)" end;
  (.history[0]) as $o
  | (if .test_size > 0 then (.history | max_by(.test_passed // 0)) else (.history | max_by(.train_passed)) end) as $b
  | (if .test_size > 0 then (($b.test_passed // 0) > ($o.test_passed // 0)) else ($b.train_passed > $o.train_passed) end) as $improved
  | {
      before: {train: score($o.train_passed; $o.train_total), test: score($o.test_passed; $o.test_total)},
      after: {train: score($b.train_passed; $b.train_total), test: score($b.test_passed; $b.test_total)},
      improved: $improved,
      applied: false,
      original_description: .original_description,
      best_description: (if $improved then $b.description else .original_description end)
    }' "$out/improve.json" >"$out/improve-summary.json"

if [ "$apply" = true ] && [ "$(jq -r .improved "$out/improve-summary.json")" = true ]; then
  desc="$(jq -r .best_description "$out/improve-summary.json")"
  [ "${#desc}" -le 1024 ] || die "新しい description が 1024 文字を超える (${#desc})" "skill の description の上限" "improve.json の history から別の案を選ぶか、この skill は見送る"
  DESC="$desc" yq --front-matter=process -i '.description = strenv(DESC) | .description style="folded"' "$dir/SKILL.md"
  [ "$(skill_meta "$dir" .description | tr -s '[:space:]' ' ' | sed 's/ $//')" = "$(echo "$desc" | tr -s '[:space:]' ' ' | sed 's/ $//')" ] ||
    die "SKILL.md の description を書き換えられなかった" "frontmatter の形式が想定外" "skills/$skill/SKILL.md の frontmatter を確認する"
  jq '.applied = true' "$out/improve-summary.json" >"$out/improve-summary.json.tmp" && mv "$out/improve-summary.json.tmp" "$out/improve-summary.json"
fi

jq -r --arg s "$skill" '"OK improve: \($s) test \(.before.test // .before.train) -> \(.after.test // .after.train) applied=\(.applied)"' "$out/improve-summary.json"
