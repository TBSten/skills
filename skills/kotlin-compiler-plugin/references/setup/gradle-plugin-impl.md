# Gradle Plugin Implementation

`KotlinCompilerPluginSupportPlugin` を使って compiler plugin を Gradle plugin としてラップするパターンの設計解説。
**完全なコードは example の実ファイルが SSoT** (scaffold.sh がコピー・置換する):

| ファイル | 役割 |
|---|---|
| `example/gradle-plugin/src/main/kotlin/.../gradle/ExampleGradlePlugin.kt` | plugin 本体 |
| `example/gradle-plugin/src/main/kotlin/.../gradle/ExampleExtension.kt` | DSL (`examplePlugin { }`) |
| `example/gradle-plugin/src/main/kotlin/.../gradle/ExampleKotlinVersionGuard.kt` | 利用側 Kotlin 版のガード + 版比較 |
| `example/gradle-plugin/build.gradle.kts` | `BuildConfig` 生成・metadata 床・plugin 登録 |
| `example/gradle-plugin/src/test/.../ExampleGradlePluginTest.kt` | ProjectBuilder sanity テスト |

## 実装の構成要素

| override | 役割 |
|---|---|
| `apply(target)` | DSL 登録 (`convention()` で既定値) → afterEvaluate で Kotlin plugin 有無チェック → Kotlin 版ガード → runtime 依存の自動追加 |
| `isApplicable(compilation)` | plugin を適用する compilation の選別 (通常 `true`) |
| `getCompilerPluginId()` | `CommandLineProcessor.pluginId` と一致させる (`BuildConfig.COMPILER_PLUGIN_ID`) |
| `getPluginArtifact()` | compiler plugin の Maven 座標 (`BuildConfig.GROUP_ID` / `VERSION`) |
| `applyToCompilation(compilation)` | DSL の値を `SubpluginOption` に変換 (`extension.enabled.map { ... }`) |

## ポイント

### 座標・ID・版は BuildConfig で埋め込む

`gradle-plugin/build.gradle.kts` の `generateBuildConfig` タスクが gradle.properties (`GROUP` / `VERSION_NAME` / `COMPILER_PLUGIN_ID` / `SUPPORTED_KOTLIN_*`) から `BuildConfig.kt` を生成する。`kotlin.srcDir(taskProvider)` で登録すると compileKotlin / sourcesJar 等の依存が自動で張られる (タスク名を列挙して `dependsOn` しない)。外部 plugin を使うなら `com.github.gmazzo.buildconfig` でもよい。

### DSL は `Property` + `convention()`

`abstract val enabled: Property<Boolean>` を持つ abstract class を `extensions.create` し、既定値は `convention(true)`。利用者は変えたい値だけ書けばよく、`applyToCompilation` では `Provider.map` で遅延評価のまま `SubpluginOption` に変換する。**渡すキーは全て compiler plugin 側の `CliOption` に宣言する** (`plugin-registration.md`)。

### fail-fast ガード

- Kotlin plugin (jvm / multiplatform / android) が 1 つも無いまま評価が終わったら warn する。`applyToCompilation` が呼ばれず黙って no-op になるのを防ぐ (root project にだけ適用しても subproject には効かない、が典型)
- 追加先 configuration (`implementation` / `commonMainImplementation`) が無ければ手動追加の方法を warn する
- 別 plugin との適用順に制約がある場合は `pluginManager.hasPlugin(...)` で検知して `GradleException` にする (例: compose-preview-lab は Kotlin 2.3 未満で Compose compiler plugin より後に適用されたらエラー)

### 利用側 Kotlin 版ガード

`getKotlinPluginVersion()` を `SUPPORTED_KOTLIN_MIN` / `SUPPORTED_KOTLIN_MAX_TESTED_EXCLUSIVE` と比較する:

| 利用側 Kotlin | 挙動 | 理由 |
|---|---|---|
| `< MIN` | `GradleException` | compiler API が違いすぎ、compiler 内部の `NoSuchMethodError` より分かりやすい |
| `>= MAX_TESTED_EXCLUSIVE` | warn のみ | 未検証だが動く可能性があり、利用者の Kotlin 更新を止めない |
| 解析不能 | warn して skip | ガード自体でビルドを壊さない |

pre-release は同じ base の stable より小さい (`2.4.0-RC < 2.4.0`) ので、MAX を `2.4.0` にすると `2.4.0-RC` は検証済み扱いになる。scaffold.sh は `--kotlin-version` から MIN=`<major>.<minor>.0` / MAX=次の minor を設定する。複数 Kotlin 版対応 (compat module) を入れたら MIN を最古の対応版に下げ、新版を CI に足すたびに MAX を上げる。

### runtime 依存の自動追加

ユーザーが `plugins { id("<plugin-id>") }` だけで使えるように、afterEvaluate で `GROUP:runtime:VERSION` を追加する。KMP なら `commonMainImplementation`、それ以外は `implementation`。DSL の `addRuntimeDependency = false` で無効化できる。

### metadata の床と bytecode

- Gradle plugin は利用者の Gradle (埋め込み Kotlin が古い) 上で読み込まれるため、`apiVersion` / `languageVersion` を `KOTLIN_2_1` に固定する (Kotlin は 1 つ先の metadata まで読める)。2.0 は Kotlin 2.3 で deprecated warning になる
- compiler plugin / Gradle plugin は利用者の JDK 17 daemon でも動くよう、`buildsrc.convention.kotlin-jvm` で JDK 21 toolchain + `jvmTarget = 17` にしている

### build.gradle.kts での登録

`gradlePlugin { plugins { create(...) { id = providers.gradleProperty("COMPILER_PLUGIN_ID").get() } } }`。`java-gradle-plugin` が `META-INF/gradle-plugins/<plugin-id>.properties` を生成し、vanniktech と組み合わせると plugin marker も公開される (`publish-convention.md`)。

gradle-plugin は `compiler-plugin` / `runtime` に依存しない (`SubpluginArtifact` と runtime 自動追加で利用側が解決する)。依存すると利用者の buildscript classpath に不要な jar が載る。
