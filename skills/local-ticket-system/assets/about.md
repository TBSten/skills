# .local/ticket — チケット管理ルール

このディレクトリでは、プロジェクトのタスク・バグ・チャプターを Markdown ファイルで管理する。

## ディレクトリ構成

```
.local/ticket/
  about.md                # 本ファイル（運用ルール）
  task-0xx-template.md    # タスクチケットテンプレート
  chapter-template.md     # チャプターテンプレート
  task-xxx-*.md           # 作業中のタスクチケット
  bug-xxx-*.md            # 作業中のバグチケット
  chapter-*.md            # チャプター（複数チケットの上位概念）
  done/                   # 完了済みチケット（実装完了・commit 済み）
  closed/                 # クローズ済みチケット（動作確認・検証済み）
  archived/               # アーカイブ済みチャプター
```

## チケット種別

| 種別 | 用途 | 命名規則 |
|------|------|----------|
| task | 1つの作業単位 | `task-{連番3桁}-{slug}.md` |
| bug | 不具合の記録と修正 | `bug-{連番3桁}-{slug}.md` |
| chapter | 複数の task/bug をまとめる上位概念 | `chapter-{slug}.md` |

## task / bug のライフサイクル

1. **作成**: `ticket/` 直下に `task-xxx-*.md` または `bug-xxx-*.md` を作成
2. **作業中**: チケット内のチェックリストを消化しながら実装を進める
3. **done**: 実装・commit が完了したら `done/` へ移動
4. **closed**: 動作確認等が完了したら `closed/` へ移動

## chapter のライフサイクル

1. **作成**: `ticket/` 直下に `chapter-*.md` を作成
2. **検討中**: スコープや検討事項を詰めていく
3. **分割**: task / bug チケットに分割する
4. **archived**: 全ての子チケットが完了したら `archived/` へ移動

chapter は done/closed には移動しない。

## 命名規則

- タスク: `task-{連番3桁}-{slug}.md` (例: `task-016-publish.md`)
- バグ: `bug-{連番3桁}-{slug}.md` (例: `bug-001-login-error.md`)
- チャプター: `chapter-{slug}.md` (例: `chapter-multi-tenant.md`)

## チケットの構成 (テンプレート)

各タスク・バグチケットは以下のセクションで構成される:

```markdown
# タイトル

概要説明

## 検証方法

動作確認の手順・コマンド

## チェックリスト

- [ ] 実装タスク ...
- [ ] 不要ファイルの削除
- [ ] 検証（検証方法セクションの項目を実施）
- [ ] セルフレビュー
- [ ] commit
- [ ] このチケットを done/ に移動
```

## 注意事項

- `.local/` ディレクトリは Git の管理対象外（`.gitignore` に追加）
- チェックリスト末尾の共通項目（不要ファイル削除、検証、セルフレビュー、commit、done 移動）は必ず含める
