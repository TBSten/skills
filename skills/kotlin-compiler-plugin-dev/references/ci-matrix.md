# CI Matrix と kctfork バージョンマップの設定詳細

## 設計方針: SSOT 駆動 + dynamic matrix

CI matrix を **行ベースのテキストファイル** (= SSOT) から `fromJSON` で動的に展開する。 これにより:

- 新バージョン追加は SSOT に 1 行追加するだけで CI 全 job に反映
- `compiler-plugin-test.sh` が local 実行でも同じ SSOT を読むので、 「CI が落ちるけど local で再現できない」 を回避
- README / docs に書くサポート行表が SSOT と sync する設計を取れる

実例: [TBSten/compose-preview-lab/.github/workflows/pull-request.yml](https://github.com/TBSten/compose-preview-lab/blob/main/.github/workflows/pull-request.yml) の `resolve-supported-kotlin-versions` + `compiler-plugin-test` ペア。

---

## SSOT (`scripts/supported-kotlin-versions.txt`)

**実ファイル (テンプレート): [`../assets/scripts/supported-kotlin-versions.txt`](../assets/scripts/supported-kotlin-versions.txt)**

対象プロジェクトに無ければこのファイルを `<project-root>/scripts/supported-kotlin-versions.txt` にコピーし、実際のサポートバージョン・サポート範囲コメントに合わせて編集する。

書式ルール: 1 行 1 バージョン。空行と `#` 始まりはコメント扱い。 CI 側で `grep -vE '^[[:space:]]*(#|$)'` で除外する。ファイル冒頭コメントにはサポート範囲の根拠 (どの API に依存して最小バージョンが決まったか) とバージョン追加手順を書いておく。

---

## GitHub Actions Workflow

**実ファイル (汎用部分のテンプレート): [`../assets/workflows/compiler-plugin-test.yml`](../assets/workflows/compiler-plugin-test.yml)**

対象プロジェクトに compiler-plugin テストの workflow が無ければ、このファイルを `<project-root>/.github/workflows/compiler-plugin-test.yml` にコピーする (既存の `pull-request.yml` がある場合は 2 つの job をそこにマージしてもよい)。中身は:

1. **`resolve-supported-kotlin-versions`** — SSOT を `grep -vE '^[[:space:]]*(#|$)' | jq -R . | jq -sc .` で JSON 配列化して outputs に載せる
2. **`compiler-plugin-test`** — `fromJSON` で dynamic matrix 展開し、各バージョンで `./scripts/compiler-plugin-test.sh "${{ matrix.kotlin }}"` を実行。`fail-fast: false` 必須 (RC/Beta が落ちても stable の結果を視認できる)

lint / binary compatibility (`apiCheck`) / baseline Kotlin での通常 unit test (JVM / JS / Wasm / Android / iOS のターゲット matrix) はプロジェクト固有のため、このテンプレートには含めていない。実例は [TBSten/compose-preview-lab/.github/workflows/pull-request.yml](https://github.com/TBSten/compose-preview-lab/blob/main/.github/workflows/pull-request.yml) を参照。

---

## Per-Version Test Script (`scripts/compiler-plugin-test.sh`)

**実ファイル: [`../assets/scripts/compiler-plugin-test.sh`](../assets/scripts/compiler-plugin-test.sh)**

対象プロジェクトに無ければこのファイルを `<project-root>/scripts/compiler-plugin-test.sh` にコピーして `chmod +x` する。

- `./scripts/compiler-plugin-test.sh <kotlin-version>` — 単一バージョンのテスト。`-Ptest.kotlin` で kctfork が使う kotlin-compiler-embeddable を差し替える (`compiler-plugin/build.gradle.kts` が `resolutionStrategy.force` に反映する — 後述)
- `./scripts/compiler-plugin-test.sh --all` — SSOT の全バージョンを順に実行し、失敗バージョン一覧を末尾に出力 (local で CI 相当の確認をする時に使う)

`--rerun-tasks` 必須 (script 内で常に付与)。 Gradle build cache が `-Ptest.kotlin` を input として認識しないため、 同じテストタスクを別 Kotlin で走らせる時 `UP-TO-DATE` で skip される事故を防ぐ。

---

## Gradle 側 (`compiler-plugin/build.gradle.kts`)

### `-Ptest.kotlin` を読んで test classpath だけ override する

main 実装は baseline Kotlin で compile されたままにする (= main classpath の Kotlin を動かさない)。 test classpath だけ `resolutionStrategy.force` で override する。

```kotlin
val compilerPluginBaselineKotlin: String = libs.versions.kotlin.get()
val testKotlinVersion: String = (findProperty("test.kotlin") as String?) ?: compilerPluginBaselineKotlin

// kctfork version selection — 大きな表ではなく境界判定で十分
val kctforkVersion: String = when {
    testKotlinVersion.startsWith("2.1") -> "0.10.0"           // K2JVMCompilerArguments.jvmDefaultStable absent before 2.2.0
    testKotlinVersion.startsWith("2.4") -> "0.13.0-alpha01"   // 2.4 系の API 変更追従
    else -> libs.versions.kctfork.get()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$compilerPluginBaselineKotlin")

    testImplementation("dev.zacsweers.kctfork:core:$kctforkVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$testKotlinVersion")
}

// ★ baseline と異なる Kotlin の test classpath を許容するためのオプション
if (testKotlinVersion != compilerPluginBaselineKotlin) {
    tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xskip-prerelease-check",
                "-Xskip-metadata-version-check",
            )
        }
    }
}

// ★ force resolution は test classpath 限定 (main classpath に波及させると KGP の初期化が壊れる)
listOf("testCompileClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name).configure {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-compiler-embeddable:$testKotlinVersion")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("test.kotlin.version", testKotlinVersion)

    // ★ kctfork が bundle する kotlin-compiler-embeddable は Java 26 の version string を
    //   パースできない。 必ず Java 21 toolchain に pin。
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
```

### よくあるミス

| やったこと                                                            | 起きること                                                                  |
| ---                                                                   | ---                                                                         |
| `configurations.all { resolutionStrategy.force(...) }`                | KGP 自体の依存も差し替わる → `Could not initialize class ...buildtools...` |
| `compileTestKotlin` に skip flag を **常に** 付ける                   | main vs test の同期不整合に気付けない / 安全網が無効化                      |
| `--rerun-tasks` を付け忘れる                                          | 2 バージョン目以降が UP-TO-DATE で skip → 実は test 走ってない              |
| `setup-java` の `java-version: 21` だけ指定                           | Gradle daemon は 21 で動くが test task は別。 `javaLauncher` での pin が必要 |

---

## JS KLIB cross-module test の落とし穴

JS / Wasm の KLIB 依存解決は KMP variant resolution に従う。 JVM project から JS KLIB を pull する場合、 通常 `org.jetbrains.kotlin:kotlin-stdlib-js` を引くと変な variant に解決されるので、 `@klib` qualifier 付きの configuration を別途作る:

```kotlin
val kotlinStdlibJsKlib by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies {
    add(kotlinStdlibJsKlib.name, "org.jetbrains.kotlin:kotlin-stdlib-js:$testKotlinVersion@klib")
}

// configuration-cache safe な ArgumentProvider 経由でテストに渡す
abstract class StdlibJsKlibArgumentProvider : CommandLineArgumentProvider {
    @get:org.gradle.api.tasks.Classpath
    abstract val klibFiles: ConfigurableFileCollection

    override fun asArguments(): List<String> =
        listOf("-Dtest.kotlin.stdlib.js.klib=${klibFiles.singleFile.absolutePath}")
}

tasks.withType<Test> {
    jvmArgumentProviders.add(
        objects.newInstance(StdlibJsKlibArgumentProvider::class).apply {
            klibFiles.from(kotlinStdlibJsKlib)
        },
    )
}
```

テスト側 (kctfork の JS compilation を構築する base class) で `System.getProperty("test.kotlin.stdlib.js.klib")` を `args.libraries` に追加する。

---

## kctfork バージョン選択の詳細

unit test で kctfork (ZacSweers/kotlin-compile-testing) を使う場合の対応表。 表が大きくならないのは、 kctfork が概ね Kotlin minor に追従して 1 release 出すため。

| Kotlin              | kctfork          | 備考                                                                          |
| ---                 | ---              | ---                                                                           |
| 2.1.x               | 0.10.0           | `K2JVMCompilerArguments.jvmDefaultStable` が 2.2.0 で追加。 2.1 では 0.10.0 系 |
| 2.2.x               | libs.versions.kctfork (現行 stable)   | 通常はバージョン catalog の現行値で十分                                       |
| 2.3.x               | libs.versions.kctfork (現行 stable)   | 同上                                                                          |
| 2.4.x (Beta)        | 0.13.0-alpha01   | 2.4 系の compiler API 変更追従                                                |

新 minor 追加時は ZacSweers/kotlin-compile-testing の Releases ページで対応バージョンを確認:
https://github.com/ZacSweers/kotlin-compile-testing/releases

RC / Beta は通常直近 stable の kctfork でいけるが、 起動失敗時は 1 つ前 / 次 の kctfork を試す。

---

## Nightly Checking (Beta/RC を予告的に検証する layer)

PR 単位の `pull-request.yml` とは別に、 nightly で:

- Beta/RC × 最新 alpha Compose × 新 Gradle の組み合わせを Property-Based Test (PBT) で揺さぶる
- Claude Code を 60 分 budget で走らせて exploratory verification を実施

詳細実装は実プロジェクトの `.github/workflows/nightly-checking.yml` を参照。 「Beta/RC が CI matrix に乗る前」 の検証 layer として、 新 Kotlin リリース直後に追加すべき。

```yaml
name: Nightly Checking
on:
  schedule:
    - cron: '0 20 * * *'   # 05:00 JST
  workflow_dispatch:

jobs:
  pbt:
    strategy:
      fail-fast: false
      matrix:
        include:
          - kotlin_version: ''
            compose_version: ''
            name: 'default'
          - kotlin_version: '2.3.21'
            compose_version: '1.11.0-alpha04'
            name: 'Kotlin 2.3.21'
          - kotlin_version: '2.4.0-Beta2'
            compose_version: '1.11.0-alpha04'
            gradle_version: '9.4.1'
            name: 'Kotlin 2.4.0-Beta2'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v5
      - name: Switch Kotlin / Compose version
        if: matrix.kotlin_version
        run: |
          sed -i "s/^kotlin = .*/kotlin = \"${{ matrix.kotlin_version }}\"/" gradle/libs.versions.toml
          sed -i "s/^compose = .*/compose = \"${{ matrix.compose_version }}\"/" gradle/libs.versions.toml
      - name: Upgrade Gradle wrapper
        if: matrix.gradle_version
        run: |
          sed -i "s|gradle-.*-bin.zip|gradle-${{ matrix.gradle_version }}-bin.zip|" gradle/wrapper/gradle-wrapper.properties
      - name: Run PBT
        run: ./gradlew jvmTest -Dkotest.tags="PBT" --warning-mode all
```

`sed -i` で `gradle/libs.versions.toml` の Kotlin / Compose 行を書き換えるだけで version 切り替えが効く設計にしておく。
