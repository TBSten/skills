# kotlin-compiler-plugin-dev

30+ の既存プラグイン調査データをもとに、Kotlin Compiler Plugin の開発・レビューを支援するスキル。

## できること

1. **前例を探す** — 30+ の既存 compiler plugin から、やりたいことに近い実装を検索する
2. **Extension Point を選ぶ** — 要件に合った FIR/IR Extension Point を提案する
3. **実装をレビュー** — 確立されたパターンとベストプラクティスをもとに実装を評価する
4. **複数バージョン対応をレビュー** — compat module layer / source set separation のアーキテクチャ評価、タンデム vs 独立リリース戦略の判断支援
5. **ソースコードを深掘り** — deepwiki MCP を使って参考プラグインの最新ソースコードを読む

## こんなときに使う

- 新しい compiler plugin プロジェクトを始めるとき
- 既存の compiler plugin に機能を追加するとき
- compiler plugin の実装を正しいか確認・レビューしたいとき
- 特定の機能を既存プラグインがどう実装しているか調べたいとき
- FIR と IR のどちらで処理すべきか判断したいとき

## 動作の流れ

1. **Step 0**: deepwiki MCP が利用可能か確認する（任意。ソースコード探索を強化）
2. **Step 1**: 要件を把握する（開発 / レビュー / 調査）
3. **Step 2**: `overview.md` を検索 — 30 プラグイン × 130+ Extension Point 実装の一覧表
4. **Step 3**: `details/` ファイルで詳細を確認（継承クラス、オーバーライドメソッド、動作、診断）
5. **Step 4**: 必要に応じて deepwiki MCP で最新ソースコードを確認
6. **Step 5**: 設計提案・レビューレポート・調査サマリを出力

## 同梱リソース

| ファイル | 内容 |
|---|---|
| `references/overview.md` | 30+ プラグインの全 Extension Point をまとめたフラットな一覧表（ソース URL 付き） |
| `references/patterns.md` | 4 つの設計パターン + Extension Point 選択ガイド + Multi-Version Support アーキテクチャ (compat module layer / source set separation) |
| `references/review-checklist.md` | K2 対応・設計パターン・コード品質・複数 Kotlin バージョン対応のチェックリスト |
| `references/details/*.md` | プラグイングループごとのソースコードレベルの詳細（8 ファイル） |

## 前提条件

- Kotlin プロジェクトと compiler plugin のソースコード（または作成計画）
- 任意: deepwiki MCP サーバー（設定するとソースコード探索が強化される）

## インストール

```sh
npx skills add TBSten/skills --skill kotlin-compiler-plugin-dev
```
