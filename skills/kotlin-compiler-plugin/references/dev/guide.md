# Kotlin Compiler Plugin Dev

> `kotlin-compiler-plugin` skill の **dev モード** (開発・レビュー・前例調査・サポート Kotlin バージョン追加/削除)。本文中のパスは skill ルート (`${CLAUDE_SKILL_DIR}`) 基準。

Kotlin Compiler Plugin の開発・レビューを、既存プラグインの調査データを基に支援する。

## Step 0: deepwiki MCP の利用可否チェック

スキル起動時にまず deepwiki MCP が利用可能かチェックする。

```
ToolSearch で mcp__deepwiki__ask_question を検索
```

- **利用可能な場合**: 以降のステップで deepwiki MCP を詳細調査に使用する
- **利用不可の場合**: ユーザーに以下を案内する:

> deepwiki MCP が利用できません。deepwiki MCP を設定すると、既存プラグインのソースコードをリアルタイムで深掘りできます。
> 設定方法: https://deepwiki.com のドキュメントを参照してください。
> deepwiki なしでも references/dev/ 内の調査データで十分な情報が得られますので、このまま続行します。

deepwiki がなくても references/dev/ 内のデータで作業を続行する。

## Step 1: ユーザーの要件を把握

以下のいずれかを特定する:

1. **開発モード**: 新しい compiler plugin を作りたい / 既存プラグインに機能を追加したい
2. **レビューモード**: 既存の compiler plugin 実装をレビュー・評価したい
3. **調査モード**: やりたいことに対する前例を探したい
4. **サポートバージョン追加・削除モード**: 既に複数バージョン対応基盤を持つプラグインに新しい Kotlin バージョンを追加 / 削除したい → **Step 6** へ直接ジャンプ

## Step 2: overview.md で前例を検索

`references/dev/overview.md` を読み込み、ユーザーの要件に合致する前例を検索する。

### 検索の観点

- **やりたい機能** → テーブルの「実装されている機能」列でマッチするものを探す
- **使いたい Extension Point** → テーブルの「FIR or IR」列でフィルタ
- **同じパターンのプラグイン** → `references/dev/patterns.md` のパターン分類を参照

該当するプラグインが見つかったら、ユーザーに前例一覧を提示する。

## Step 3: details/ で詳細を確認

Step 2 で見つかった前例について、`references/dev/details/` 内の対応ファイルを読み込む。

| 対象プラグイン | 読むファイル |
|---|---|
| noarg, allopen, sam-with-receiver | `references/dev/details/01-noarg-allopen-sam-with-receiver.md` |
| KSP, atomicfu | `references/dev/details/02-ksp-atomicfu.md` |
| kotlinx.serialization | `references/dev/details/03-kotlinx-serialization.md` |
| Koin, Anvil, Metro, MoshiX, Redacted | `references/dev/details/04-koin-anvil-metro-moshix-redacted.md` |
| DebugLog, Zipline, Arrow Optics, back-in-time, Kondition, suspend-kontext, AspectK | `references/dev/details/05-debuglog-zipline-arrow-kitakkun.md` |
| assign-plugin, jvm-abi-gen, kotlin-dataframe, scripting | `references/dev/details/06-assign-jvm-abi-gen-dataframe-scripting.md` |
| lombok, compose | `references/dev/details/07-lombok-compose.md` |
| parcelize, power-assert, kapt, js-plain-objects | `references/dev/details/08-parcelize-power-assert-kapt-js-plain-objects.md` |

details ファイルには以下が含まれる:
- CompilerPluginRegistrar の登録内容
- 各 Extension の継承クラス・オーバーライドメソッド
- 具体的な動作の詳細
- 診断メッセージの一覧

### 実装時の API 確認 (外部 guide)

選んだ FIR / IR Extension の具体的な書き方 (最新 API・実装手順・テスト/デバッグ方法) を確認するときは、外部 skill kitakkun/kotlin-compiler-plugin-skills の該当トピック guide を読む。トピック対応表・取得方法 (ローカルインストール優先、なければ raw URL)・本 skill の references との使い分けは [`references/dev/external-kitakkun-skills.md`](external-kitakkun-skills.md) を参照。

## Step 4: deepwiki MCP で最新ソースコードを深掘り (任意)

deepwiki MCP が利用可能な場合、details/ の情報だけでは不十分なとき、deepwiki で最新のソースコードを確認する。

### 使い方

overview.md の Source code url 列にある GitHub リポジトリ情報から、deepwiki に質問する:

```
mcp__deepwiki__ask_question
  repo_name: "JetBrains/kotlin"  (または "google/ksp" 等)
  question: "How does FirNoArgConstructorGenerator implement getCallableNamesForClass and generateConstructors?"
```

### deepwiki を使うべきケース

- details/ に含まれる情報が古い可能性がある場合
- 特定のメソッドの最新実装を正確に確認したい場合
- details/ にない周辺コード (テスト、ユーティリティ等) を確認したい場合

### deepwiki を使わなくてよいケース

- Extension Point の選択や設計パターンの判断 → references/dev/ で十分
- どのプラグインが参考になるかの特定 → overview.md で十分

## Step 5: 結果の出力

モードに応じて出力する。

### 開発モードの出力

```markdown
## Compiler Plugin 設計提案

### やりたいこと
- ...

### 推奨する Extension Point
| Extension Point | 役割 | 前例 |
|---|---|---|

### 推奨する設計パターン
- パターン: (1〜4 のいずれか)
- 理由: ...

### 最も参考にすべきプラグイン
- プラグイン名: ...
- 類似度: ...
- 参考ファイル: ... (URL)

### 実装ステップ
1. ...
2. ...
```

### レビューモードの出力

