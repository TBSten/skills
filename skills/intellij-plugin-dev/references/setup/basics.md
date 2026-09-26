# 基本セットアップ (build & runIde できるまで)

プラグインが `compileKotlin` / `buildPlugin` / `runIde` まで通る最小の gradle 構成。IntelliJ Platform
Gradle Plugin (v2) 前提。preview / snapshot / test 固有の配線は `setup/preview.md` / `setup/snapshot.md`
と各 usage reference (`analysis-api-testing.md` / `driver-smoke.md`) 側。

実ファイル (SSoT): `example/build.gradle.kts` / `example/settings.gradle.kts` /
`example/gradle/libs.versions.toml` / `example/gradle.properties`。**snippet を転記して再構築せず、
`scripts/scaffold.sh` で example から生成する** (SKILL.md「example scaffold」)。

## バージョンの塊 (実証済み)

SSoT は `example/gradle/libs.versions.toml` (scaffold 後は生成先の toml)。塊ごと更新する。

| 項目 | 値 | 備考 |
|---|---|---|
| Gradle (wrapper) | `9.6.0` | wrapper は同梱していない。既存 repo の `gradlew` + `gradle/wrapper/` をコピーでよい |
| IntelliJ Platform Gradle Plugin | `2.18.1` | version は `settings.gradle.kts` 側に書く (下記)。新しい版 (`2.19.0`) があると `is outdated` 警告が出るが、2.18.1 で通る |
| ビルド SDK | `intellijIdea("2026.1")` = build **261** | IU ブランチに解決 (`IU-261.22158.x`) |
| Kotlin | `2.3.0` | **261 同梱 Kotlin 以下**でビルドする制約 |
| Compose Compiler | `org.jetbrains.kotlin.plugin.compose` `2.3.0` | Kotlin と揃える |
| Compose Multiplatform | `1.9.0` | preview (standalone Compose Desktop) 専用。Kotlin `2.3.0` との組み合わせで確認済み |
| Jewel (standalone) | `0.37.0` + `jewelForIde` `261.26222.65` | preview 専用 (`setup/preview.md`) |
| jvmToolchain | **21** (JBR 21) | マシン既定 java が 17 なら `JAVA_HOME` / `-Dorg.gradle.java.home` で明示 |

上の組み合わせで `buildPlugin` / `test` / `updatePreview` が Gradle の非推奨警告なしで通ることを確認済み
(出る警告は IntelliJ Platform Gradle Plugin の「outdated: 2.18.1 → 2.19.0」だけ)。

- **Compose Multiplatform の版を動かすとき**: 公式に「CMP の版ごとの最低 Kotlin 版」の表は無い。確かめる先は
  (1) Compose Multiplatform ドキュメントの「Kotlin compatibility」の節 (Kotlin 2.1.0 以上 / Compose Compiler
  plugin を Kotlin と同じ版にする) と (2) standalone Jewel (`jewel` / `jewelForIde`) が前提にしている Compose の版。
  CMP は preview 専用なので、plugin 本体の Compose (IDE 同梱) とは独立に決めてよい。

## 独立ビルドにする理由

`<plugin-module>/` は自前 `settings.gradle.kts` を持つ **独立ビルド**。1 つの Gradle ビルド内で
Kotlin Gradle Plugin を複数版併用できず、プラグインは「ターゲット IDE 同梱の Kotlin 以下」で
ビルドせねばならないため、本体ライブラリ (別の Kotlin 版) と隔離する。root の通常サブプロジェクト
化は Kotlin 単一版強制のため却下された。

- **cwd に注意**: 独立ビルドなので `./gradlew test` / `updatePreview` / `buildPlugin` / `runIde` 等は
  **`<plugin-module>/` をカレントにして実行**する (リポジトリ root で叩くと別の root ビルドが走り、
  これらの task は存在しない)。各 reference のコマンド例も同じ前提。root/CI からは
  `./gradlew -p <plugin-module> ...` で明示的に呼ぶ (`setup/snapshot.md` の CI ゲート参照)。

## 統合ディストリの罠 (2025.3 / build 253 以降)

- **`intellijIdeaCommunity("2026.1")` は解決不可**。253 以降 IntelliJ IDEA Community は個別配布されず
  統合 IDEA に一本化されたため。→ `intellijIdea("2026.1")` (IU ブランチ) を使う。Android SDK / Java
  plugin に依存しない要件はこれで満たせる。
- **settings 側の import**: `settings.gradle.kts` の `dependencyResolutionManagement { repositories {
  intellijPlatform { defaultRepositories() } } }` は拡張関数なので、ファイル先頭に
  `import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform` が要る。無いと
  Unresolved reference で初回の `buildPlugin` から落ちる (example は import 済み)。
