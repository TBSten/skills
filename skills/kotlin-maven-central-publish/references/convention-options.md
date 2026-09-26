# convention plugin の設計オプション

`example/publish-convention.gradle.kts` (setup-publish.sh が生成) の各設定の理由と、プロジェクト都合で変えたい時の選択肢。
いずれも TBSten の Kotlin ライブラリ (katachi / koma-strict / capture-compiler-plugin / debuggable-compiler-plugin / compose-preview-lab / cream) の実設定で裏取り済み。

## 1. convention を置く場所: buildSrc か build-logic (included build) か

`setup-publish.sh --convention-dir <dir>` で切り替える (デフォルト `buildSrc`)。

| | buildSrc | build-logic (included build) |
|---|---|---|
| ルート settings | 不要 (自動で読まれる) | `pluginManagement { includeBuild("build-logic") }` が必要 (script が未設定を ACTION_REQUIRED で報告) |
| convention ビルドに載せたプラグイン (KGP 等) のモジュールからの要求 | **バージョン無し** で要求する: `id(libs.plugins.kotlin.jvm.get().pluginId)`。`alias(...)` だと `already on the classpath with an unknown version` で失敗 | **`alias(...)` (バージョン付き) のまま** で良い。逆に id のみだと `Plugin ... was not found` で失敗 |
| 向いている構成 | 単発のライブラリ、既存 buildSrc がある | ksp-plugin-project-setup / kotlin-library-bootup のように複数の convention plugin を持つ構成 |

どちらでも **Kotlin Gradle plugin (と Android を使うなら AGP) を vanniktech と同じ convention ビルドの classpath に載せる** 必要がある。
vanniktech 0.36+ は KGP のクラスに触るため、別 classloader にあると
`Make sure the Kotlin version 2.2.0 or newer is applied. ... was not able to access Kotlin plugin classes` で失敗する。
script は catalog の `org.jetbrains.kotlin.*` / `com.android.*` エイリアスを検出して `implementation(plugin(libs.plugins.<alias>))` を生成する。

### class plugin (`Plugin<Project>`) で書く場合

build-logic で class ベースの convention plugin (compose-preview-lab の `PublishConventionPlugin`) にする場合も中身は同じ。
`pluginManager.apply("com.vanniktech.maven.publish")` → `extensions.configure<MavenPublishBaseExtension> { ... }` と書き、
`gradlePlugin { plugins { create("publish") { id = "publish-convention"; implementationClass = "..." } } }` で登録する。
`afterEvaluate` で包む必要は無い (vanniktech の値は Provider で遅延評価される)。

## 2. 署名の条件付き有効化

`signAllPublications()` は非 SNAPSHOT 版で署名を **必須** にする (`gradleSigning.setRequired(!SNAPSHOT)`)。
鍵の無いローカルで `publishToMavenLocal` を通すには条件付きにする必要がある。方式は 2 つ。

| 方式 | 書き方 | 長所 | 短所 |
|---|---|---|---|
| **A. ローカル publish タスクの時だけスキップ (既定)** | `taskNames.any { it.substringAfterLast(':').endsWith("ToMavenLocal") }` なら呼ばない | CI で Secrets が欠けていると署名段階で即失敗 (fail fast) | タスク名判定なので、IDE から別名タスク経由で呼ぶと効かないことがある |
| B. 鍵がある時だけ有効 | `providers.gradleProperty("signingInMemoryKey").isPresent \|\| providers.gradleProperty("signing.keyId").isPresent` | タスク名に依存しない | CI で Secrets が欠けても未署名のまま upload され、Central の検証で後から失敗する |

- 判定は **末尾セグメント** で行う。完全一致だと `:lib:publishToMavenLocal` のような path 付き指定を取りこぼし、`no configured signatory` で落ちる
- B の鍵判定は `System.getenv(...)` ではなく `providers.gradleProperty(...)` を使う。`ORG_GRADLE_PROJECT_*` 環境変数と `~/.gradle/gradle.properties` の両方を拾える
- 独自プロパティで明示的に切る方式 (`-Pxxx.skipSigning`) もあるが、付け忘れで失敗するので A を推奨

## 3. artifactId と coordinates

