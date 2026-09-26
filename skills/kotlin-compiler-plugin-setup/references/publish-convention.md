# Publish / Release Convention

Maven Central 公開と CI / release の設計解説。**実ファイルは example が SSoT** (scaffold.sh がコピー・置換する):

| ファイル | 役割 |
|---|---|
| `example/gradle.properties` | `GROUP` / `VERSION_NAME` / `COMPILER_PLUGIN_ID` / `SUPPORTED_KOTLIN_*` / `POM_*` の SSoT + ビルド高速化設定 |
| `example/build.gradle.kts` | `allprojects { group; version }` を gradle.properties から設定 |
| `example/buildSrc/src/main/kotlin/publish.gradle.kts` | vanniktech convention (`id("buildsrc.convention.publish")`) |
| `example/.github/workflows/ci.yml` | PR / main の CI |
| `example/.github/workflows/release.yml` | tag push → Maven Central → GitHub Release |

## 座標とバージョンの SSoT

- 全モジュールの座標は `GROUP:<project.name>:VERSION_NAME`。**各モジュールは `coordinates(...)` を呼ばず** `pom { name; description }` だけ書く (呼ぶと SSoT が崩れる)
- Gradle plugin が消費者に注入する `compiler-plugin` / `runtime` のバージョンも gradle.properties から生成した `BuildConfig` を使う (`gradle-plugin-impl.md`)。ハードコードすると公開後に存在しないバージョンを解決しに行って壊れる
- バージョンを上げる時に触るのは `VERSION_NAME` の 1 行だけにする。README 等にバージョンが散らばる場合は bump 用の project-local skill を用意する (`kotlin-compiler-plugin-dev` の `references/release-operations.md`)
- 共通 POM (url / license / scm / developer) は `POM_*` gradle property で与える (`com.vanniktech.maven.publish` が自動で読む)。Maven Central は url / scm / developer が空だと reject する

## 署名

`signingInMemoryKey` (CI: `ORG_GRADLE_PROJECT_signingInMemoryKey`) か `signing.keyId` がある時だけ `signAllPublications()` する。鍵の無いローカルでも `publishToMavenLocal` が通る。

## Gradle plugin marker

`java-gradle-plugin` + vanniktech の組み合わせで、plugin 本体 (`GROUP:gradle-plugin`) と plugin marker (`<plugin-id>:<plugin-id>.gradle.plugin`) の両方が Maven Central に出る。利用者は `pluginManagement { repositories { mavenCentral() } }` だけで `plugins { id("<plugin-id>") version "x.y.z" }` を使える (Gradle Plugin Portal への publish は不要)。

## ビルド高速化設定 (gradle.properties)

| 設定 | 理由 |
|---|---|
| `org.gradle.caching` / `configuration-cache` / `parallel` | 標準の高速化 |
| `org.gradle.jvmargs=-Xmx4G` / `kotlin.daemon.jvmargs=-Xmx4G` | Kotlin compiler + KMP (native) でメモリ不足になりやすい |
| `kotlin.incremental.js=false` / `kotlin.incremental.js.klib=false` | [KT-82395](https://youtrack.jetbrains.com/issue/KT-82395) 回避 |
| `kotlin.native.binary.gc=cms` | Kotlin/Native の並行 GC |

daemon の JDK を固定したい場合は `./gradlew updateDaemonJvm --jvm-version=21` で `gradle/gradle-daemon-jvm.properties` を生成してコミットする (OS 別の toolchain URL が書かれるので手書きしない)。Android target を足す時は settings の `google()` に `content { includeGroupByRegex("com\\.android.*") ... }` を付け、Google 以外の依存で google() を引かない。

## CI (`ci.yml`)

- `concurrency.cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}` — PR の古い run だけ cancel し、main は cancel しない
- `paths-ignore: ['**/*.md', '.local/**']` — ドキュメントだけの変更で CI を回さない
- gradle タスクは `run: >-` の 1 行 1 タスク (scaffold.sh が skip したモジュールの行を落とすため)
- 失敗時だけ `**/build/reports/**` を upload
- `final` job (`if: always()` + `needs.*.result` 集約) を branch protection の required check にする。job を増減しても protection 設定を変えずに済む
- `publishToMavenLocal` job で POM / publication の配線ミスを release 前に検知する
- 複数 Kotlin バージョン matrix は `kotlin-compiler-plugin-dev` の `assets/workflows/compiler-plugin-test.yml` を追加する

## Release (`release.yml`)

- trigger: `v*.*.*` tag push。`workflow_dispatch` は `dryRun` (既定 true) で publish を skip した予行演習
- tag と `VERSION_NAME` の一致をチェックしてから publish (tag の打ち間違いで別バージョンが出るのを防ぐ)
- `concurrency: { group: release, cancel-in-progress: false }` — Maven Central への publish を並走・中断させない
- `publishAndReleaseToMavenCentral --no-configuration-cache` — vanniktech の signing タスクは configuration cache 非対応
- `macos-latest` で実行し、KMP runtime の Apple klib も 1 回で公開する
- `softprops/action-gh-release` の `generate_release_notes: true` + `prerelease: ${{ contains(github.ref_name, '-') }}` (`v1.0.0-rc1` は pre-release)
- Secrets は `ORG_GRADLE_PROJECT_*` という名前で登録し、そのまま `env:` に流す (Gradle が project property として読むので対応表が要らない)。GPG 鍵生成・Secrets 登録の手順は `kotlin-maven-central-publish` skill を参照
- 別案: GitHub Release の published を trigger にし、`workflow_dispatch` に `stage_only` (Central Portal に置くだけで手動 Publish) を用意する構成もある (debuggable-compiler-plugin)