- **plugin version 衝突**: `settings.gradle.kts` に `org.jetbrains.intellij.platform.settings` の
  version を書いたら、`build.gradle.kts` 側は `id("org.jetbrains.intellij.platform")` を
  **version 無指定** で適用する (両方に version を書くと classpath 衝突で落ちる)。

## dependencies (要点)

実体は `example/build.gradle.kts` の `dependencies { intellijPlatform { ... } }` (SSoT)。構成の意図:

- `intellijIdea("2026.1")` — build 261 (AS Quail 2026.1.1 と同世代)。
- `bundledPlugin("org.jetbrains.kotlin")` — Analysis API (K2) 同梱 = 追加依存なしで
  `analyze{}`/`KaSession` が載る。
- `bundledModule(...)` ×6 (jewel.foundation / jewel.ui / jewel.ideLafBridge /
  compose.runtime.desktop / compose.foundation.desktop / skiko) — Jewel/Compose/Skiko は 261
  バンドルを引く (自前 Compose を持たない)。`compose.foundation.desktop` は compile classpath に
  runtime を伝播しないので明示。
- `testFramework(TestFrameworkType.Platform)` — `BasePlatformTestCase` など (詳細は
  `analysis-api-testing.md`)。
- `testImplementation("junit:junit:4.13.2")` — 2.0.0-rc1 以降 Platform は JUnit4 を供給しない
  (`BasePlatformTestCase` は JUnit4 系)。

- `plugin.xml` 側にも同名モジュールを `<dependencies><module name="..."/>` で宣言し、plugin
  classloader から解決させる (`bundledModule(...)` と対を成す)。plugin.xml の最小登録は
  `ide-integration.md`。
- **AA jar の実体**: `bundledPlugin("org.jetbrains.kotlin")` の `plugins/Kotlin/lib/kotlin-plugin.jar`
  に `org.jetbrains.kotlin.analysis.api.*` が同梱されている。
- **stdlib は同梱しない**: `gradle.properties` に `kotlin.stdlib.default.dependency=false` を必ず入れる。
  無いと Kotlin stdlib が自動依存に入り、IDE 同梱版との重複や `verifyPluginProjectConfiguration` の警告に
  なる (stdlib は platform が供給する)。

## K2 強制と since/until build

実体は `example/build.gradle.kts` の `kotlin {}` / `intellijPlatform {}` / `tasks.test` (SSoT)。要点:

- `kotlin { jvmToolchain(21) }` (JBR 21)。
- `buildSearchableOptions = false` — 小さい plugin は省く (headless IDE 起動を避ける)。
- `sinceBuild = "261"` (floor) / `untilBuild = provider { null }` — AS の追従遅れ・将来の 261.x を
  締め出さない (下記の注意)。
- `tasks.test { systemProperty("idea.kotlin.plugin.use.k2", "true") }` — AA を K2 で動かす。
  test に `useJUnitPlatform()` を付けない (JUnit4)。

- K2 対応宣言は `plugin.xml` の `<supportsKotlinPluginMode supportsK2="true"/>` も必要。
- **`untilBuild = null` は上限無し = 全ての将来 build に互換と宣言する**ことに注意 (コメントの「261.x」
  だけでなく 262 以降も含む)。この plugin モジュール (`<plugin-module>`) は AS の build 番号追従遅れを締め出さないため意図的に
  上限無しにしているが、AA / bundled Jewel・Compose module は platform 更新の影響を受け
  やすい。上限無しにするなら **`verifyPlugin` (Plugin Verifier) を CI ゲートに入れ、current/recommended
  IDE への互換を検証**する運用とセットにする (でないと将来 IDE でロード不能になりうる)。互換を 261 系に
  絞るなら `untilBuild = "261.*"`。example は `intellijPlatform { pluginVerification { ides { recommended() } } }`
  を配線済みなので、CI に `./gradlew verifyPlugin` を足すだけでよい (推奨 IDE をダウンロードするので
  毎ビルドではなく CI で回す)。

- SDK 実体 (~1GB) は `~/.gradle/caches/<gradle>/transforms/.../idea-2026.1-.../` に展開・キャッシュ。
  プロジェクト直下 `.intellijPlatform/` (~2MB・ivy descriptor と sandbox のみ) とは別物。**初回のみ DL**。
- 所要時間の目安 (最小スキャフォールド、macOS 実測):

  | 実行 | 時間 |
  |---|---|
  | 初回 `./gradlew buildPlugin` (SDK DL 込み) | ~4〜5 分 |
  | 2 回目以降 `./gradlew buildPlugin test` | ~35 秒 |
  | 2 回目以降 `./gradlew updatePreview` | ~13 秒 |

- Gradle 9.6 で `val previewImplementation: Configuration by configurations.getting` は非推奨警告になる。
  `configurations.getByName("previewImplementation")` を使う (example は対応済み)。
