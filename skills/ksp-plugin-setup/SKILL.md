---
name: ksp-plugin-setup
description: >
  KSP plugin (Symbol Processor) プロジェクトを cream.kt 由来の構成で一式セットアップする。
  runtime (KMP 全ターゲット、アノテーション宣言のみ) / ksp (JVM only, processor) / test (KMP 統合テスト)
  の 3 モジュール、feature / core / options / util の 4 層 + ProcessContext + context parameters による
  processor 設計、kctfork による e2e コンパイルテスト・facet 形式の Markdown golden・generator 駆動の
  snapshot matrix・Konsist によるレイヤ自動強制、version catalog / buildLogic / KSP×KMP workaround /
  GitHub Actions matrix CI を含む。生成先プロジェクトに .claude/rules/*.md を配置して規約を常設する。
  Use when requested: "KSP プラグインを作りたい", "KSP processor のプロジェクトをセットアップ",
  "setup ksp plugin", "cream と同じ構成で KSP プラグインを作って", "symbol processor のプロジェクト構成",
  "KSP プラグインのテスト基盤を整えたい", "アノテーションプロセッサを新規作成".
metadata:
  status: Experimental
  group: Kotlin ライブラリ/ツール開発
---

# KSP Plugin Setup

KSP plugin プロジェクトを、実運用に到達した構成 ([cream.kt](https://github.com/TBSten/cream) 由来)
で一式セットアップする。スキャフォールドするだけでなく、生成先に `.claude/rules/*.md` を置いて
以後も構成が崩れないようにする。

## Usage

### 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト名** — ルートプロジェクト名 (kebab-case)。モジュール名 `<project-name>-runtime` /
   `<project-name>-ksp` の接頭辞になる
2. **パッケージ名 / Group ID** — 例: `com.example.myplugin`
3. **最初のアノテーション名** — 例: `@Greeting`。feature ディレクトリ名の由来になる
4. **セットアップ範囲** — 以下から選択 (デフォルト: 全て)
   - [x] Gradle 基盤 (settings / version catalog / buildLogic / gradle.properties)
   - [x] runtime モジュール (KMP 全ターゲット)
   - [x] ksp モジュール (processor 骨組み: root 3 ファイル + feature / core / options / util)
   - [x] test モジュール (KMP 統合テスト)
   - [x] テスト基盤 (kctfork + facet snapshot + generator + Konsist)
   - [x] CI (GitHub Actions matrix)
   - [x] `.claude/rules/` (規約の常設)
5. **Kotlin / KSP バージョン** — デフォルトは `example/gradle/libs.versions.toml` の値

### example のプレースホルダー

`example/` 配下をコピーするときに置換する。

| プレースホルダー | 置換先 | 出現箇所 |
|---|---|---|
| `<project-name>` | プロジェクト名 | build ファイル、CI、rules |
| `<group-id>` | Maven groupId | build ファイル |
| `com.example.ksppluginsetup` | パッケージ名 | Kotlin ソース |
| `ksppluginsetup.` | KSP option / snapshot 更新プロパティの接頭辞 | Kotlin ソース |
| `Example` (`ExampleSymbolProcessor` 等) | プロジェクト名の PascalCase | Kotlin ソース |
| `Greeting` / `greeting` / `greetingFun` | 最初のアノテーション名 | Kotlin ソース |
| `<owner>` / `<repo>` / `<year>` | GitHub 情報 | publish 設定、issue リンク |

### example のディレクトリ対応

`example/` はソースセットのパスを省いた形で置いてある。コピー先は以下。

| example 内 | コピー先 |
|---|---|
| `example/runtime/*.kt` | `<project-name>-runtime/src/commonMain/kotlin/<package>/` |
| `example/ksp/main/**` | `<project-name>-ksp/src/main/kotlin/<package>/ksp/**` |
| `example/ksp/test/**` | `<project-name>-ksp/src/test/kotlin/<package>/ksp/**` |
| `example/test/GreetingTestData.kt` | `test/src/commonMain/kotlin/<package>/test/<name>/` |
| `example/test/GreetingTest.kt` | `test/src/commonTest/kotlin/<package>/test/<name>/` |
| `example/ksp/META-INF-services.txt` | 中身を `<project-name>-ksp/src/main/resources/META-INF/services/...` へ |
| それ以外 (build ファイル / CI) | 同じ相対パス |

## セットアップ手順

### Step 1: Gradle 基盤

`example/` から以下をコピーし、プレースホルダーを置換する。

```
<project-root>/
├── settings.gradle.kts          # foojay resolver + includeBuild("./buildLogic") + 3 モジュール
├── build.gradle.kts             # allprojects で group / version (version catalog 由来)
├── gradle.properties            # configuration-cache + caching + ksp.incremental=false
├── gradle/libs.versions.toml    # 自プロジェクトの version も含む SSoT
└── buildLogic/
    ├── settings.gradle.kts      # 親の catalog を共有 + foojay resolver (両方に必要)
    └── convention/              # 最小の convention plugin (lint のみ)
```

要点 3 つ。詳細と落とし穴は references/build-and-ci.md を参照:

- **version catalog がすべての SSoT**。自プロジェクトの version も catalog に置く
- **KSP のバージョンは `<kotlin>-<ksp>` 形式**。Kotlin と必ずセットで上げる
- **foojay resolver は root と buildLogic の両方に**書く。片方だけだと included build の
  `jvmToolchain(17)` が "No matching toolchains" で落ちる

### Step 2: runtime モジュール

`example/runtime/build.gradle.kts` と `example/runtime/Greeting.kt` をベースに作成する。

- **アノテーション宣言のみ**。実行時ロジックを 1 行も置かない (だから全ターゲットで publish できる)
- KMP 全ターゲット + `explicitApi()`
- API 設計の規約 (SOURCE retention の統一、統一サーフェス、トークン定数、`.Map` / `.Exclude` の
  入れ子) は references/processor-design.md を参照

### Step 3: ksp モジュール (processor)

`example/ksp/build.gradle.kts` と `example/ksp/main/` 配下をベースに作成する。

```
<project-name>-ksp/src/main/kotlin/<package>/ksp/
├── <Name>SymbolProcessor.kt          # composition root: option パース + feature の dispatch のみ
├── <Name>SymbolProcessorProvider.kt  # KSP provider
├── ProcessContext.kt                 # {resolver, options, codeGenerator, logger} (leaf)
├── feature/<name>/Process<Name>.kt   # 1 注釈 = 1 ディレクトリ = 1 エントリ関数
├── core/common/ ・ core/<family>Fun/ ・ core/error/
├── options/                          # option の data class + パース
└── util/ ・ util/ksp/                # 汎用ヘルパのみ
```

守るべき境界:

- **root 直下は 3 ファイルのみ**。生成ロジック・ヘルパ・例外を置かない
- **依存は feature → core → util の一方向**。唯一の上向きは `feature → ProcessContext`
- **feature 間の依存は禁止**。共有したくなったら core へ降ろす
- **`feature/` 直下・`core/` 直下に `.kt` を置かない**。必ずサブディレクトリへ
- **層ごとに context を絞る**。feature は `context(ctx: ProcessContext)`、core は
  `context(options: <Name>Options, logger: KSPLogger)`

`<project-name>-ksp/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
に provider の FQN を 1 行書く (`example/ksp/META-INF-services.txt` 参照)。

生成ロジック・診断・option・例外階層の設計判断は references/processor-design.md を参照。

### Step 4: test モジュール (KMP 統合テスト)

`example/test/build.gradle.kts` をベースに作成し、`example/test/GreetingTestData.kt` /
`GreetingTest.kt` の形で commonMain (注釈付き入力) と commonTest (検証) をファイル 1:1 で対応させる。

**KSP × KMP の workaround が必須**。`kspCommonMainKotlinMetadata` の生成物を commonMain の srcDir に
足し、他の ksp タスクをそれに依存させる。ただし **`*Test` の ksp タスクは無効化しない**
(kotest の per-target launcher 生成に必要)。詳細は references/build-and-ci.md。

### Step 5: テスト基盤

`example/ksp/test/testing/` 配下をコピーする。依存順は compile → snapshot → generator → poet →
konsist。

- **kctfork** で実コンパイル。`inheritClassPath = false` + 明示 classpath (速度)、`useKsp2()`、
  TeeOutputStream でコンソール出力を捕捉
- **facet 形式の Markdown golden**。入力 / options / ExitCode / コンソール / 生成物を 1 ファイルに束ねる
- **generator 駆動の snapshot**。KotlinPoet でシナリオを組み、option 軸と `cartesian` で直積を取る。
  option の代表値は `withRepresentativeValues` で絞る (全直積は組み合わせ爆発する)
- **診断も golden で固定**。エラーメッセージ本文そのものを ExitCode と併せて比較する
- **Konsist** で層・root 許可ファイル・1 ファイル行数上限を import ベースで自動強制

`test` タスクには 4 点が必須 (`useJUnitPlatform()` / `maxHeapSize` / `forkEvery` / `-D` の
systemProperty 明示転送)。理由と各ファイルの役割は references/testing.md を参照。

### Step 6: CI

`example/.github/workflows/gradle.yml` をコピーする。`matrix.include` で OS を最小化し
(Apple ターゲットだけ macOS)、`concurrency` / `timeout-minutes` / wrapper-validation を含む。

publish が必要なら `example/.github/workflows/publish.yml` をコピーする。GPG 鍵・secrets・
Sonatype 登録まで含む完全な手順は **`kotlin-maven-central-publish` スキル**に委譲する。

### Step 7: 規約の常設 (`.claude/rules/`)

`assets/rules/` の 5 ファイルを生成先の `.claude/rules/` にコピーし、`<project-name>` を置換する。

| ファイル | 対象パス | 内容 |
|---|---|---|
| `ksp-architecture.md` | ksp モジュール全体 | 層と依存方向テーブル (SSoT)、context parameters、命名、注釈追加手順 |
| `ksp-top-level.md` | `ksp/*.kt` | root 直下に置いてよい 3 ファイル |
| `ksp-feature-top-level.md` | `ksp/feature/*.kt` | feature 直下禁止、feature がやること / やらないこと |
| `ksp-core-top-level.md` | `ksp/core/*.kt` | core のサブパッケージ一覧、`GenerateSourceAnnotation` パターン |
| `ksp-test.md` | test 系 | 3 系統のテスト、feature 5 種、golden 形式、命名規約 |

これらは Konsist テストと対になっている。**依存方向テーブルを変えたら同じコミットで
ArchTest も更新する**旨を、プロジェクトの CLAUDE.md にも書いておく。

### Step 8: ビルド確認

```sh
./gradlew :<project-name>-ksp:test          # kctfork + Konsist
./gradlew :<project-name>-ksp:test -D<project-name>.snapshot.update=true   # golden 初回記録
./gradlew jvmTest                           # test モジュールの振る舞い検証
./gradlew ktlintCheck
```

golden の初回記録後は **必ず中身を読んでからコミットする**。最初の記録が誤った出力を捕まえる
唯一の機会で、以降は差分しか見えなくなる。

## リソース

| リソース | いつ読むか |
|---|---|
| references/build-and-ci.md | Gradle 基盤・KSP×KMP workaround・kotest 配線・CI・publish の詳細と落とし穴 |
| references/processor-design.md | example を自分のアノテーションに置き換える時、生成 / 診断 / option の設計判断 |
| references/testing.md | テスト基盤の各ファイルの役割、feature のテストを追加する時、Konsist の注意点 |
| assets/rules/*.md | 生成先の `.claude/rules/` にコピーするテンプレート (規約の SSoT) |
| example/ | コピー元一式 |

## セットアップ完了メッセージ

```
## セットアップ完了

### モジュール構成
- <project-name>-runtime/ (KMP 全ターゲット、アノテーション宣言のみ)
- <project-name>-ksp/ (JVM only、feature / core / options / util)
- test/ (KMP 統合テスト)

### テスト基盤
- kctfork e2e / facet golden / generator 駆動 snapshot / Konsist アーキテクチャテスト

### 規約
- .claude/rules/ に 5 ファイル配置済み

### ビルド結果
- :<project-name>-ksp:test: [SUCCESS / FAILED]
- jvmTest: [SUCCESS / FAILED]
- ktlintCheck: [SUCCESS / FAILED]

### 次のステップ
1. runtime に自分のアノテーションを宣言する
2. core/common に <Name>SourceAnnotation を追加する
3. feature/<name>/Process<Name>.kt を書き、SymbolProcessor に dispatch を 1 行足す
4. feature の 5 種テストを追加し、golden を記録する
```
