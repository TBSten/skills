# library-watch

TBSten の Kotlin ライブラリを約 2 週に 1 回調べ、別プロジェクトでも使える技法 (ビルド設定・CI・publish・テスト基盤・`.claude/` の書き方など) が増えていれば、関連する skill を更新する PR を作る GitHub Actions。調べるライブラリの一覧も自動で更新する。

## 仕組み

```
schedule (毎週月曜) ─ week-gate: ISO 週番号が奇数ならここで終了
        │
detect  (AI なし)   prepare-branch → detect-changes → discover-candidates
        │           差分・候補・archive がどれも無ければここで終了 (AI を呼ばない)
update  (AI)        collect-diff → claude-code-action (prompt.md) → update-state record → patch
        │           push 権限を持たない。結果は artifact で渡す
pr                  prepare-branch → publish-pr: 固定ブランチ library-watch/auto に force-push し PR を作成 / 更新
```

- **1 回の実行で PR 1 本**。ブランチは `library-watch/auto` 固定。前回の PR が open のままなら、そのブランチを main に rebase して続きから作業し、同じ PR を更新する (前回の本文は `<details>` に残る)。rebase が衝突した時は main から作り直す
- 前回から HEAD が動いていないライブラリはスキップする。候補も無ければ AI は呼ばれない
- 各ライブラリの `last_commit` の更新は、skill の更新と同じ PR に入る。PR をマージした時点で「そこまで調べた」ことになる
- archive されたライブラリは一覧から外し、`ignored` に理由付きで移す。rename は自動で名前を直す
- 候補は TBSten の公開 repo のうち Kotlin・fork でない・archive でない・一覧に無いもの (最近 push された順に最大 15 件)。採否は AI が決め、採用は `libraries`、見送りは `ignored` に記録する

| ファイル | 役割 |
|---|---|
| `libraries.yml` | SSoT。調べるライブラリ (`repo` / `skills` / `paths` / `last_commit`) と、見送った候補 (`ignored`) |
| `prompt.md` | AI への指示 (判断の基準・skill 更新の規約・PR 本文の形式) |
| `../workflows/library-watch.yml` | workflow |
| `../../scripts/library-watch/*.sh` | 決定的な処理 (下表)。AI は判断だけを行う |

| script | 役割 |
|---|---|
| `week-gate.sh <event> [date]` | schedule の時だけ ISO 週番号で間引く |
| `prepare-branch.sh [--branch] [--base]` | 作業ブランチを用意 (open な PR があれば続きから) |
| `detect-changes.sh [--only a,b] [--force]` | `git ls-remote` で HEAD を比べ、changed / unchanged / archived / missing / renamed を JSON で出す |
| `collect-diff.sh --changes F --out D [--max-bytes N]` | `last_commit..HEAD` の log と `paths` に絞った diff を Markdown にまとめる (上限を超えたら切って明記) |
| `discover-candidates.sh [--max N]` | 一覧に無い TBSten の Kotlin repo を候補として出す |
| `update-state.sh record / set-commit / add / ignore / remove` | `libraries.yml` をコメントを保ったまま書き換える |
| `publish-pr.sh --patch F --body F [--dry-run]` | patch を commit して force-push し、PR を作成 / 更新する |

各 script は `--help` で usage を表示する。依存: `bash` / `git` / `gh` / `jq` / `yq` (mikefarah/yq v4。GitHub の ubuntu runner には入っている)。

## 必要な設定

1. **secret `CLAUDE_CODE_OAUTH_TOKEN`**: `claude setup-token` で発行したトークンを Settings > Secrets and variables > Actions に登録する (API キーを使う場合は workflow の `claude_code_oauth_token` を `anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}` に替える)
2. **Actions に PR 作成を許可**: Settings > Actions > General > Workflow permissions で "Allow GitHub Actions to create and approve pull requests" を有効にする
3. (任意) private なライブラリを調べる場合は、対象 repo を読める PAT を secret に登録し、workflow の `GH_TOKEN` をそれに替える。既定の `GITHUB_TOKEN` では公開 repo しか見えない

補足: `GITHUB_TOKEN` で作った PR や push では他の workflow が起動しない (GitHub の仕様)。このリポジトリには PR で走る CI が無いので問題ないが、追加した場合は PAT か GitHub App のトークンに替える。

## 手動実行

Actions タブ > library-watch > Run workflow。または:

```sh
gh workflow run library-watch.yml                                   # 全件 (week-gate は schedule 以外では効かない)
gh workflow run library-watch.yml -f only=TBSten/cream,TBSten/katachi
gh workflow run library-watch.yml -f force=true -f only=TBSten/cream  # 差分が無くても直近 20 commit を調べ直す
```

ローカルで script だけ試す例 (ネットワークを使う。一覧は scratch にコピーして使う):

```sh
cp .github/library-watch/libraries.yml /tmp/lw.yml
scripts/library-watch/update-state.sh --libraries /tmp/lw.yml set-commit TBSten/cream <古い 40 桁 SHA>
scripts/library-watch/detect-changes.sh --libraries /tmp/lw.yml > /tmp/changes.json
scripts/library-watch/collect-diff.sh --changes /tmp/changes.json --out /tmp/diffs --work-dir /tmp/clones
scripts/library-watch/discover-candidates.sh --libraries /tmp/lw.yml --max 5
```

## 一覧の編集

`libraries.yml` は手で編集してよい (script 経由ならコメントを保ったまま書き換えられる)。

- **追加**: `scripts/library-watch/update-state.sh add TBSten/foo --skills ksp-plugin-setup --paths '*.gradle.kts,gradle/,.github/,.claude/'` (`last_commit` には今の HEAD が入る = 次回以降の変更から調べる)
- **見るパスの調整**: `paths` は git pathspec。`*` は階層をまたいでマッチする。生成物・snapshot のように大きくて技法を含まないパスは入れない (diff の上限を食う)
- **調べ直す**: `last_commit` を古い commit に戻すか、`force` 付きで手動実行する
- **外す**: `update-state.sh remove TBSten/foo`。候補に再び上がらないようにするなら続けて `ignore TBSten/foo --reason '...'`
- **見送った候補を採り直す**: `ignored` から消す (次回の候補に上がる) か、`add` する (`ignored` からは自動で消える)

## コストの上限

- detect は AI を呼ばない。差分も候補も無ければ update job は動かない
- diff は 1 ライブラリ 60KB まで (`--max-bytes`)。候補は 1 回 15 件まで
- update job は `timeout-minutes: 60`、claude は `--max-turns 80`
