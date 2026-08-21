---
name: intellij-plugin-dev
description: >
  IntelliJ Platform / Android Studio 向けプラグインを、人手を最小化してエージェント主導で
  開発するためのツーリング・進め方リファレンス。「実装↔動作確認の自走フィードバックループ」を
  6 チャネルに分解し、正しさ = ヘッドレス機能テスト (Kotlin Analysis API)、見た目 = headless PNG
  自己目視 (renderComposeScene + standalone Jewel) の 2 本を主軸に据える。tool window (Jewel/Compose)、
  gutter line marker、エディタ追従、ノード→ソースのナビ、PSI 挿入 (コード生成)、headless preview + VRT
  golden、Driver スモーク、build/since-until 配線をカバーする。example scaffold 一式
  (scripts/scaffold.sh で新規プラグインの土台を生成) を同梱。
  Use when requested: "IntelliJ プラグインを作りたい", "IntelliJ Platform plugin dev",
  "Jewel/Compose で tool window", "addComposeTab", "Analysis API がテストで動かない",
  "注釈の型引数が取れない", "renderComposeScene で見た目確認", "VRT golden を組む",
  "gutter line marker が出ない", "エディタ追従 / ノード→ソースのナビ", "PSI 挿入でコード生成",
  "プラグインの build 配線 / since-until", "Driver スモーク", "AS vs IDEA の build スキュー".
metadata:
  status: WIP
  group: Kotlin ライブラリ/ツール開発
---

# IntelliJ Platform プラグイン開発の進め方 (エージェント主導)

IntelliJ / Android Studio 向けプラグインを **人手を最小化してエージェントで回す** ための
ツーリングと進め方のリファレンス。Jewel/Compose の tool window + Kotlin Analysis API frontend +
gutter line marker 構成のプラグインを 2026-07 に実装して確定した知見のスナップショット。

新規のプラグイン機能を作る・ビルド/テスト基盤を組む・動作確認の回し方を決めるとき、または
「AA がテストで動かない」「見た目を確認したい」「gutter が出ない」で詰まったときに、該当する
reference だけを開いて使う。

## 中心思想: 実装↔動作確認の自走フィードバックループ

IDE プラグインは「実装したものを IDE に入れて手で触る」以外の検証手段が揃っているか不透明で、
**フィードバックループを組めるかどうかが最初のリスク**になる。結論は、次の 6 チャネルに分解すれば
大半をエージェント (人手なし) で回せる。**「正しさ = ヘッドレス機能テスト」「見た目 = headless PNG
自己目視」の 2 本を主軸**にし、実 IDE 起動系は毎回ではなく定期チェックポイントに置く。

| ループ | 手段 | 自走 | 位置づけ |
|---|---|---|---|
| コンパイル | `./gradlew buildPlugin` / `compileKotlin` | ✅ | 最速の生存確認 |
| **正しさ (主軸)** | `BasePlatformTestCase` + Analysis API → `./gradlew test` | ✅ | テキスト・決定的。AA→model→描画対象/ナビ先をアサート |
| 純ロジック | レイアウト座標 / lowering / hit-test を素の JUnit | ✅ | platform 不要・最速 |
| **見た目 (主軸)** | Compose/Jewel UI を `renderComposeScene` で PNG に焼き、エージェントが画像として目視 | ✅ | headless。重なり/はみ出し/色/配置を検知。VRT golden 化も可 |
| 実 IDE スモーク | Driver `testIdeUi` (headful・Xvfb) | ⚠ 重い | CI・定期チェックポイント向け。毎イテレーション不向き |
| 実機 AS 追従 | AS へ zip 導入して手動 | ❌ 人間 | build 番号スキュー・テーマ・美観サインオフ |

### 自走の要点

- **「正しさ = 機能テスト (text)」と「見た目 = PNG 自己目視 (headless)」の 2 本**で、実装→検証→修正を
  人手なしで回せる。live IDE / Driver / AS は定期チェックポイント (毎回ではない)。
- **PNG 目視で検知できる異常**: 重なり・はみ出し・色・ラベル衝突・矢印・部品配置。
- **検知できない** = 対話/タイミング (→ Driver スモーク)・最終的な美観 (→ 人間)。この 2 つだけを
  人間/実機のチェックポイントに寄せ、それ以外は test + PNG で閉じる。
- 各主軸の実体: 見た目 = `references/headless-preview.md`、機能テスト (AA) =
  `references/analysis-api-testing.md`、Driver = `references/driver-smoke.md`。

### 詰まりやすいゲートと解消 (実証済み)

当初の未確定リスクと、実際にどう解消したか。同種のプラグインで同じ順に潰していける。

1. **AA をテスト内で回せるか** → ✅ `BasePlatformTestCase` 内で `analyze{}` が回る (K2 は
   systemProperty で有効化)。詳細レシピは `references/analysis-api-testing.md`。
2. **ジェネリック注釈 `@YourGenericAnno<A>` の型引数を取れるか** (最大リスク) → ✅ 注釈 PSI の型引数を
   `analyze { typeRef.type }` で解決すれば確定的に取れる。
