---
name: contribute-batch
description: >
  現在のプロジェクトで得た複数の知見を skill / rule / prompt に仕分け (triage) し、
  必要なものを並列 subagent で一括作成して TBSten/skills リポジトリへの 1 つの PR にまとめる
  オーケストレータ型 skill。単一種別を 1 つだけ登録する場合は
  contribute-skill / contribute-rule / contribute-prompt を使う。
  Use when requested: "知見をまとめて contribute", "一括で contribute", "仕分けして PR",
  "contribute batch", "知見を仕分けして登録", "複数の知見をまとめて PR",
  "まとめてスキル化・ルール化", "batch contribute".
  gh CLI と git がインストールされている必要がある。
metadata:
  status: Experimental
---

# contribute-batch

現在のプロジェクトから複数の知見を収集し、skill / rule / prompt に仕分けて並列 subagent で一括作成し、TBSten/skills リポジトリへの 1 つの PR にまとめる。
このスキルの実行者は **オーケストレータ** として振る舞う。成果物の作成は subagent に委譲し、仕分け・README 更新・commit・PR 作成だけを自分で行う。

## 前提条件チェック

スキル起動時にまず以下を確認する。失敗した場合は対処方法を案内して中断する
(Step 4 の script でも再検証されるが、仕分け作業の前に早期検知する)。

1. `git --version` で git の存在を確認
2. `gh auth status` で gh CLI の認証状態を確認

## Step 1: 起動時の確認

ARGUMENTS からユーザーが収集したい知見群の説明を受け取る。以下を確認する。
ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **収集対象** — どの知見群を対象にするか。具体的なファイルパスやセクションを特定する
2. **batch slug** — kebab-case で命名。作業 branch 名 (`feat/contribute-batch-<slug>`) と作業ディレクトリ名に使う。知見群の内容から提案する
3. **リポジトリ** — デフォルトは `TBSten/skills`。fork を使う場合はユーザーに確認する

status / group は成果物ごとに異なるため、Step 3 の仕分けで決定する (デフォルトは `Experimental`)。

## Step 2: 知見の収集

ユーザーが指定した収集対象を読み取る。独立したファイル読み取りは必ず並列で実行すること。
対象が多い場合は Agent tool で並列に収集する。

主なソース:

- CLAUDE.md の特定セクション
- `.claude/rules/` / `.claude/skills/` 内の既存ファイル
- コードベース内のパターン・ユーティリティ・手順・設定ファイル
- 実際の利用箇所・テストコード (Grep で検索 → 並列 Read)
- ユーザーの説明そのもの

## Step 3: 仕分け (triage)

収集した知見を候補単位に分割し、それぞれ以下の基準で種別を判断する:

| 種別 | 向いている知見 | 配布形態 |
|---|---|---|
| **skill** | AI が繰り返し実行する再利用可能な手順・ワークフロー | インストールして常用 (`gh skill install`) |
| **rule** | コードを書く際に常時適用される規約・アーキテクチャ指針 | `.claude/rules/` に常駐 |
| **prompt** | 一回で完結するセットアップ・scaffold 手順 | インストール不要、raw URL で一回限り実行 |
| **見送り** | プロジェクト固有すぎる / 汎用化コストが過大 / 既存の skill・rule・prompt と重複 | — |

- 迷った場合は「繰り返し実行する手順か？」→ skill、「常時適用される規約か？」→ rule、「一回きりの手順か？」→ prompt の順に問う
- 1 つの知見群を複数種別に分割してよい (例: セットアップ手順 → prompt、運用規約 → rule)
- 既存の skill / rule / prompt との重複は README の一覧で確認する。既存の更新が適切なら見送りとし、その旨をユーザーに伝える

仕分け結果を以下の表でユーザーに提示し、**承認を得てから** 次のステップに進む:

| # | 知見 | 種別 | 名前 (kebab-case) | status | group | 判断根拠 |
|---|---|---|---|---|---|---|

見送りにした知見も表に含め、理由を明記する。

## Step 4: ワークスペースの準備