- vanniktech の既定 artifactId は `project.name`。`:lib:core` と `:other:core` のようにネストすると衝突するため、convention で `project.path` から導出する (`:ksp:processor` → `ksp-processor`)。トップレベルモジュールでは `project.name` と同じ
- KMP の場合もプラットフォーム接尾辞 (`-jvm` 等) は vanniktech が維持する
- **モジュール側では `coordinates(...)` を呼ばない**。モジュール固有にしたいのは pom の name / description だけで、次のように上書きする:

```kotlin
mavenPublishing {
    pom {
        description.set("KSP processor for mylib")
    }
}
```

## 4. group / version の SSoT

| 置き場所 | 書き方 | 備考 |
|---|---|---|
| version catalog `[versions]` (例: `mylib = "0.1.0"`) | 各モジュール (またはルート `allprojects`) で `version = libs.versions.mylib.get()` | katachi / koma-strict。Renovate 等が触らないキー名にする。`setup-publish.sh --version-ref mylib --version 0.1.0` |
| `gradle.properties` の `VERSION_NAME` / `GROUP` | 何も書かなくて良い (vanniktech が `VERSION_NAME` / `GROUP` を自動で読む) | capture。`--version-ref` を省略すると script はこちらを使う |

publish.yml の「Tag matches the version」ステップはこの SSoT を読むので、どちらかに統一する。

### SNAPSHOT を使わない運用 (任意)

main のバージョンを常に「次にリリースする予定の版」(`0.2.0` 等) にしておき、`-SNAPSHOT` を付けない運用もある (capture/docs/versioning.md)。
`publishToMavenLocal` の成果物が常に意味のある版になる一方、手元で `publishAndReleaseToMavenCentral` を叩くと即リリースになるので、
Central の credentials はローカルに置かず CI (publish.yml) だけで publish する前提にする。publish.yml は SNAPSHOT 版の publish を拒否する。

## 5. javadoc jar と Dokka

Central は javadoc jar を必須にしているが、中身は空でも受理される。vanniktech 0.36+ の既定:

| プロジェクト | javadoc jar |
|---|---|
| `org.jetbrains.dokka-javadoc` を apply | Dokka の Javadoc 出力 (`dokkaGeneratePublicationJavadoc`) を自動で使う |
| `org.jetbrains.dokka` のみ | Dokka の HTML 出力 |
| KMP (Dokka なし) | 空 jar |
| JVM (Dokka なし) | 素の javadoc |

Dokka でちゃんとした javadoc を出すなら、公開モジュール (または convention) に `org.jetbrains.dokka-javadoc` を apply するだけでよい。
Dokka 2.x では HTML (`org.jetbrains.dokka`) と Javadoc (`org.jetbrains.dokka-javadoc`) は別プラグイン。

`configure(KotlinJvm(javadocJar = JavadocJar.Dokka(...)))` で明示する場合、タスク名は **`dokkaGeneratePublicationJavadoc`**。
`"dokkaGenerate"` は出力を持たない lifecycle タスクで、渡すと依存タスクは走るのに **jar の中身が空** になる (vanniktech `JavadocJar.dokkaJavadocJar` は `from(tasks.named(name))` するだけのため)。

ソースへのリンク:

```kotlin
dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            // localDirectory の既定はモジュールルート。相対パスと "#L<行>" は Dokka が付ける
            remoteUrl("https://github.com/<owner>/<repo>/blob/main/${project.name}")
        }
    }
}
```

## 6. 古い Kotlin の利用者が読めるようにする (languageVersion / apiVersion 床)

既定では成果物の Kotlin metadata はビルドに使った Kotlin の版になり、コンパイラは「自分 +1 マイナー」までしか読めない
(例: Kotlin 2.4 でビルドすると Kotlin 2.2 の利用者側で全シンボルが `Unresolved reference`)。
サポートする最低 Kotlin を決め、公開モジュールで床を下げる:

```kotlin
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
    // 推移的に入る kotlin-stdlib も下げないと、そちらの metadata で同じ問題が起きる
    coreLibrariesVersion = "2.2.20"
}
```

代償としてライブラリ自身がその版の言語機能しか使えなくなる (preview 扱いに戻った機能は `-X...` フラグが要る)。
下限を上げる時は README の対応表も一緒に更新する。