```markdown
## Compiler Plugin レビューレポート

### 概要
- プラグイン名: ...
- 使用 Extension Point: ...
- K2 対応: Yes/No

### Extension Point の選択
- [適切/要改善] FIR Extension: ...
- [適切/要改善] IR Extension: ...

### 設計パターン
- 採用パターン: パターン 1/2/3/4
- 前例との類似度: ... (最も近いプラグイン名)

### チェックリスト結果
(references/dev/review-checklist.md のチェックリストの結果。複数バージョン対応セクションも含める)

### 複数 Kotlin バージョン対応
- バージョニング戦略: タンデム / 独立
- アーキテクチャ: compat module layer / source set separation / 未対応
- 問題点 (minVersion 設定ミス、mergeServiceFiles 漏れ、fail-fast 設定等):

### 問題点・改善提案
1. ...
2. ...

### 前例参照
- 類似プラグイン: ... (URL)
- 参考にすべきファイル: ... (URL)
```

### 調査モードの出力

前例の一覧と、各前例の概要・ソースコード URL を提示する。

## Step 6: サポート Kotlin バージョンの追加・削除

既に複数バージョン対応基盤 (compat module layer または source set separation) を持つ Kotlin Compiler Plugin プロジェクトに対して、サポート対象 Kotlin バージョンを追加・削除する。基盤の初期セットアップは setup モード (`references/setup/guide.md` の Step 4: Multi-Kotlin Version Support) を参照。

### 概要

- **A: Compat Module Layer** — `compiler-plugin/compat-kXX/` 形式のモジュールがあり、ServiceLoader で実装を選択して ShadowJar で 1 jar にバンドルする構成
- **B: Source Set Separation** — `src/v2_0_0/kotlin` / `src/pre_2_0_0/kotlin` 等のディレクトリで Gradle が動的に切り替える構成

### ワークフロー

詳細手順 (要件確認 → 既存 module で対応可能か判定 → 新 compat module 追加 (delegation pattern) → SSOT 更新 → CI matrix / kctfork / Compose マップ / README 更新 → テスト切り分け → ドキュメント反映) は **[`references/dev/multi-version-workflow.md`](multi-version-workflow.md)** を参照。

### テスト基盤の配布 (assets)

対象プロジェクトに以下が無ければ、本 skill の `assets/` からコピーする。**script は読解・書き換え・再実装せず、そのまま実行する** (SSOT のバージョン一覧のみプロジェクトに合わせて編集する):

| 対象プロジェクトのパス | コピー元 | 備考 |
|---|---|---|
| `scripts/compiler-plugin-test.sh` | `assets/scripts/compiler-plugin-test.sh` | コピー後 `chmod +x`。`<version>` 単体 / `--all` (SSOT 全バージョン + 失敗一覧) |
| `scripts/supported-kotlin-versions.txt` | `assets/scripts/supported-kotlin-versions.txt` | SSOT。実際のサポートバージョンに合わせて編集 |
| `.github/workflows/compiler-plugin-test.yml` | `assets/workflows/compiler-plugin-test.yml` | 既存 `pull-request.yml` への job マージも可 |
| `scripts/smoke-test.sh` | `assets/scripts/smoke-test.sh` | 任意。publishToMavenLocal → 独立 consumer を対象 Kotlin でビルド → `javap` で注入シンボル検証 (`--absent` で zero-overhead 検証)。使い方は `references/dev/release-operations.md` |

### よく使う詳細リファレンス

| 用途 | ファイル |
|---|---|
| `CompatContext` SPI / delegation / ShadowJar 設定 | [`references/dev/compat-module-setup.md`](compat-module-setup.md) |
| Source set 分離アプローチ | [`references/dev/source-set-separation.md`](source-set-separation.md) |
| SSOT 駆動 CI matrix の YAML テンプレ | [`references/dev/ci-matrix.md`](ci-matrix.md) |
| Beta/RC/Stable 比較 (Maturity) | [`references/dev/kotlin-tooling-version.md`](kotlin-tooling-version.md) |
| capability flag の設計 / self-skip | [`references/dev/version-gating.md`](version-gating.md) |
| reflection shim (小さな差分の吸収) | [`references/dev/reflection-shim.md`](reflection-shim.md) |
| 失敗パターン別の原因と対処 | [`references/dev/troubleshooting.md`](troubleshooting.md) |
| 公開 artifact の smoke / ローカル並列 matrix / semver (Kotlin 版追加=MINOR・削除=MAJOR) / プロジェクト内 skill・rule の型 | [`references/dev/release-operations.md`](release-operations.md) |
| API 差分の吸収方針 (併読) | kitakkun の `multi-version-kotlin-support` guide ([`references/dev/external-kitakkun-skills.md`](external-kitakkun-skills.md)) |

### サポートバージョン追加・削除モードの出力

```markdown
## Kotlin X.Y.Z サポート追加レポート

### 対象
- バージョン: X.Y.Z (stable / Beta / RC / dev)
- operation: 追加 / 削除 / 置換
- アーキテクチャ: A: Compat Module Layer / B: Source Set Separation

### 実施内容
- [ ] SSOT (`scripts/supported-kotlin-versions.txt`) 更新
- [ ] compat module / ソースセット 追加・修正 (該当時)
- [ ] CI matrix 確認 (`fail-fast: false`、SSOT 駆動)
- [ ] kctfork バージョンマップ更新 (該当時)
- [ ] Compose マップ更新 (KMP/CMP プロジェクトの場合)
- [ ] README 両言語 / `docs/support-kotlin-versions.md` 反映
- [ ] 全バージョンでテスト GREEN

### 問題・注記
- (例: 新 API 境界が必要だった理由、shim/capability flag の追加 etc)
```
