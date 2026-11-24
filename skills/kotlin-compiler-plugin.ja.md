# Kotlin Compiler Plugin スキル

[English](./kotlin-compiler-plugin.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin K2 Compiler Plugin (FIR + IR) の新規セットアップ、設計相談・レビュー・前例調査、サポート Kotlin バージョンの追加・削除、モジュール横断のコンパイル時収集の scaffold をまとめて扱う [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

旧 `kotlin-compiler-plugin-setup` / `kotlin-compiler-plugin-dev` / `collector-compiler-plugin` の 3 skill を統合したもの。`SKILL.md` は依頼をモードに振り分けるだけで、各モードの手順は `references/<mode>/guide.md` にあるため、エージェントは必要なモードの分だけ読み込む。

## クイックスタート

### 1. スキルをインストール:

```bash
gh skill install tbsten/skills kotlin-compiler-plugin
```

### 2. AI エージェントに依頼:

```
Kotlin compiler plugin のプロジェクトをセットアップして
この変換は FIR と IR どっちでやるべき？似たことをしている既存プラグインはある？
うちの compiler plugin で Kotlin 2.4.0 をサポートしたい
モジュール横断で @Collectable 付きクラスをコンパイル時に集めたい
```

## モード

| モード | こんなときに | guide | 旧 skill |
|---|---|---|---|
| [setup](#setup-モード) | compiler plugin のマルチモジュールプロジェクトを新規作成 (compiler-plugin / gradle-plugin / runtime / kctfork テスト / integration test / CI・release) | `references/setup/guide.md` | `kotlin-compiler-plugin-setup` |
| [dev](#dev-モード) | 設計相談 (FIR と IR、Extension Point の選択)、前例調査、実装レビュー、サポート Kotlin バージョンの追加・削除 | `references/dev/guide.md` | `kotlin-compiler-plugin-dev` |
| [collector](#collector-モード) | Gradle モジュールを横断してアノテーション付きクラスをコンパイル時に収集 (hint 関数パターン) | `references/collector/guide.md` | `collector-compiler-plugin` |

## スキルの構成

```
kotlin-compiler-plugin/
├── SKILL.md                 # モードへの振り分け
├── references/
│   ├── setup/               # setup guide + 設計解説
│   ├── dev/                 # dev guide + 前例調査データ + 複数バージョン対応パターン
│   └── collector/           # collector guide + アーキテクチャ + コードテンプレート
├── scripts/scaffold.sh      # setup: example/ からプロジェクトを生成
├── example/                 # setup: 完全なプロジェクト skeleton
├── assets/                  # dev: 対象プロジェクトにコピーするテスト基盤
└── evals/
```

## setup モード

### 動作の仕組み

スキルは `scripts/scaffold.sh` を実行する。この script は `example/` の完全なプロジェクト skeleton — build ファイルに加え、実パッケージ `com.example.compilerpluginsetup` + `Example` クラス prefix で書かれた Kotlin ソース (CommandLineProcessor、CompilerPluginRegistrar、IR/FIR extension、no-op transformer、kctfork テストヘルパー、integration test のエントリポイント) — をコピーし、プロジェクト名・パッケージ・Group ID・Plugin ID に置換・rename する。エージェントは script をそのまま実行し (ドキュメントからのコード転記はしない)、生成結果をレビューしてビルドを確認する。

### セットアップされるもの

#### プロジェクト構成

| モジュール | 説明 |
|---|---|
| `buildSrc/` | Convention plugins: `kotlin-jvm` (JDK 21 toolchain で Java 17 bytecode、JUnit5 + テストログ設定) と `publish` (vanniktech maven-publish、鍵がある時だけ署名) |
| `compiler-plugin/` | Compiler plugin 本体 (AutoService + KSP、FIR + IR extension、`enabled` CLI オプションを配線済み) |
| `gradle-plugin/` | Gradle plugin ラッパー (KotlinCompilerPluginSupportPlugin)。`Property` + `convention()` の DSL、fail-fast ガード、利用側 Kotlin 版ガード、生成 `BuildConfig` |
| `runtime/` | Kotlin Multiplatform API 宣言 (placeholder の marker 1 つ) |
| `integration-test/test-jvm/` | JVM 単体のエンドツーエンドテスト (`kotlinCompilerPluginClasspath`) |
| `integration-test/test-kmp/` | KMP (JVM + JS) のエンドツーエンドテスト (`kotlinCompilerPluginClasspath`) |
| `integration-test/test-gradle-plugin/` | Gradle TestKit E2E。fixture ビルドが `plugins { id(...) }` で適用し、`includeBuild` + `dependencySubstitution` で本リポジトリから解決する |
| `.github/workflows/` | `ci.yml` (PR のみ cancel、paths-ignore、失敗時 reports upload、`final` 集約 job) と `release.yml` (tag push、tag と版の一致チェック、Maven Central、GitHub Release) |

#### ビルド設定

| ファイル | 説明 |
|---|---|
| `gradle.properties` | `GROUP` / `VERSION_NAME` / plugin ID / 対応 Kotlin 範囲 / `POM_*` の SSoT と、ビルド高速化設定 (caching、configuration cache、parallel、KT-82395 回避) |
| `build.gradle.kts` | gradle.properties から `allprojects { group; version }` を設定 |
| `settings.gradle.kts` | マルチモジュール構成 + foojay toolchain resolver |
| `gradle/libs.versions.toml` | Version catalog (Kotlin, KSP, AutoService, kctfork, Kotest, maven-publish) |
| `buildSrc/build.gradle.kts` | kotlin-dsl + 共有 version catalog |

#### テスト基盤

| コンポーネント | 説明 |
|---|---|
| kctfork (KotlinCompilation) | インメモリコンパイルによるユニットテスト |
| Kotest (FunSpec) | テストフレームワーク (JUnit5 ランナー) |
| Integration test (JVM / KMP) | `kotlinCompilerPluginClasspath` を使うモジュール |
| Gradle plugin sanity | `ProjectBuilder` による DSL 既定値・runtime 依存の配線テスト |
| Gradle plugin E2E | Gradle TestKit + fixture プロジェクト (利用者と同じ書き方のビルド。publish 不要) |

### 主要コンセプト

#### Plugin 登録

Compiler plugin は `META-INF/services/` ファイルで登録される (`@AutoService` で自動生成):
- `CommandLineProcessor` — Plugin ID を宣言
- `CompilerPluginRegistrar` — FIR + IR extension を登録

#### FIR vs IR

- **FIR (Frontend)** — バリデーション、正確な行番号でのエラー報告
- **IR (Backend)** — バイトコード/JS/Native 出力に影響する実際のコード変換

#### Unit Test パターン

kctfork でインメモリコンパイルし、リフレクションで結果を検証:
- `compile(source)` — プラグイン登録済みでコンパイル
- `shouldCompileOk()` — コンパイル成功を検証
- `loadTopLevelField(name)` — クラスローダー経由でフィールド値を取得

### 関連

- [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (MIT, kitakkun 作) — scaffold 後、bootstrap・公式テストインフラ (diagnostic / box テスト。kctfork と併用)・Gradle plugin 連携・複数バージョン対応の guide を参照する。内容はコピーせず raw URL で参照し、ローカルにインストール済みならそちらを優先する
- [dev モード](#dev-モード) — 30+ プラグインの前例調査、FIR/IR Extension 向け kitakkun guide の全トピック対応表、サポート Kotlin バージョンの追加・削除

## dev モード

### できること

1. **前例を探す** — 30+ の既存 compiler plugin から、やりたいことに近い実装を検索する
2. **Extension Point を選ぶ** — 要件に合った FIR/IR Extension Point を提案する
3. **実装をレビュー** — 確立されたパターンとベストプラクティスをもとに実装を評価する
4. **複数バージョン対応をレビュー** — compat module layer / source set separation のアーキテクチャ評価、タンデム vs 独立リリース戦略の判断支援
5. **サポート Kotlin バージョンの追加・削除** — compat module の delegation pattern / capability flag / reflection shim / SSOT 駆動 CI matrix / kctfork version mapping を駆使して新バージョン対応を導入
6. **ソースコードを深掘り** — deepwiki MCP を使って参考プラグインの最新ソースコードを読む

### こんなときに使う

- 新しい compiler plugin プロジェクトを始めるとき
- 既存の compiler plugin に機能を追加するとき
- compiler plugin の実装を正しいか確認・レビューしたいとき
- 特定の機能を既存プラグインがどう実装しているか調べたいとき
- FIR と IR のどちらで処理すべきか判断したいとき
- 既に複数バージョン対応基盤を持つプラグインに新しい Kotlin バージョンを追加 / 削除したいとき

### 動作の流れ

1. **Step 0**: deepwiki MCP が利用可能か確認する（任意。ソースコード探索を強化）
2. **Step 1**: 要件を把握する（開発 / レビュー / 調査 / サポートバージョン追加・削除）
3. **Step 2**: `references/dev/overview.md` を検索 — 30 プラグイン × 130+ Extension Point 実装の一覧表
4. **Step 3**: `references/dev/details/` ファイルで詳細を確認（継承クラス、オーバーライドメソッド、動作、診断）
5. **Step 4**: 必要に応じて deepwiki MCP で最新ソースコードを確認
6. **Step 5**: 設計提案・レビューレポート・調査サマリを出力
7. **Step 6**: サポートバージョン追加・削除モードでは `references/dev/multi-version-workflow.md` を参照しつつ、compat module 追加・SSOT/CI matrix 更新・テスト切り分けを実施

### 同梱リソース

| ファイル | 内容 |
|---|---|
| `references/dev/overview.md` | 30+ プラグインの全 Extension Point をまとめたフラットな一覧表（ソース URL 付き） |
| `references/dev/patterns.md` | 4 つの設計パターン + Extension Point 選択ガイド + Multi-Version Support アーキテクチャ (compat module layer / source set separation) |
| `references/dev/review-checklist.md` | K2 対応・設計パターン・コード品質・複数 Kotlin バージョン対応のチェックリスト |
| `references/dev/details/*.md` | プラグイングループごとのソースコードレベルの詳細（8 ファイル） |
| `references/dev/multi-version-workflow.md` | サポート Kotlin バージョン追加・削除の詳細ワークフロー |
| `references/dev/compat-module-setup.md` | `CompatContext` SPI / delegation pattern / ShadowJar 設定 |
| `references/dev/source-set-separation.md` | source set 分離アプローチの詳細 |
| `references/dev/ci-matrix.md` | SSOT 駆動 dynamic matrix の YAML テンプレ / per-version test script |
| `references/dev/kotlin-tooling-version.md` | `KotlinToolingVersion` with Maturity 比較ロジック |
| `references/dev/version-gating.md` | capability flag の設計 + テストの self-skip |
| `references/dev/reflection-shim.md` | 小さな差分を吸収する reflection shim |
| `references/dev/troubleshooting.md` | 失敗パターン別の原因と対処 |
| `references/dev/release-operations.md` | 運用: 公開 artifact の mavenLocal smoke (`javap`)・CI matrix のローカル並列再現・semver 方針 (Kotlin 版追加 = MINOR / 削除 = MAJOR)・プロジェクト内 skill と `paths` 付き `.claude/rules` の型・テスト戦略マトリクス |
| `references/dev/external-kitakkun-skills.md` | 外部 skill kitakkun/kotlin-compiler-plugin-skills の 24 guide の対応表 (raw URL・インストール方法) と、本 skill の references との使い分け |

### 同梱アセット

複数バージョン対応のテスト基盤が無い対象プロジェクトへそのままコピーする実ファイル (スキルは同等品を手書きせず、これらをそのまま配布する):

| ファイル | コピー先 | 内容 |
|---|---|---|
| `assets/scripts/compiler-plugin-test.sh` | `scripts/` (+ `chmod +x`) | バージョン別テスト実行 script。`--all` で SSOT 全バージョンをループし失敗一覧を出力 |
| `assets/scripts/supported-kotlin-versions.txt` | `scripts/` | サポート Kotlin バージョンの SSOT テンプレート (プロジェクトに合わせて編集) |
| `assets/workflows/compiler-plugin-test.yml` | `.github/workflows/` | SSOT 駆動 dynamic CI matrix (resolve + test job) |
| `assets/scripts/smoke-test.sh` | `scripts/` (+ `chmod +x`) | 任意。publishToMavenLocal → 独立 consumer を指定 Kotlin 版でビルド → `javap -p -c` で注入シンボルを検証 (`--absent` で zero-overhead 検証) |

### 前提条件

- Kotlin プロジェクトと compiler plugin のソースコード（または作成計画）
- サポートバージョン追加・削除を行う場合は、複数バージョン対応基盤 (compat module layer または source set separation) が既に存在すること。基盤の初期セットアップは [setup モード](#setup-モード) (`references/setup/guide.md` の Step 4: Multi-Kotlin Version Support) を参照
- 任意: deepwiki MCP サーバー（設定するとソースコード探索が強化される）

### 関連

- [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (MIT, kitakkun 作) — FIR/IR Extension の API、公式テストインフラ、デバッグ、Gradle 連携、複数バージョン対応のトピック別 guide (Kotlin 2.4.x 基準、Kotlin compiler ソースで裏取り済み)。本 skill の `references/dev/details/` が「どの既存プラグインが同じことをしているか」、kitakkun guide が「現行 API でどう書くか」を担う。内容はコピーせずリンクで参照する (`references/dev/external-kitakkun-skills.md` 参照)

## collector モード

> collector モードは元 `collector-compiler-plugin` skill。[TBSten/collector-compiler-plugin-skill](https://github.com/TBSten/collector-compiler-plugin-skill) から移管され、現在はこのリポジトリで管理している。

### 背景

[metro](https://github.com/ZacSweers/metro) や [koin-compiler-plugin](https://github.com/InsertKoinIO/koin-compiler-plugin) は、Kotlin Compiler Plugin で複数モジュールにまたがるアノテーション付きクラスをコンパイル時に収集・集約している。この仕組みは DI フレームワークに限らず汎用的に有用だが、Compiler Plugin の実装は参入障壁が高い。本スキルはこのパターンを汎用化し、自分のプロジェクトに同様の仕組みを導入できるようにする。KSP では実現できない cross-module 収集をカバーする。

### 動作の仕組み

1. **ヒアリング** — プロジェクトタイプ (KMP / Android / JVM)、収集対象、tag によるグルーピング、base package、アノテーション名
2. **設計確認** — アノテーション名と引数、hint パッケージ (`<base-package>.hints`)、モジュール構成、生成ファイル一覧
3. **コード生成** — `references/collector/architecture.md` を読み、`references/collector/templates/*` をパッケージ名・アノテーション名に合わせてカスタマイズ
4. **動作確認ガイド** — `./gradlew :compiler-plugin:test`、`publishToMavenLocal`、サンプルプロジェクトでの確認

#### 生成されるモジュール

| モジュール | 説明 |
|---|---|
| `annotations/` | `@Collectable` / `@CollectAll` マーカーアノテーション (KMP、runtime 依存) |
| `compiler-plugin/` | `CompilerPluginRegistrar` + FIR / IR extension (JVM、compile-time only) |
| `gradle-plugin/` | Compiler plugin を適用する Gradle plugin |

テストは `kotlin-compile-testing` を使う。

#### ユーザー向け API

```kotlin
@Collectable(tag = "api-handler")
class UserApiHandler : ApiHandler { ... }

// コンパイラが body を生成
@CollectAll(tag = "api-handler")
fun collectApiHandlers(): List<KClass<*>>
```

### 主要コンセプト

#### hint 関数パターン

```
[Module A のコンパイル]
  1. FIR: @Collectable 付きクラスを検出 → hint 関数スタブ生成
  2. IR: hint 関数の body 生成 → registerFunctionAsMetadataVisible() で公開
  → JAR のメタデータに hint 関数が含まれる

[Module B のコンパイル (Module A に依存)]
  3. referenceFunctions(CallableId) で hint パッケージを検索
  4. hint 関数のパラメータ型から収集対象クラスを復元
```

| API | 役割 |
|---|---|
| `CompilerPluginRegistrar` | エントリポイント |
| `FirDeclarationGenerationExtension` | FIR で hint 関数スタブを生成 |
| `IrGenerationExtension` | IR で hint body 生成 + discovery |
| `registerFunctionAsMetadataVisible()` | hint 関数を downstream モジュールに公開 |
| `referenceFunctions(CallableId)` | downstream で hint 関数を検索 |

#### metro vs koin

| 観点 | metro | koin |
|---|---|---|
| hint パッケージ | `metro.hints` | `org.koin.plugin.hints` |
| 関数名 | scope 名由来 (`appScope`) | prefix + metadata (`componentscan_*_single`) |
| メタデータ格納 | パラメータ型のみ | 関数名 + パラメータ名・型 |
| FIR の役割 | nested interface 生成 (DI 固有) | hint 関数スタブ生成 (汎用的) |
| 集約機能 | 除外・置換あり | なし (validation のみ) |
| 核心 API | **同じ** | **同じ** |

### 制約

- **Kotlin 2.3.x+ (K2 compiler) 必須** — K1 では動作しない
- Compiler Plugin API は minor version 間で binary compatible ではないため、Kotlin バージョンアップへの追従が必要
- `registerFunctionAsMetadataVisible()` / `referenceFunctions()` は stable ではないが、metro / koin の両方で使われている