同梱の script を実行する。**script を読解・書き換え・再実装せず、そのまま実行する**:

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/setup-workspace.sh" --slug <slug> --repo <repo>
```

- `<slug>` は Step 1 の batch slug、`<repo>` は Step 1 のリポジトリ (デフォルト `TBSten/skills`。その場合 `--repo` は省略可)
- script はリポジトリの clone (既に clone 済みなら再利用) と作業 branch `feat/contribute-batch-<slug>` の作成 (既存なら checkout) を行う
- 成功時は末尾に 1 行 JSON `{"ok":true,"cloneDir":"...","branch":"..."}` を出力する。`cloneDir` / `branch` を以降のステップで使う
- 失敗時は stderr に「何が・なぜ・どう直すか」が出力され非 0 で終了する。対処してから再実行する (冪等なので再実行して壊れない)

## Step 5: 並列作成 (subagent)

仕分け結果の成果物ごとに subagent を起動する。**最大 5 並列**。6 件以上はバッチに分けて順次実行する。

各 subagent には以下を指示する:

1. `<cloneDir>/.claude/skills/contribute-<type>.md` (type は skill / rule / prompt) を読み込み、そのガイドの構成ルール・書き方・「AI 責務最小化 (script 化)」に従うこと
2. Step 2 で収集・整理した知見の内容 (subagent は現プロジェクトを知らないため、必要な知見は prompt に埋め込むかファイルパスで渡す)
3. 編集してよいのは **担当成果物のファイルのみ**:
   - skill: `skills/<name>/`、`skills/<name>.md`、`skills/<name>.ja.md`
   - rule: `rules/<name>/`、`rules/<name>.md`、`rules/<name>.ja.md`
   - prompt: `prompts/<name>/`、`prompts/<name>.md`、`prompts/<name>.ja.md`
4. **README.md / README.ja.md の更新、git commit / push、PR 作成は行わない** — これらは orchestrator の責務 (並列編集の競合防止)
5. プロジェクト固有情報 (パス・URL・認証情報・個人名等) を含めず、必要ならプレースホルダーに置換すること
6. 完了報告に含めさせる項目: 作成ファイル一覧 / status / group / README の説明セルに載せる 1 行説明 (日英とも 80 文字以内)

全 subagent の完了後、orchestrator が各担当ディレクトリの成果物を確認する。
不足・規約違反があれば該当 subagent に修正を指示するか、orchestrator が直接修正する。

## Step 6: 統合と PR 作成

orchestrator が以下を順に行う。

1. **README の一括更新** — 各 subagent の報告 (status / group / 1 行説明) をもとに、`README.md` / `README.ja.md` の該当テーブルへ全成果物の行を追加する。グループのまとまり・グループセルの書式・status 絵文字は `<cloneDir>/.claude/skills/contribute-<type>.md` の規約に従う
2. **セルフレビュー** — 成果物ごとに、対応するガイド (`contribute-<type>.md`) のセルフレビュー項目に従って確認する (「PR 前セルフレビュー: AI 責務最小化チェックリスト」を含む)。status / group の SSoT と README の一致、README 説明セル 80 文字以内も確認する
3. **プロジェクト固有情報の除外チェック** — 全成果物を横断して、プロジェクト固有のパス・URL・認証情報・個人名等が含まれていないか確認し、結果をユーザーに報告する
4. **ユーザー確認** — 以下の形式で提示し、**明示的な許可を得てから** push・PR 作成を実行する:

```
<cloneDir> に成果物を作成しました。
内容を確認したい場合はこのディレクトリを直接参照できます。

- push 先: <repo> (ブランチ: feat/contribute-batch-<slug>)
- 成果物一覧:
  - skill: <name> (<主要ファイル>)
  - rule: <name> (<主要ファイル>)
  - ...
- 見送りにした知見: <一覧。無ければ「なし」>
- プロジェクト固有情報: 含まれていないことを確認済み

このまま 1 つの PR を作成してよろしいですか？
```

5. **commit** — 意味単位で分ける。成果物ごとに 1 commit (`add <name> skill` / `add <name> rule` / `add <name> prompt`) + README 更新で 1 commit (`docs(readme): add contribute-batch artifacts`) を基本とする
6. **push と PR 作成** — branch を push し、**1 つの PR** を作成する

### PR フォーマット

- **タイトル**: 成果物が 1 件のみなら既存慣行どおり `Add skill: <name>` / `Add rule: <name>` / `Add prompt: <name>`。複数件は同じ書式をカンマ区切りで列挙する (例: `Add skill: <a>, rule: <b>`)
- **本文**: `## Summary` / `## 実行イメージ` / `## 備考` の 3 セクション **のみ** で構成する (既存 contribute-* と同じ)
  - **Summary** — 箇条書き最大 3 つ、各行 50 文字以内。バッチ全体の目的と主要ファイル 1〜3 つを含める
  - **実行イメージ** — 成果物ごとに発動・実行イメージを `1.` 始まりの番号付き箇条書きで 1 行ずつ列挙する。各行 100 文字以内
  - **備考** — 仕分けで見送りにした知見と理由を書く。無ければ「特になし」と書く

作成された PR の URL をユーザーに報告する。

## エラー時の対応

- `gh auth status` 失敗 → `gh auth login` を案内
- setup-workspace.sh 失敗 → stderr の対処方法に従って環境を直し、script を再実行する。script 自体は書き換えない
- subagent の成果物がガイドに不適合 → 該当 subagent に修正指示、または orchestrator が直接修正
- push 失敗 → リポジトリへの write 権限を確認するよう案内。fork の利用を提案
- 同名ブランチが push 先に既存で衝突 → 別の slug で Step 4 からやり直す (script は slug ごとに独立した clone・branch を作る)

## フォールバック

`<cloneDir>/.claude/skills/contribute-<type>.md` が存在しない場合、各成果物は contribute-skill / contribute-rule / contribute-prompt skill の「フォールバック」セクションと同じ最低限の構成で作成し、README のテーブルは手動で更新するようユーザーに案内する。
