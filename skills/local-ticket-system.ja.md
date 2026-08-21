# local-ticket-system

Markdown ベースのローカルチケット管理システム。

## インストール

```sh
gh skill install tbsten/skills local-ticket-system
```

## 概要

`.local/ticket/` ディレクトリ構造でタスク・バグ・チャプターを Markdown ファイルとして管理するスキル。`.local/` は gitignore 対象のため、リポジトリ履歴を汚さずに作業中のタスクを気軽に管理できる。

## 機能

- **チケット種別の判断** — 要件を分析して適切な種別 (task / bug / chapter) を選択してから作成
- **タスクチケット** (`task-{NNN}-{slug}.md`) — チェックリスト形式で機能実装を追跡
- **バグチケット** (`bug-{NNN}-{slug}.md`) — 再現手順・修正候補を含むバグ記録
- **チャプターチケット** (`chapter-{slug}.md`) — 関連する task/bug を上位目標でグループ化。スコープ・モチベーション・分割計画を含む
- **ライフサイクル管理** — task/bug: アクティブ → `done/` → `closed/`。chapter: アクティブ → task に分割 → `archived/`。意図的な先送りは `deferred/`
- **絵文字 prefix タイトル** — チケットタイトル先頭に絵文字 prefix を付与 (例: `🗑️ 不要なコード削除`、`✨ ログイン機能の追加`、`🐛 null pointer in user lookup`) し、一覧でひと目で分類できる
- **テンプレートベース** — 共通チェックリスト項目付きの統一フォーマット
- **script による決定的操作** — 同梱 script が決定的な作業 (ディレクトリ作成・連番算出・ステータス移動) を担当し、AI は判断 (種別・絵文字・タイトル・本文) に専念
- **言語・フレームワーク不問** — どのプロジェクトでも利用可能

## ディレクトリ構成

```
.local/ticket/
├── about.md              # 運用ルール
├── task-0xx-template.md  # タスクチケットテンプレート
├── bug-0xx-template.md   # バグチケットテンプレート
├── chapter-template.md   # チャプターテンプレート
├── task-xxx-*.md         # 作業中のタスクチケット
├── bug-xxx-*.md          # 作業中のバグチケット
├── chapter-*.md          # チャプター
├── done/                 # 完了済みチケット（実装完了・commit 済み）
├── closed/               # クローズ済みチケット（動作確認・検証済み）
├── archived/             # アーカイブ済みチャプター
└── deferred/             # 後回しチケット（意図的に将来に先送り）
```

## Scripts

決定的な作業は同梱 script が担当する (読解・再実装せずそのまま実行する)。失敗時は stderr に `ERROR:` / `WHY:` / `FIX:` が出力される。

| Script | 用途 |
|--------|------|
| `scripts/setup.sh` | 冪等なワンショットセットアップ: `.local/ticket/` + ステータスサブディレクトリの作成、テンプレートのコピー、`.gitignore` への `.local/` 追記 |
| `scripts/new-ticket.sh <task\|bug\|chapter> <slug>` | 次の連番を算出 (`done/` `closed/` `deferred/` `archived/` も種別ごとに走査) してテンプレートからチケットを作成し、パスを出力 |
| `scripts/move-ticket.sh <file> <done\|closed\|deferred\|archived>` | チケットのステータス移動。`deferred` 指定時は理由ブロック雛形 (実行日付き) を追記してから移動 |

## チケット種別

| 種別 | 用途 | 粒度 |
|------|------|------|
| task | 1つの作業単位 | 小〜中。1回の作業セッションで完了できる |
| bug | 既存の不具合の記録と修正 | 小〜中。1つのバグに対して1チケット |
| chapter | 複数の task/bug をまとめる上位概念 | 大。要件整理 → task/bug に分割して進める |

## チケットのライフサイクル

### task / bug

1. **作成** — `.local/ticket/` 直下にチケットを配置
2. **作業中** — チェックリストを消化しながら実装
3. **done** — 実装・commit が完了 → `done/` へ移動
4. **closed** — 動作確認が完了 → `closed/` へ移動
5. **deferred** — 意図的に後回し → `deferred/` へ移動（再着手の意図あり）

### chapter

1. **作成** — `.local/ticket/` 直下にチャプターを配置
2. **検討中** — スコープや検討事項を詰めていく
3. **分割** — task / bug チケットに分割する
4. **archived** — 全ての子チケットが完了 → `archived/` へ移動
5. **deferred** — 着手を先送りにするとき → `deferred/` へ移動

### deferred/ のルール

`scripts/move-ticket.sh <file> deferred` を実行すると、移動前に以下の雛形 (日付は実行日) がチケット末尾へ自動追記される。移動後に `TODO:` プレースホルダを実際の内容に書き換える:

```markdown
**Deferred 理由**: TODO:DeferredReason
**再起票 trigger**: TODO:ReopenTrigger
**Deferred 日付**: YYYY-MM-DD
```

再着手するときは `deferred/` から `ticket/` 直下に戻す。

## 前提条件

- Git 管理されたプロジェクト
- `.local/` が `.gitignore` に含まれていること
