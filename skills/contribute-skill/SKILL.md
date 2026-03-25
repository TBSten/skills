---
name: contribute-skill
description: >
  現在のプロジェクトで得た知見・パターン・ワークフローを TBSten/skills リポジトリに
  skill として登録するための PR を自動作成する。
  プロジェクトの CLAUDE.md、.claude/rules/、.claude/skills/、コードベースから知見を収集し、
  再利用可能な skill としてパッケージングして PR を作成するまでを一貫して行う。
  Use when requested: "知見をスキルリポジトリに登録", "contribute skill", "この知見を共有",
  "スキルとして登録", "知見をまとめて PR", "このパターンをスキル化".
  gh CLI と git がインストールされている必要がある。
---

# contribute-skill

現在のプロジェクトから知見を収集し、TBSten/skills リポジトリに skill として登録する PR を自動作成する。

## 前提条件チェック

スキル起動時にまず以下を確認する。失敗した場合は対処方法を案内して中断する。

1. `git --version` で git の存在を確認
2. `gh auth status` で gh CLI の認証状態を確認

## Step 1: 起動時の確認

ARGUMENTS からユーザーが収集したい知見の説明を受け取る。以下を確認する。
ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **skill 名** — kebab-case で命名。知見の内容から適切な名前を提案する
2. **収集対象** — どの知見をスキル化するか。具体的なファイルパスやセクションを特定する
3. **スキルの対象ユーザー** — どのようなプロジェクト・状況で使われるスキルか
4. **リポジトリ** — デフォルトは `TBSten/skills`。fork を使う場合はユーザーに確認する

## Step 2: 知見の収集と整理

ユーザーが指定した収集対象を読み取る。

### 収集の効率化

収集対象が多い場合、Agent tool を活用して並列に収集する:

1. コアファイルの読み取り (Glob でファイル一覧取得 → 並列 Read)
2. 利用箇所の調査 (Grep で呼び出し元を検索 → 並列 Read)
3. テストコードの調査 (並列 Agent)

独立したファイル読み取りは必ず並列で実行すること。

主なソース:

- CLAUDE.md の特定セクション
- `.claude/rules/` 内のルールファイル
- `.claude/skills/` 内の既存スキル
- コードベース内のパターンやユーティリティ
- **実際の利用箇所** — Grep 等で対象コードの呼び出し元を調査し、利用パターン（ViewModel での使い方、UI での使い方、テストでの使い方等）を収集する
- **テストコード** — 対象コードのテストを検索 (`*Test.kt`, `*Spec.kt`, `*.test.ts` 等)
- ユーザーの説明そのもの

### テストコードの扱い

テストが見つかった場合、以下の基準で同梱方針を判断する:

- **example/test/ に同梱**: テストの書き方自体がスキルの一部である場合（テストパターンがスキルの価値の一つ）
- **references/ に記載**: テストパターンを参考情報として提供する場合
- **同梱しない**: テストがプロジェクト固有のフレームワークに強く依存する場合

同梱する場合は Step 3.5 と同様にプロジェクト固有の依存を除去する。

読み取った知見を以下の形式で整理し、ユーザーに提示する:

- **スキルの目的** — 1〜2文で何を実現するか
- **トリガー条件** — どのような発話・状況で発動すべきか (5個以上リストアップ)
- **手順・ワークフロー** — 具体的な実行ステップの箇条書き
- **利用パターン** — 実プロジェクトでの使い方を 3 つ以上リストアップ
- **同梱リソース** — サンプルコードやテンプレート等。不要であれば「なし」と明記
- **対象プロジェクトの前提** — 言語、フレームワーク、ディレクトリ構成等の前提条件

ユーザーの承認を得てから次のステップに進む。

## Step 3: ワークディレクトリの準備と add-skill.md の読み込み

1. ワークディレクトリを準備する:

```bash
rm -rf /tmp/contribute-skill
git clone --depth 1 https://github.com/<repo>.git /tmp/contribute-skill
```

`<repo>` は Step 1 で確認したリポジトリ (デフォルト: `TBSten/skills`)。

2. clone したリポジトリ内のスキル作成ガイドを読み込む:

```
/tmp/contribute-skill/.claude/skills/add-skill.md
```

add-skill.md を読み込んだ後、以下のように統合する:

- **add-skill.md の Step 1 (確認事項)**: contribute-skill の Step 1-2 で既に完了。スキップする
- **add-skill.md の Step 2 (SKILL.md 作成)**: フォーマット・構成ルールに従う。内容は contribute-skill の Step 2 で整理した知見を使う
- **add-skill.md の Step 3-5 (リソース配置・ドキュメント・README)**: そのまま従う

ガイドが見つからない場合は、以下の「フォールバック」セクションに従う。

## Step 3.5: コードの汎用化

example にコードを配置する前に以下を確認・実行する:

1. **プロジェクト固有の import を検出**: example 対象ファイルの import を全走査し、スキルの example パッケージ外への依存をリストアップ
2. **依存の分類と対処**:
   - 標準ライブラリ (`kotlin.*`, `kotlinx.*`) → そのまま
   - フレームワーク標準 (`androidx.*`, `react` 等) → そのまま
   - プロジェクト固有ユーティリティ → 標準的な代替に置換、または example に同梱
   - プロジェクト固有アノテーション → 削除
