# feedback-mode

アプリに **debug ビルド限定のフィードバックモード** を組み込み、溜まったフィードバックを Claude Code に消化させる。

## インストール

```sh
gh skill install tbsten/skills feedback-mode
```

## 概要

フィードバックモードでは、動いているアプリ上で画面の部品を選んでコメントを書く。アプリが文脈（どの部品か・画面の状態・直近のログ・
選んだ部品を枠で囲んだスクリーンショット）を自動で付けて、**1 件 = 1 記録**としてフィードバック置き場（Inbox）に保存する。
Claude Code は置き場を読み、記録を実装・調査・設計に振り分けてサブエージェントに任せ、対応済みの記録を archive して報告する。

この skill は **分岐ルーター**。起動時に環境を判定（`scripts/detect.sh`）し、必要な側だけを読む。

| 置き場 | 配線 | 判定 | 読むもの |
|---|---|---|---|
| ある | ある / 無い | `loop` | `references/loop/`（消化ループ）だけ |
| 無い | ある（部分的） | `partial` | 構築ガイドの一部の手順 → loop |
| 無い | 無い | `build` | `references/build/`（構築）だけ |

片側しか読まないので、関係のない指示がコンテキストに入らない。

## 使うべきタイミング

- Desktop（Electron / Tauri）・Web・スマホ（iOS / Android / React Native / Flutter）アプリで、画面・状態・ログ・スクリーンショット付きのフィードバックを Claude Code の作業に流したい
- フィードバックの保存先（ローカルのファイル・開発機への HTTP 受け口・Issue トラッカー・クラウドストレージ）を変えたい
- フォルダに溜まったフィードバックを、サブエージェントで一通り対応させたい
- すでに debug menu・開発者向け画面があり、そこにフィードバックの入口を足したい

## 設計方針

- **Sink による抽象化**: 置き場を `Sink` インターフェース（`putAttachments` / `putEntry` / `list` / `read` / `archive`）にした。
  同梱の実装はローカルのファイル・メモリ（テスト用）・HTTP。Issue トラッカー・クラウドストレージは概要のみ
- **debug ビルド限定**: 配線はビルド時に消す（DCE + 動的 import、`#if DEBUG`、debug source set、別 entrypoint など）。実行時の `enabled` は保険で、
  本番成果物を script で grep して確認する
- **既存のデバッグ基盤に乗る**: debug menu・dev panel があれば、新しい入口やガードを作らず、そこに 1 項目足す
- **ログ基盤は別タスク**: フィードバックモードは `getRecentLogs` の差し込み口からログを読んで添付するだけ

## 同梱物

| パス | 役割 |
|---|---|
| `SKILL.md` | ルーター。環境を判定し、build か loop の片側だけ読む |
| `references/concepts.md` | 用語・記録の形・状態遷移・Sink の契約（共通） |
| `references/build/` | 構築ガイド（手順・チェックリスト）・アーキテクチャ・環境別の実装指針 |
| `references/loop/` | 消化ループ（運用ルール・分類・履歴・報告）・サブエージェントのプロンプト・全体チェック・落とし穴 |
| `assets/templates/` | 導入先にコピーするコード: コア（Sink・採番・redact・Markdown の組み立て・node:test のテスト）・Electron main の配線・Web の UI・モバイル（React Native / Flutter）・既存の debug menu に乗せる例・TypeScript 版のコアと vitest の Sink 契約テスト（`core-ts/`）・Astro の Dev Toolbar 統合（`web/astro/`）・記録とメモの例・JSON スキーマ |
| `scripts/detect.sh` | 読み取りだけの環境判定。`VERDICT=loop` / `partial` / `build` を出す |
| `scripts/copy-core.sh` | コアのテンプレートを導入先にコピーし、`node:test` を実行する |
| `scripts/check-no-feedback-in-prod.sh` | 本番ビルドの成果物にフィードバックモードの痕跡が残っていないか grep する |

## 成熟度と既知の制約

status は **Experimental**。参照実装は、このループを数週間運用した社内向けの Electron ツール。
2 つ目の参照実装（Astro 7 + Starlight の Web の docs サイト）で、Astro の Dev Toolbar 統合・schema 1 の記録・場所の手がかり・メモ機能・TypeScript 版のコアを足した。TypeScript 版のコア（vitest 32 件）と、最小の Astro 7 のプロジェクトでの統合も確認済み。
確認済み: コアのテンプレート（メモリ・ファイル・HTTP の Sink に対する `node:test` 36 件）、Web の UI と開発用サーバ（ヘッドレス Electron）、
偽の成果物に対する本番検査。**未検証**: Electron main の配線・Tauri・React Native・Flutter・iOS / Android のネイティブ断片・html2canvas のスクリーンショット・
LAN 越しや実機からの HTTP・各環境の「本番から外す」ビルド設定・Issue トラッカー / クラウドストレージの Sink。
リリース前には必ず成果物を検査すること。
