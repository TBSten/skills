# skill-eval

このリポジトリの skill を約 2 週に 1 回、数件ずつ評価して改善する GitHub Actions。skill-creator の 4 モード (Create / Eval / Improve / Benchmark) のうち、**Improve と Benchmark を定期実行**する。結果は PR 1 本にまとまって届く。

- **Benchmark**: `claude plugin eval` (Claude Code CLI 組み込み) で、skill の `evals/<case>/` を skill あり / なしの 2 本立てで実行し、pass rate・時間・コストを測る。推移は `history/<skill>.jsonl` に残り、モデル更新による劣化や「skill なしでも同じ結果 = skill が不要になった」を見つけられる
- **Improve**: skill-creator の `run_loop.py` で、発火テスト `evals/trigger-eval.json` に対して description を作り直し、held-out test で **真に良くなった時だけ** SKILL.md の description を書き換える
- **eval が無い skill**: 最初に AI がテストケースと発火テストを作り、同じ実行で Benchmark / Improve まで回す。作った eval は PR でレビューする

## 仕組み

```
schedule (毎週木曜) ─ week-gate --parity odd: ISO 週番号が偶数ならここで終了 (library-watch は偶数週)
        │
select   (AI なし)  prepare-branch → select-skills: Archived 以外を最後に評価した日が古い順に 3 件
        │
evaluate            skill-creator を取得 (anthropics/skills の固定 commit)
        │           eval が無い skill だけ claude-code-action (prompt.md) が eval を作る
        │           skill ごとに check-evals → run-benchmark → run-improve --apply → record-history
        │           build-pr-body。push 権限を持たない。結果は artifact で渡す
pr                  prepare-branch → publish-pr: 固定ブランチ skill-eval/auto に force-push し PR を作成 / 更新
```

- **1 回の実行で PR 1 本**。ブランチは `skill-eval/auto` 固定。前回の PR が open のままなら続きから作業し、同じ PR を更新する (前回の本文は `<details>` に残る)。作業ブランチの用意と PR の作成は library-watch の script (`scripts/library-watch/prepare-branch.sh` / `publish-pr.sh`) を使う
- 評価日は history の最終行で決まるので、PR をマージした時点で「そこまで評価した」ことになる。失敗した skill も `error` 付きで記録し、次回は後回しになる
- 対象は 24 skill ほどなので、既定 (3 件 / 2 週) で一巡に約 4 か月かかる。早めたい時は手動実行で `max` を上げる

| ファイル | 役割 |
|---|---|
| `history/<skill>.jsonl` | SSoT。1 行 = 1 回の評価結果 (日付・モデル・Benchmark の要約・Improve の前後・エラー) |
| `prompt.md` | eval を作る AI への指示 (何をテストするかの判断基準) |
| `case-format.md` | eval の形式 (テストケースと発火テスト)。check-evals.sh が検証する内容の説明 |
| `../workflows/skill-eval.yml` | workflow |
| `../../scripts/skill-eval/*.sh` | 決定的な処理 (下表)。AI は eval の中身を決めるだけ |
| `../../skills/<skill>/evals/` | 各 skill の eval。`<case>/` (prompt.md・case.yaml・graders/) と `trigger-eval.json`。形式は `case-format.md` |

| script | 役割 |
|---|---|
| `select-skills.sh [--max N] [--only a,b]` | 今回評価する skill を JSON で出す |
| `check-evals.sh --skill NAME` | eval の形式を検証する (case.yaml の schema_version・grader の frontmatter・発火テストの件数など) |
| `run-benchmark.sh --skill NAME --out DIR [--model] [--judge-model] [--runs]` | `claude plugin eval` を実行し `benchmark-summary.json` にまとめる |
| `run-improve.sh --skill NAME --skill-creator DIR --out DIR --model M [--apply]` | `run_loop.py` を空のプロジェクトで実行し、`--apply` なら改善した description を書き込む |
| `record-history.sh --skill NAME --out DIR --model M [--error MSG]` | `history/<skill>.jsonl` に 1 行追記する |
| `build-pr-body.sh --results DIR --selected FILE` | 結果表・description の前後・新規 eval・失敗をまとめた PR 本文を出す |

各 script は `--help` で usage を表示する。依存: `bash` / `jq` / `yq` (mikefarah/yq v4) / `python3` / `claude` (Claude Code CLI v2.1.269 以降)。

## 必要な設定

library-watch と同じ。

1. **secret `CLAUDE_CODE_OAUTH_TOKEN`**: `claude setup-token` で発行したトークン (library-watch 用に登録済みならそのまま使われる)
2. **Actions に PR 作成を許可**: Settings > Actions > General > Workflow permissions で "Allow GitHub Actions to create and approve pull requests"
3. (任意) repository variable `SKILL_EVAL_MODEL` (既定 `claude-opus-5-5`) と `SKILL_EVAL_JUDGE_MODEL` (既定 `claude-sonnet-5-5`) でモデルを替えられる

## 手動実行

Actions タブ > skill-eval > Run workflow。または:

```sh
gh workflow run skill-eval.yml                                  # 評価日が古い順に 3 件 (week-gate は schedule 以外では効かない)
gh workflow run skill-eval.yml -f only=pr-fix-loop,kotlin-tuple # skill を指定
gh workflow run skill-eval.yml -f max=6 -f skip_improve=true    # 6 件、Benchmark だけ
```

ローカルで 1 skill だけ試す例 (実際にモデルを呼ぶ。skill-creator は `git clone --depth 1 https://github.com/anthropics/skills` などで用意する):

```sh
scripts/skill-eval/check-evals.sh --skill kotlin-tuple
scripts/skill-eval/run-benchmark.sh --skill kotlin-tuple --out .local/skill-eval/kotlin-tuple --runs 1
scripts/skill-eval/run-improve.sh --skill kotlin-tuple --skill-creator <skills の clone>/skills/skill-creator \
  --out .local/skill-eval/kotlin-tuple --model claude-sonnet-5-5 --max-iterations 2   # --apply を付けると書き換える
```

## eval を手で直す

- `skills/<skill>/evals/` は普通のファイルなので、PR 上でも main でも手で直してよい。直したら `check-evals.sh` を通す
- 易しすぎる case (skill あり / なしの両方で 100%) は Δ が出ないので、skill の規約を知らないと満たせない grader を足すか、case を難しくする
- 発火テストは true / false 各 8 件以上。false は「キーワードは被るが別の作業」の near-miss にする
- `gh skill install` は skill のディレクトリを丸ごとコピーするので、`evals/` も利用者の手元に届く (実行には影響しない)

## コストの上限

- 対象は 1 回 3 skill (`max`)。eval がある skill には AI の作成ステップは走らない
- Benchmark: case ごとに 2 run × 2 本立て (`--runs 2`)。各 run は case の `max_turns` / `timeout_seconds` で止まる
- Improve: 発火テスト 20 件 × 2 回 × (元の description + 最大 3 案)。1 skill 30 分前後。各問い合わせは 30 秒で打ち切られる。並列にすると worker 同士の一時 command が混ざって判定が壊れるので、直列 (1 worker) で回す
- evaluate job は `timeout-minutes: 300`、eval 作成の claude は `--max-turns 120`
