---
name: local-ticket-system
description: >
  Markdown ベースのローカルチケット管理システムをプロジェクトにセットアップする。
  .local/ticket/ ディレクトリにタスクチケットやバグチケットを作成し、
  チェックリスト形式で進捗を追跡する。Git 管理対象外のため気軽に使える。
  言語・フレームワーク不問。
  Use when requested: "チケットシステムを導入", "タスク管理を Markdown で",
  "ローカルチケット管理をセットアップ", "チケットを作成", "バグチケットを作成",
  "local ticket system", "setup ticket management".
---

# local-ticket-system

プロジェクトに `.local/ticket/` ディレクトリベースのチケット管理システムをセットアップし、チケットの作成・管理を行う。

## Usage

以下のいずれかの状況で使用する:

- プロジェクトにローカルチケット管理を新規導入したい
- 既存のチケットシステムにタスクチケットやバグチケットを追加したい
- チケットのステータスを変更したい (done / closed への移動)

## Step 1: セットアップ (初回のみ)

プロジェクトに `.local/ticket/` が存在しない場合、以下を実行する。

1. `.local/ticket/` ディレクトリを作成
2. `assets/about.md` を `.local/ticket/about.md` としてコピー
3. `assets/task-0xx-template.md` を `.local/ticket/task-0xx-template.md` としてコピー
4. `.gitignore` に `.local/` が含まれていなければ追加

既に `.local/ticket/` が存在する場合はスキップする。

## Step 2: チケットの作成

ユーザーの要件に応じてチケットを作成する。

### 命名規則

- タスク: `task-{連番3桁}-{slug}.md` (例: `task-001-add-login.md`)
- バグ: `bug-{連番3桁}-{slug}.md` (例: `bug-001-null-pointer.md`)

連番は `.local/ticket/` 内の既存チケット (done/, closed/ 含む) から最大番号を取得し +1 する。

### チケットの構成

テンプレート (`assets/task-0xx-template.md`) をベースに、以下のセクションで構成する:

```markdown
# タイトル

概要説明

## 検証方法

動作確認の手順・コマンド

## チェックリスト

- [ ] 実装タスク 1
- [ ] 実装タスク 2
- [ ] 不要ファイルの削除
- [ ] 検証（検証方法セクションの項目を実施）
- [ ] セルフレビュー
- [ ] commit
- [ ] このチケットを done/ に移動
```

チェックリスト末尾の共通項目 (不要ファイル削除〜done 移動) は必ず含める。

### バグチケットの場合

バグチケットには以下を追加する:

- **再現手順** — 問題を再現する具体的な手順
- **期待される動作** と **実際の動作**
- **推測される原因** — わかる範囲で
- **対応方針の候補** — 複数案がある場合はリストアップ

## Step 3: チケットのステータス管理

チケットのライフサイクル:

1. **作成**: `.local/ticket/` 直下に配置
2. **作業中**: チェックリストを消化しながら実装
3. **done**: 実装・commit が完了 → `done/` へ移動
4. **closed**: 動作確認・検証が完了 → `closed/` へ移動

ステータス変更はファイルの移動で行う:

```bash
mv .local/ticket/task-001-add-login.md .local/ticket/done/
```
