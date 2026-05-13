# Troubleshooting: 失敗パターン別の原因と対処

multi-Kotlin support の構築・運用で頻繁に踏む地雷を網羅した check list。 症状でリードを引いて該当節を読む。

---

## 1. `NoSuchMethodError: org.jetbrains.kotlin.<X>` (runtime)

**症状**: 特定 Kotlin 版で plugin を load した瞬間 or 最初の IR transform で発生。

**原因**: main 実装 (baseline Kotlin で compile) が直接呼んでる Kotlin compiler API の signature が、 runtime の Kotlin 版で変わっている。

**対処の優先度**:

1. **`CompatContext` に shim メソッドを追加** — `IrBuilderWithScope` の receiver widening 等、 signature が違うだけの場合
   ```kotlin
   public interface CompatContext {
       public fun irCall(builder: IrBuilderWithScope, callee: IrSimpleFunctionSymbol): IrCall
   }
   ```
   各 compat module は自分の baseline で `irCall(builder, callee)` を呼ぶだけ → 生成 bytecode は対応 JVM signature を参照。

2. **reflection shim で吸収** — 影響範囲が 1 シンボル / 1 enum entry / 1 method なら新 compat module を作らず `compat/IrXxxCompat.kt` で吸収。 詳細は `reflection-shim.md`。

3. **新 compat module を作る** — 上記で吸収できないほど影響範囲が広いとき。 `compat-module-setup.md` の delegation pattern を使う。

---

## 2. `NoClassDefFoundError: org.jetbrains.kotlin.fir.declarations.FirNamedFunction`

**症状**: plugin の startup 時 (FIR extension の class load) に発生。 plugin 全体が動かなくなる。

**原因**: 新 Kotlin 版で導入された class を、 plugin の field type / method signature が直接参照しているので、 古い Kotlin で classloader が **必要なくても** 該当 class を resolve しようとして fail する。 try-catch では絶対に救えない (JVM verifier の段階で死ぬ)。

**対処**: **capability flag で register をスキップ** する。
```kotlin
if (compat.supportsFirCheckers()) {
    +::PreviewLabFirCheckersExtension
}
```
詳細と KDoc の書き方は `version-gating.md`。

---

## 3. `IncompatibleClassChangeError`

**症状**: method 呼び出し時 / field access 時に発生。

**原因**: 同じシンボル名だが return type / field type が widening / narrowing された (例: Kotlin 2.4 で `IrUtilsKt.getAnnotation` の return が `IrConstructorCall?` → `IrAnnotation?` に widening)。

**対処**: reflection shim、 もしくはエクステンション関数を **自前で再実装** (= 該当処理を直接 `annotations.firstOrNull { ... }` で書き下す)。 `IrAnnotationCompat` 参照。

---

## 4. `Cannot parse 'X' as Java version` (kctfork test 起動時)

**症状**: kctfork (`dev.zacsweers.kctfork:core`) を使うテストが、 `KotlinCompilation` を `compile()` した瞬間に scary stack trace で死ぬ。 stack trace に Java 26 とかが見える。

**原因**: kctfork が bundle する `kotlin-compiler-embeddable` の中の Java version parser が、 JDK 26+ の新 version string format をパースできない。 Gradle daemon が JDK 26 で走ってると test JVM もそれを継承して死ぬ。

**対処**: test task の `javaLauncher` を Java 21 toolchain に明示的に pin:

```kotlin
tasks.withType<Test> {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
```

注意: `gradle.properties` の `org.gradle.java.home` で global pin すると Gradle 全体が 21 になるので、 task 単位の `javaLauncher` 推奨。

---

## 5. `metadata version is X.Y.0, expected version is A.B.0`

**症状**: `compileTestKotlin` task で出る。 `-Ptest.kotlin=X.Y.Z` で baseline と異なる Kotlin の test classpath を構成したとき発生。

**原因**: test source は baseline KGP で compile されるが、 test classpath には `-Ptest.kotlin` で指定した別 Kotlin の `kotlin-compiler-embeddable` が乗っている。 metadata version が一致しないと KGP が拒否する。

**対処**: `testKotlinVersion != baseline` の時だけ skip flag を追加:

```kotlin
if (testKotlinVersion != compilerPluginBaselineKotlin) {
    tasks.named<KotlinCompile>("compileTestKotlin") {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xskip-prerelease-check",
                "-Xskip-metadata-version-check",
            )
        }
    }
}
```

**やってはいけない**: 常時 (= 同一バージョン時にも) skip flag を付ける → main 実装が baseline と乖離してても警告が消える / 安全網が無効化。

---

## 6. `Could not initialize class org.jetbrains.kotlin.buildtools.internal.<X>`

**症状**: Gradle build がそもそも始まらない。 `:compiler-plugin:test` を実行しようとした時点で KGP の初期化で死ぬ。

**原因**: `configurations.all { resolutionStrategy.force(...) }` または `subprojects { ... }` レベルで `kotlin-compiler-embeddable` の version を強制している → KGP が自分の作業に使う dependency も置き換わってしまう。

