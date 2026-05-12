# kotlin-compiler-plugin-dev

30+ の既存プラグイン調査データをもとに、Kotlin Compiler Plugin の開発・レビュー、および複数 Kotlin バージョン対応の追加・削除を支援するスキル。

## できること

1. **前例を探す** — 30+ の既存 compiler plugin から、やりたいことに近い実装を検索する
2. **Extension Point を選ぶ** — 要件に合った FIR/IR Extension Point を提案する
3. **実装をレビュー** — 確立されたパターンとベストプラクティスをもとに実装を評価する
4. **複数バージョン対応をレビュー** — compat module layer / source set separation のアーキテクチャ評価、タンデム vs 独立リリース戦略の判断支援
5. **サポート Kotlin バージョンの追加・削除** — compat module の delegation pattern / capability flag / reflection shim / SSOT 駆動 CI matrix / kctfork version mapping を駆使して新バージョン対応を導入
6. **ソースコードを深掘り** — deepwiki MCP を使って参考プラグインの最新ソースコードを読む

## こんなときに使う

- 新しい compiler plugin プロジェクトを始めるとき
- 既存の compiler plugin に機能を追加するとき
- compiler plugin の実装を正しいか確認・レビューしたいとき
- 特定の機能を既存プラグインがどう実装しているか調べたいとき
- FIR と IR のどちらで処理すべきか判断したいとき
- 既に複数バージョン対応基盤を持つプラグインに新しい Kotlin バージョンを追加 / 削除したいとき

## 動作の流れ

1. **Step 0**: deepwiki MCP が利用可能か確認する（任意。ソースコード探索を強化）
2. **Step 1**: 要件を把握する（開発 / レビュー / 調査 / サポートバージョン追加・削除）
3. **Step 2**: `overview.md` を検索 — 30 プラグイン × 130+ Extension Point 実装の一覧表
4. **Step 3**: `details/` ファイルで詳細を確認（継承クラス、オーバーライドメソッド、動作、診断）
5. **Step 4**: 必要に応じて deepwiki MCP で最新ソースコードを確認
6. **Step 5**: 設計提案・レビューレポート・調査サマリを出力
7. **Step 6**: サポートバージョン追加・削除モードでは `references/multi-version-workflow.md` を参照しつつ、compat module 追加・SSOT/CI matrix 更新・テスト切り分けを実施

## 同梱リソース

| ファイル | 内容 |
|---|---|
| `references/overview.md` | 30+ プラグインの全 Extension Point をまとめたフラットな一覧表（ソース URL 付き） |
| `references/patterns.md` | 4 つの設計パターン + Extension Point 選択ガイド + Multi-Version Support アーキテクチャ (compat module layer / source set separation) |
| `references/review-checklist.md` | K2 対応・設計パターン・コード品質・複数 Kotlin バージョン対応のチェックリスト |
| `references/details/*.md` | プラグイングループごとのソースコードレベルの詳細（8 ファイル） |
| `references/multi-version-workflow.md` | サポート Kotlin バージョン追加・削除の詳細ワークフロー |
| `references/compat-module-setup.md` | `CompatContext` SPI / delegation pattern / ShadowJar 設定 |
| `references/source-set-separation.md` | source set 分離アプローチの詳細 |
| `references/ci-matrix.md` | SSOT 駆動 dynamic matrix の YAML テンプレ / per-version test script |
| `references/kotlin-tooling-version.md` | `KotlinToolingVersion` with Maturity 比較ロジック |
| `references/version-gating.md` | capability flag の設計 + テストの self-skip |
| `references/reflection-shim.md` | 小さな差分を吸収する reflection shim |
| `references/troubleshooting.md` | 失敗パターン別の原因と対処 |

## 前提条件

- Kotlin プロジェクトと compiler plugin のソースコード（または作成計画）
- サポートバージョン追加・削除を行う場合は、複数バージョン対応基盤 (compat module layer または source set separation) が既に存在すること。基盤の初期セットアップは `kotlin-compiler-plugin-setup` の Step 10 を参照
- 任意: deepwiki MCP サーバー（設定するとソースコード探索が強化される）

## インストール

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-dev
```