3. **パッケージ名の抽象化**: `com.example.<skill-name-without-hyphens>` に統一 (add-skill.md のパッケージ名規約に準拠)
4. **置換結果をユーザーに提示**: 何を何に置換したかの一覧を示し、承認を得る

## Step 4: スキルファイルの作成

clone した `/tmp/contribute-skill/` 内で、add-skill.md の手順に従い以下を作成する。
Step 2 で整理した知見をもとに、SKILL.md の各セクションを埋めていく。

1. `skills/<skill-name>/SKILL.md` — frontmatter (name, description) + 手順書
2. 必要に応じて `references/`, `example/`, `assets/` 内のリソース
3. `skills/<skill-name>.md` — 詳細ドキュメント (英語)
4. `skills/<skill-name>.ja.md` — 詳細ドキュメント (日本語)
5. `README.md` と `README.ja.md` の Available Skills テーブルに行を追加

### レビューの提示方法

作成ファイル数に応じて提示方法を変える:

- **5 ファイル以下**: 全ファイルの内容を提示
- **6 ファイル以上**: 以下を提示
  1. ファイル一覧と各ファイルの概要 (1行)
  2. SKILL.md の全内容 (最重要ファイル)
  3. 特に注意が必要なファイルをピックアップして内容を提示
  4. 「他のファイルも確認しますか？」とユーザーに確認

フィードバックがあれば修正してから次に進む。

### プロジェクト固有情報の除外チェック

スキルは公開リポジトリに登録されるため、作成したファイルに以下が含まれていないか細心の注意を払う:

- プロジェクト固有のファイルパス、URL、ドメイン名
- 社内システムやサービスの名前
- 認証情報、トークン、API キー
- 個人名、メールアドレス、チーム名
- 社内ドキュメントへのリンク
- その他、公開すべきでない情報

知見を汎用化する際に具体例が必要な場合は、プレースホルダー (`<project-name>`, `<your-package>` 等) に置き換える。
チェック結果をユーザーに報告し、問題がないことを確認してから次に進む。

## Step 4.5: セルフレビュー

PR 作成前に以下を確認する:

1. **必要なファイルが揃っているか** — 以下のファイルが存在することを確認する:
   - `./skills/<skill-name>.ja.md` が存在すること
   - `./skills/<skill-name>.md` が存在すること
   - `./skills/<skill-name>/SKILL.md` が存在すること
   - SKILL.md 内で参照しているすべてのファイルが `./skills/<skill-name>/` 配下に存在すること
   - `README.md` と `README.ja.md` の Available Skills テーブルに新しいスキルのエントリが追加されていること
2. **SKILL.md の word count が 5,000 words 以下か** — `wc -w` で確認。超える場合は詳細を references/ に分離する
3. **SKILL.md と references/ に情報重複がないか** — SKILL.md には概要・手順のみ、詳細コード例は references/ に分離
4. **example/ 内の import が整合しているか** — `grep -r "^import"` でプロジェクト固有依存が残っていないか確認
5. **README テーブルが既存行と同じフォーマットか** — HTML タグ、改行、code block の書き方を既存行と比較

問題が見つかった場合は修正してからユーザーに報告する。

## Step 5: PR の作成

以下の内容をユーザーに提示し、**push と PR 作成の許可を明示的に得てから**実行する:

- push 先リポジトリとブランチ名
- コミットに含まれるファイル一覧
- プロジェクト固有情報が含まれていないことの最終確認

許可を得たら、以下を実行する:

```bash
cd /tmp/contribute-skill
git checkout -b add-skill/<skill-name>
git add skills/<skill-name>/ README.md README.ja.md
git commit -m "add <skill-name> skill"
git push -u origin add-skill/<skill-name>
```

PR を作成する:

```bash
gh pr create \
  --repo <repo> \
  --head add-skill/<skill-name> \
  --title "Add <skill-name> skill" \
  --body "## Summary
- <Step 2 で整理したスキルの目的>

## Files
- skills/<skill-name>/SKILL.md
- <その他の追加ファイル一覧>

## Test plan
- [ ] npx skills add <repo> --skill <skill-name> でインストールできること
- [ ] スキルのトリガーフレーズで正しく発動すること
"
```

作成された PR の URL をユーザーに報告する。

## エラー時の対応

- `gh auth status` 失敗 → `gh auth login` を案内
- clone 失敗 → ネットワーク接続の確認を案内
- push 失敗 → リポジトリへの write 権限を確認するよう案内。fork の利用を提案
- PR 作成失敗で同名ブランチが既存 → ブランチ名にサフィックス (`-v2` 等) を付与して再試行

## フォールバック

add-skill.md が clone 先に存在しない場合、以下の最低限の構成で作成する:

1. `skills/<skill-name>/SKILL.md` に YAML frontmatter (name, description) + 手順を記述
2. `skills/<skill-name>.md` / `<skill-name>.ja.md` に概要・使い方・前提条件を記述
3. README のテーブルは手動で更新するようユーザーに案内する
