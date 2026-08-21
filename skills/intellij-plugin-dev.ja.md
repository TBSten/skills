# intellij-plugin-dev

IntelliJ Platform / Android Studio 向けプラグインを、人手を最小化してエージェント主導で開発するためのツーリング・進め方リファレンス。Jewel/Compose の tool window + Kotlin Analysis API frontend + gutter line marker 構成のプラグインを実装して確定した知見をまとめている。

## 中心となる考え方

IDE プラグインは「実装したものを IDE に入れて手で触る」以外の検証手段が揃っているか不透明で、フィードバックループを組めるかが最初のリスクになる。このスキルは検証を **6 チャネル**に分解し、

- **正しさ = ヘッドレス機能テスト** (`BasePlatformTestCase` + Kotlin Analysis API)
- **見た目 = headless PNG 自己目視** (`renderComposeScene` + standalone Jewel)

の 2 本を主軸に据え、実 IDE 起動系 (Driver / 実機 AS) は毎回ではなく定期チェックポイントに寄せる。これにより実装→検証→修正の大半を人手なしで回せる。

## できること

1. **build 基盤を組む** — intellijPlatform (v2) / bundled Kotlin(AA)・Jewel・Compose・Skiko / JBR21 / K2 / since-until を配線し、`compileKotlin`・`buildPlugin`・`runIde` が通るところまで。動く scaffold を `example/` として同梱し、`scripts/scaffold.sh` で自分のリポジトリに生成できる
2. **機能テストを書く** — 注釈を Analysis API (K2) で解析する frontend を `BasePlatformTestCase` 上でヘッドレスにテスト。ジェネリック注釈の型引数解決・注釈スタブ fixture・logged error の握り潰しまで
3. **見た目を headless で回す** — Jewel/Compose UI を `renderComposeScene` で PNG に焼き、VRT golden で回帰を掛ける (透明角検査・managed clean・機械判定ゲート込み)
4. **IDE に組み込む** — tool window (`addComposeTab`) / gutter line marker / エディタ追従 / ノード→ソースのナビ / PSI 挿入 (コード生成) の配線と、lifecycle・性能の罠の回避
5. **Driver スモークを足す** — 実 IDE を薄く駆動する E2E スモークを定期チェックポイントに配線し、UI ツリー (locator) を覗く

## こんなときに使う

- 新しい IntelliJ / Android Studio プラグインの開発を始めるとき
- Jewel/Compose の tool window や gutter line marker を実装するとき
- 「Analysis API がテストで動かない」「注釈の型引数が取れない」で詰まったとき
- UI の見た目を IDE 起動なしで確認したい / VRT golden を組みたいとき
- エディタ追従・ノード→ソースのナビ・PSI 挿入 (コード生成) を配線したいとき
- 実 IDE を駆動する Driver スモークを足したいとき
- Android Studio と IntelliJ の build 番号スキュー等の固有ハマりに遭ったとき

## 動作の流れ

1. **Step 1**: SKILL.md の「中心思想」で検証を 6 チャネルに分解し、主軸 2 本 (機能テスト / PNG 目視) で回す方針を確認する
2. **Step 2**: 同梱の `example/` から `scripts/scaffold.sh` でプラグインモジュールを生成し (script は読解・再実装せずそのまま実行する)、`references/setup/` (basics → preview → snapshot) を設計解説として読む
3. **Step 3**: 実装したい観点に応じて usage reference を開く (機能テスト = `analysis-api-testing.md` / 見た目 = `headless-preview.md` / IDE 組み込み = `ide-integration.md`)
4. **Step 4**: headless で閉じられない対話・タイミングは `driver-smoke.md` の Driver スモークを定期チェックポイントに置く
5. **Step 5**: Compose Desktop / IntelliJ 固有の罠に当たったら `gotchas.md` の索引から一次記載へ飛ぶ

## 同梱リソース

| ファイル | 内容 |
|---|---|
| `example/` | references の snippet を整合する 1 プロジェクトに束ねた動く scaffold (`com.example.plugin`・独立 Gradle ビルド): build 配線 / plugin.xml / Compose tool window + 共有 Composable / `PreviewMain.kt`・`PreviewChecks.kt` / AA テスト harness。コード片の SSoT |
| `scripts/scaffold.sh` | `example/` から新規プラグインモジュールを生成する (`--dest` / `--package` / `--plugin-id` / `--plugin-name`、`--dry-run` / `--force`)。冪等 (`--force` なし上書き禁止)・末尾 1 行 JSON |
| `references/setup/basics.md` | 基本 build 配線 (intellijPlatform / SDK 261 / bundled Kotlin(AA)・Jewel・Compose・Skiko / JBR21 / K2 / since-until)。統合ディストリの罠・stdlib 非同梱 |
| `references/setup/preview.md` | preview (headless PNG) を焼く build 配線 (source set 共有 / standalone Jewel・Compose Desktop / `:icons` / `updatePreview`・`verifyPreview` タスク登録) |
| `references/setup/snapshot.md` | VRT golden の配線 (`snapshots/preview` 置き場 / update=同期・verify=比較 / alpha=255・managed clean ゲート / CI ゲート) |
| `references/analysis-api-testing.md` | 注釈を AA で解析する機能テスト。ジェネリック注釈の型引数解決 / 注釈スタブ fixture / 無関係 logged error の抑制 / 壊れたコードへの耐性 |
| `references/headless-preview.md` | `renderComposeScene` + Jewel standalone + VRT golden。推奨ワークフロー (baseline → verify → 目視 → 人間確認) と自動ゲート |
| `references/ide-integration.md` | tool window / `addComposeTab` / gutter line marker / エディタ追従 / ナビ / PSI 挿入 / lifecycle (stale race・dumb mode) / 性能 (background 化・cancellation・`runCatching` の罠) |
| `references/driver-smoke.md` | 実 IDE を駆動する Driver スモーク。2 層構成 (機能テスト厚く + Driver 薄く) / UI ツリー (locator) の覗き方 / 内部デバッグ AnAction |
| `references/gotchas.md` | Compose Desktop / IntelliJ 固有のハマりどころ (ピンチが来ない・AS vs IDEA build スキュー・描画方針の却下記録) と各罠の索引 |

## 前提条件

- Kotlin プロジェクトと IntelliJ Platform Gradle Plugin (v2)
- ターゲット IDE: IntelliJ Platform 2026.1 (build 261) 系を実証済み。バージョンは各自のターゲット IDE に合わせて調整する
- UI を Jewel/Compose で組む場合は JBR 21 / Compose Desktop / Skiko / standalone Jewel
- Analysis API を使う場合は K2 有効化 (`idea.kotlin.plugin.use.k2` + `supportsKotlinPluginMode supportsK2="true"`)
- 任意: JetBrains MCP (`get_file_problems` 等の事実確認に使える)

## インストール

```sh
gh skill install tbsten/skills intellij-plugin-dev
```