3. **fixture の stdlib / 注釈依存** → ✅ 注釈スタブを fixture 内ソースで同梱すれば足りる
   (`KClass` も解決。デフォルト light fixture の stdlib に載っており明示追加不要だった)。
4. **見た目を headless で焼けるか** → ✅ `renderComposeScene` + standalone Jewel で PNG 化に成功
   (素 Compose と Jewel Int UI の両方)。
5. **実 IDE Driver の配線** → Driver スモークは実 UI ができてから薄く 1 本、が現実的。

### 旧案 (supersede 済み)

- 自前 `Graphics2D` canvas / plain Swing プレビュー / JB* コンポーネントを test で描画 →
  **Jewel (Compose) standalone + `renderComposeScene` 採用で全て supersede**。plain Swing は LaF が
  実 IDE と非忠実になる問題があり、Jewel standalone はテーマ忠実なので採用。
- Remote Robot (レガシー UI テスト) は不採用。実 IDE 駆動は Driver (`testIdeUi`) を使う。

## example scaffold (実ファイルの SSoT) と scripts/scaffold.sh

references の snippet を整合する 1 プロジェクトに束ねた実ファイルが `example/`
(パッケージ `com.example.plugin`、独立 Gradle ビルド一式: build 配線 / plugin.xml /
tool window + 共有 Composable / `PreviewMain.kt`+`PreviewChecks.kt` / AA テスト harness)。
**新規プラグインの土台は snippet の転記で再構築せず、`scripts/scaffold.sh` を実行して
example から生成する** (example がコード片の SSoT。references は設計解説と実ファイルへの参照)。

```sh
skills/intellij-plugin-dev/scripts/scaffold.sh \
  --dest <plugin-module-dir> \
  --package com.acme.myplugin \
  --plugin-id com.acme.myplugin \
  --plugin-name "My Plugin" \
  [--dry-run] [--force]
```

- **script は読解・書き換え・再実装せず、そのまま実行する**。失敗したら stderr の
  `ERROR / why / fix` に従って引数を直して再実行する。既存ファイルは `--force` を明示しない
  限り上書きしない (冪等)。末尾 1 行の JSON (`{"ok":true,...}`) で成否を判定する。
- 生成後にやること:
  1. `// CUSTOMIZE` / `TODO(CUSTOMIZE)` マーカーを自分のプラグインに合わせて埋める
     (UI 本体 = `src/shared/.../ui/`、preview matrix = `PreviewMain.kt` の `scenarios`、
     表示名や説明 = `plugin.xml`、Compose Multiplatform 版 = `gradle/libs.versions.toml`)。
  2. Gradle wrapper は同梱していない → 既存 wrapper を使うか `gradle wrapper` で生成。
  3. `./gradlew buildPlugin` (初回は SDK DL で ~4〜5 分) → `./gradlew test` →
     `./gradlew updatePreview` で golden を初回生成し `snapshots/preview/` を commit。
     以後の日々の回し方は `references/headless-preview.md` の「推奨ワークフロー」。
- Driver 層 (`references/driver-smoke.md`) は example でも未配線の雛形のまま (推奨レシピ)。

## reference の索引

必要な観点だけ開く。コード片の実体 (SSoT) は上記 `example/`。

| ファイル | いつ読む |
|---|---|
| `references/setup/basics.md` | プロジェクトの基本 build 配線 (intellijPlatform / SDK 261 / bundled Kotlin(AA)・Jewel・Compose・Skiko / JBR21 / K2 / since-until)。`compileKotlin`・`buildPlugin`・`runIde` が通るまで |
| `references/setup/preview.md` | preview (headless PNG) を焼くための build 配線 (source set 共有 / standalone Jewel・Compose Desktop / `:icons` / `updatePreview`・`verifyPreview` タスク登録) |
| `references/setup/snapshot.md` | VRT golden の配線 (`snapshots/preview` 置き場 / update=同期・verify=比較 / alpha=255・managed clean ゲート / CI ゲート) |
| `references/analysis-api-testing.md` | `@YourSpec` 等を AA で解析する機能テストを書く / 「AA がテストで動かない」「型引数が取れない」「無関係な logged error で落ちる」で詰まった |
| `references/headless-preview.md` | UI (Jewel/Compose) の見た目を IDE 起動なしで確認したい / preview harness と VRT golden ゲートを組む |
| `references/ide-integration.md` | tool window / `addComposeTab` / gutter line marker / エディタ追従 / ノード→ソースのナビ / PSI 挿入 (コード生成) を組む / lifecycle (stale race・dumb mode・invalidation)・性能 (background 化・cancellation・`runCatching` の罠) で詰まった |
| `references/driver-smoke.md` | 実 IDE を駆動する E2E スモークを足す / 実 IDE の UI ツリー (locator) を覗く / 内部デバッグ AnAction を作る |
| `references/gotchas.md` | Compose Desktop / IntelliJ 固有のハマりどころ (ピンチが来ない・AS vs IDEA build スキュー等) |
