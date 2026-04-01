# local-ticket-system

Markdown ベースのローカルチケット管理システム。

## インストール

```sh
npx skills add tbsten/skills \
  --skill local-ticket-system
```

## 概要

`.local/ticket/` ディレクトリ構造でタスクやバグを Markdown ファイルとして管理するスキル。`.local/` は gitignore 対象のため、リポジトリ履歴を汚さずに作業中のタスクを気軽に管理できる。

## 機能

- **タスクチケット** (`task-{NNN}-{slug}.md`) — チェックリスト形式で機能実装を追跡
- **バグチケット** (`bug-{NNN}-{slug}.md`) — 再現手順・修正候補を含むバグ記録
- **ライフサイクル管理** — チケットをステージ移動: アクティブ → `done/` → `closed/`
- **テンプレートベース** — 共通チェックリスト項目付きの統一フォーマット
- **言語・フレームワーク不問** — どのプロジェクトでも利用可能

## ディレクトリ構成

```
.local/ticket/
├── about.md              # 運用ルール
├── task-0xx-template.md  # チケットテンプレート
├── task-xxx-*.md         # 作業中のタスクチケット
├── bug-xxx-*.md          # 作業中のバグチケット
├── done/                 # 完了済みチケット（実装完了・commit 済み）
└── closed/               # クローズ済みチケット（動作確認・検証済み）
```

## チケットのライフサイクル

1. **作成** — `.local/ticket/` 直下にチケットを配置
2. **作業中** — チェックリストを消化しながら実装
3. **done** — 実装・commit が完了 → `done/` へ移動
4. **closed** — 動作確認が完了 → `closed/` へ移動

## 前提条件

- Git 管理されたプロジェクト
- `.local/` が `.gitignore` に含まれていること

## インストール

```sh
npx skills add tbsten/skills --skill local-ticket-system
```
