# local-ticket-system

Markdown ベースのローカルチケット管理システム。

## インストール

```sh
npx skills add tbsten/skills \
  --skill local-ticket-system
```

## 概要

`.local/ticket/` ディレクトリ構造でタスク・バグ・チャプターを Markdown ファイルとして管理するスキル。`.local/` は gitignore 対象のため、リポジトリ履歴を汚さずに作業中のタスクを気軽に管理できる。

## 機能

- **チケット種別の判断** — 要件を分析して適切な種別 (task / bug / chapter) を選択してから作成
- **タスクチケット** (`task-{NNN}-{slug}.md`) — チェックリスト形式で機能実装を追跡
- **バグチケット** (`bug-{NNN}-{slug}.md`) — 再現手順・修正候補を含むバグ記録
- **チャプターチケット** (`chapter-{slug}.md`) — 関連する task/bug を上位目標でグループ化。スコープ・モチベーション・分割計画を含む
- **ライフサイクル管理** — task/bug: アクティブ → `done/` → `closed/`。chapter: アクティブ → task に分割 → `archived/`。意図的な先送りは `deferred/`
- **テンプレートベース** — 共通チェックリスト項目付きの統一フォーマット
- **言語・フレームワーク不問** — どのプロジェクトでも利用可能

## ディレクトリ構成

```
.local/ticket/
├── about.md              # 運用ルール
├── task-0xx-template.md  # タスクチケットテンプレート
├── chapter-template.md   # チャプターテンプレート
├── task-xxx-*.md         # 作業中のタスクチケット
├── bug-xxx-*.md          # 作業中のバグチケット
├── chapter-*.md          # チャプター
├── done/                 # 完了済みチケット（実装完了・commit 済み）
├── closed/               # クローズ済みチケット（動作確認・検証済み）
├── archived/             # アーカイブ済みチャプター
└── deferred/             # 後回しチケット（意図的に将来に先送り）
```

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

移動前にチケット内に以下を追記すること:

```markdown
**Deferred 理由**: <なぜ後回しにするか>
**再起票 trigger**: <どういう条件で再着手するか>
**Deferred 日付**: YYYY-MM-DD
```

再着手するときは `deferred/` から `ticket/` 直下に戻す。

## 前提条件

- Git 管理されたプロジェクト
- `.local/` が `.gitignore` に含まれていること