**対処**: `force` resolution は **test classpath 限定**:

```kotlin
listOf("testCompileClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name).configure {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-compiler-embeddable:$testKotlinVersion")
        }
    }
}
```

---

## 7. test が `UP-TO-DATE` で skip される (CI matrix の 2 つ目以降)

**症状**: matrix の 1 つ目は test が走るが、 2 つ目以降 `Task :compiler-plugin:test UP-TO-DATE` で skip される。 ローカルでも `./scripts/compiler-plugin-test.sh 2.3.21` の後に `2.4.0-Beta2` を実行すると再現。

**原因**: Gradle build cache が `-Ptest.kotlin` の値を input として認識しない。 task 入力が同じだから「実行済み」 と判定される。

**対処**: per-version test script で **`--rerun-tasks`** を必ず付ける。

```bash
./gradlew :compiler-plugin:test --rerun-tasks -Ptest.kotlin="$VERSION" --continue
```

CI ジョブ毎に runner が変わる場合は build cache が空なので発生しないが、 self-hosted runner / 個別 cache key の場合は要注意。

---

## 8. ShadowJar に compat module が含まれない

**症状**: `gradle publishToMavenLocal` した plugin を別プロジェクトで使うと `error("No CompatContext.Factory implementations found")` で死ぬ。

**原因 a**: `embedded` configuration に新 compat module を追加し忘れた。
```kotlin
dependencies {
    add(embedded.name, projects.compilerPlugin.compatK2320)   // ← これ漏れ
}
```

**原因 b**: `mergeServiceFiles()` を `shadowJar { ... }` に書き忘れた → 1 compat module 分の `META-INF/services/...$Factory` しか jar に入らない。

**確認方法**:

```bash
./gradlew :compiler-plugin:shadowJar
unzip -p compiler-plugin/build/libs/*-all.jar \
  META-INF/services/me.tbsten.compose.preview.lab.compiler.compat.CompatContext\$Factory
```

各 compat module の Factory FQN が **改行区切りで全部** 出るのが正解。

---

## 9. `settings.gradle.kts` の typesafe accessor が見つからない (`projects.compilerPlugin.compatK240Beta2`)

**症状**: build script で `projects.compilerPlugin.compatK240Beta2` が unresolved になる。

**原因**: settings.gradle.kts の `include(":compiler-plugin:compat-k240_beta2")` が漏れている、 または `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` が無い。

**対処**:
```kotlin
// settings.gradle.kts
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":compiler-plugin:compat-k240_beta2")
```

camel 変換ルール: kebab → camel, underscore も camel boundary 扱い。 `compat-k240_beta2` → `compatK240Beta2`。

---

## 10. Beta / RC で plugin が動かないが、 stable では動く

**症状**: `2.4.0-Beta2` だけ test 失敗。 `2.3.21` (stable) では問題なし。

**原因の切り分け**:

1. `KotlinToolingVersion` の Maturity comparison が壊れて Factory 選択がずれている → Factory 列挙して `compat.javaClass.simpleName` を log
2. Beta で API breakage が入った → 該当 API 周りの compat shim を更新 (新 compat module or 既存の override)
3. kctfork が Beta に未対応 → `when { startsWith("2.4") -> ... }` で kctfork 0.13.0-alpha01 等に切り替え

---

## 11. JS / Wasm の test だけ `referenceFunctions` で missing になる

**症状**: JVM では `collectAllModulePreviews()` が動くが、 JS/Wasm の test では空配列が返る。

**原因**: KT-82395 (KLIB incremental compile bug)。 2.3.20 まで KLIB の top-level declaration が stale cache に残って `referenceFunctions` が間違ったエントリを返す。

**対処**: `supportsKlibCrossModuleHint()` capability flag で gating。 2.3.21+ または 2.4.0-Beta2+ で true を返す compat module を使う。 詳細は `version-gating.md`。

---

## 12. Compose Multiplatform 版が合わない

**症状**: nightly で Kotlin 2.4.0-Beta2 × Compose `1.11.0-alpha04` の組合せがビルド失敗。

**対処**: JetBrains/compose-multiplatform Release Notes で "Supports Kotlin X.Y" アナウンスを確認。 新 Kotlin 版が出てから対応 Compose alpha が出るまでに lag があるので、 Compose 側の release が来るまで Kotlin だけ Beta、 Compose は stable のまま、 等の split 構成を取れるかも検討。

---

## 13. ServiceLoader が `compat-kXX` の Factory を見つけない

**症状**: ローカルでは動くが、 IDE から JetBrains plugin として使うと NCDFE / Service load 失敗。

**原因**: IDE が plugin の ShadowJar ではなく、 個別の `compat-kXX-X.X.X-dev.jar` を classpath に並べていて、 `META-INF/services` の merge が効いていない。

**対処**: gradle-plugin 側で plugin artifact として **常に shadow jar を参照** する設定にする (`SubpluginArtifact("me.tbsten.compose-preview-lab", "compiler-plugin", version)` で main artifact のみ参照、 各 compat module は別 publish しない)。
