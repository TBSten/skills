# Add/Remove Supported Kotlin Version — Workflow

Kotlin Compiler Plugin のサポート対象 Kotlin バージョンを追加・削除するための詳細ワークフロー。
本ワークフローはプロジェクトに既に複数バージョン対応基盤（compat module layer または source set separation）が存在することを前提とする。基盤の初期セットアップは `kotlin-compiler-plugin-setup` の Step 10 を参照。

`kotlin-compiler-plugin-dev` の Step 6 から本ファイルが参照される。

---

## Step 0: 要件確認とアーキテクチャ判定

ユーザーの指示から以下を把握する。明確なら聞き直さない。

1. **対象バージョン** — 追加 / 削除 / 置換したい Kotlin バージョン (stable / Beta / RC / dev のいずれか)
2. **operation** — 追加 / 削除 / 置換
3. **プロジェクトのアーキテクチャ** — 以下のどちらか:
   - **A: Compat Module Layer** — `compiler-plugin/compat-kXX/` 形式のモジュールがある。ServiceLoader で実行時に実装を選択する。 ShadowJar で 1 jar にバンドル
   - **B: Source Set Separation** — `src/v2_0_0/kotlin` / `src/pre_2_0_0/kotlin` 形式のディレクトリがある。Gradle がビルド時にソースセットを切り替える

アーキテクチャが不明な場合は `settings.gradle.kts` と `build.gradle.kts` を読んで判断する。

### A の構成 (このスキルが主に想定する形)

```
compiler-plugin/                  # baseline Kotlin で compile される main 実装 (ShadowJar 公開)
├── compat/                       # 共有 SPI: CompatContext, KotlinToolingVersion, ServiceLoader
├── compat-k210/                  # Kotlin 2.1.20+ 実装
├── compat-k222/                  # Kotlin 2.2.x 実装 (delegation: by k210)
├── compat-k2220/                 # Kotlin 2.2.20+ 実装 (delegation: by k222)
├── compat-k230/                  # Kotlin 2.3.0+ 実装 (delegation: by k2220)
├── compat-k2320/                 # Kotlin 2.3.20+ (capability flag on/off)
├── compat-k2321/                 # Kotlin 2.3.21+ (KLIB IC safety)
└── compat-k240_beta2/            # Kotlin 2.4.0-Beta2+ 実装
```

各 compat module は `compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:X.Y.Z")` を独自に pin する。

---

## Step 1A: Compat Module Layer の場合

### 1A-1: 既存 compat module で対応可能か判定

各 compat module の `Factory.minVersion` で `KotlinToolingVersion` 比較する。 `minVersion ≤ current` を満たす中で最大の minVersion を持つ Factory が選ばれる。 Beta / RC は STABLE より小さい (`2.4.0 > 2.4.0-Beta2 > 2.4.0-Beta1`) — 詳細は `./kotlin-tooling-version.md`。

**判定フロー**:

1. `scripts/supported-kotlin-versions.txt` (= SSOT) に対象バージョンを 1 行追加
2. `./scripts/compiler-plugin-test.sh X.Y.Z` でテスト
3. **OK → Step 2 へ進む**
4. **失敗パターン別の対処**:
   - `NoSuchMethodError` / `NoClassDefFoundError` / `IncompatibleClassChangeError` → API 境界が変わった
     - 小さな accessor / enum 差分 → **reflection-based shim** で吸収 (`./reflection-shim.md`)
     - method signature 差分 → 該当 compat module の同名メソッドを修正、または新 compat module
   - 単に **未知のクラスが存在しない** (例: `FirNamedFunction` が 2.3.20 で導入) → **capability flag** (`supportsFirCheckers()`) で feature を gating
   - kctfork 起動不能 → kctfork version map を更新 (Step 2)

**削除の場合**: Step 2 のファイル更新と SSOT 行削除のみ。 compat module は残したままで OK (将来再追加時の差分を最小化)。

### 1A-2: 新 compat module が必要な場合 (`delegation` パターン推奨)

新しい API 境界が生じた場合のみ実施。 共通実装は **最も近い既存 module に `by` で委譲し、 差分メソッドだけ override** する。 これにより 1 module あたりの行数を典型 10〜30 行に収められる。

```kotlin
// compiler-plugin/compat-k2320/src/main/kotlin/.../compat/k2320/CompatContextImpl.kt
package me.tbsten.compose.preview.lab.compiler.compat.k2320

import me.tbsten.compose.preview.lab.compiler.compat.CompatContext
import me.tbsten.compose.preview.lab.compiler.compat.k230.CompatContextImpl as K230Impl

class CompatContextImpl : CompatContext by K230Impl() {
    // override する差分メソッドだけ書く
    override fun supportsFirHintGeneration(): Boolean = true
    override fun supportsFirCheckers(): Boolean = true

    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.3.20"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
```

詳細手順 (build.gradle.kts / settings.gradle.kts / META-INF/services / ShadowJar 取り込み) は `./compat-module-setup.md` を参照。

---

## Step 1B: Source Set Separation の場合

`src/v{バージョン}/kotlin` または `src/pre_{バージョン}/kotlin` の命名規則で新しいソースディレクトリを追加する。

詳細コードは `./source-set-separation.md` を参照。

1. **ディレクトリ作成**: `src/v{major}_{minor}_{patch}/kotlin/`
2. **VersionSpecificAPIImpl を実装**: そのバージョン向けの処理を書く
3. **Gradle の動的ソースセット選択ロジック** に新バージョンの条件を追加

---

## Step 2: 共通ファイル更新

アーキテクチャによらず必ず更新する。

### 2-1: SSOT (`scripts/supported-kotlin-versions.txt`)

CI matrix と test script の **唯一のソース**。 ここに 1 行追加するだけで CI 全 job に伝搬する設計を推奨。

```
2.1.20
2.1.21
...
2.3.21
2.4.0-Beta2   # ← 新規行
```

### 2-2: CI Matrix (dynamic, fromJSON)

`.github/workflows/pull-request.yml` の `compiler-plugin-test` job は SSOT を JSON 配列に変換して matrix に渡す:

```yaml
resolve-supported-kotlin-versions:
  runs-on: ubuntu-latest
  outputs:
    list: ${{ steps.read.outputs.list }}
  steps:
    - uses: actions/checkout@v4
    - id: read
      shell: bash
      run: |
        # Drop blank lines and comments, then turn the remaining lines into a JSON array.
        list=$(grep -vE '^[[:space:]]*(#|$)' scripts/supported-kotlin-versions.txt | jq -R . | jq -sc .)
        echo "list=$list" >> "$GITHUB_OUTPUT"

compiler-plugin-test:
  needs: resolve-supported-kotlin-versions
  strategy:
    fail-fast: false   # 必須: 一部失敗しても他バージョンの結果を確認
    matrix:
      kotlin: ${{ fromJSON(needs.resolve-supported-kotlin-versions.outputs.list) }}
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with: { java-version: '17', distribution: 'temurin' }
    - uses: gradle/actions/setup-gradle@v5
    - run: ./scripts/compiler-plugin-test.sh "${{ matrix.kotlin }}"
```

**ポイント**:
- `fail-fast: false` 必須。 Beta / RC が失敗しても stable の結果を視認できるようにする
- `setup-java` の `java-version: 17` は **CI runner JVM の話**。 kctfork が新 JDK の version string をパースできない場合は Gradle 側で `javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }` を test task に pin する (= 後述 2-3 の Java 21 pin と関係)

完全な YAML テンプレート / per-version test script は `./ci-matrix.md`。

### 2-3: kctfork バージョンマップ (compat module の test 用)

unit test で kctfork (ZacSweers/kotlin-compile-testing) を使う場合、 テスト対象 Kotlin に合致した kctfork が必要。 大きなマップを作らず、 境界の when 式で十分:

```kotlin
// compiler-plugin/build.gradle.kts
val testKotlinVersion: String =
    (findProperty("test.kotlin") as String?) ?: libs.versions.kotlin.get()

val kctforkVersion: String = when {
    testKotlinVersion.startsWith("2.1") -> "0.10.0"           // K2JVMCompilerArguments.jvmDefaultStable absent before 2.2.0
    testKotlinVersion.startsWith("2.4") -> "0.13.0-alpha01"   // 2.4 系の API 変更追従
    else -> libs.versions.kctfork.get()                       // 通常は最新 stable
}
```

最新の kctfork は https://github.com/ZacSweers/kotlin-compile-testing/releases で確認。 新 minor 追加で kctfork も上げる必要があるかを check。

**Java toolchain pin**: kctfork が bundle する kotlin-compiler-embeddable は Java 26+ の version string をパースできない。 test task に必ず Java 21 toolchain を指定する:

```kotlin
tasks.withType<Test> {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
```

### 2-4: Compose Multiplatform マップ (KMP / CMP プロジェクトの場合)

integration test が CMP を使っている場合、 nightly / matrix で Kotlin → Compose バージョン対応を管理する。 JetBrains/compose-multiplatform Release Notes の "Supports Kotlin X.Y" を確認。

実例 (`.github/workflows/nightly-checking.yml`):

```yaml
matrix:
  include:
    - kotlin_version: '2.3.21'
      compose_version: '1.11.0-alpha04'
    - kotlin_version: '2.4.0-Beta2'
      compose_version: '1.11.0-alpha04'
      gradle_version: '9.4.1'   # 新 Kotlin で Gradle wrapper も上げる場合
```

Step では `sed -i` で `gradle/libs.versions.toml` の `kotlin = "..."` / `compose = "..."` 行を書き換えるだけで version 切り替え可能。

### 2-5: README

`README.md` と `README.ja.md` の Supported Kotlin Versions テーブルに行を追加/削除。 両方を必ず更新する。

---

## Step 3: テスト実行と切り分け

全バージョン × 全テストが GREEN になるまで繰り返す。

```bash
# 単体バージョンテスト (compat module + main JVM テスト)
./scripts/compiler-plugin-test.sh 2.4.0-Beta2

# 全バージョン (CI 相当)
for v in $(grep -vE '^[[:space:]]*(#|$)' scripts/supported-kotlin-versions.txt); do
  ./scripts/compiler-plugin-test.sh "$v" || echo "FAILED: $v"
done
```

スクリプトが存在しない場合は CI 相当のコマンドを手動で実行 (`-Ptest.kotlin=X.Y.Z` + `--rerun-tasks`)。

### 失敗パターン → 対応

| 症状                                | 原因                                                | 対処                                                                                |
| ---                                 | ---                                                 | ---                                                                                 |
| `NoSuchMethodError`                 | main 実装が内部で使う API が変わった                | `CompatContext` に新メソッドを追加し、 各 compat module で実装                      |
| `NoClassDefFoundError`              | 新バージョンで導入されたクラスを早期に load 試行    | `CompatContext.supportsXxx()` capability flag で gating (`./version-gating.md`) |
| `IncompatibleClassChangeError`      | binary signature 差分 (return type / param 等)      | reflection shim (`./reflection-shim.md`) で吸収                            |
| `Cannot parse 'X' as Java version`  | kctfork が新 JDK の version string をパース不可     | test JVM の toolchain を `JavaLanguageVersion.of(21)` に pin                        |
| `metadata version is X.Y.0`         | test classpath の Kotlin が baseline と乖離         | `compileTestKotlin` に `-Xskip-prerelease-check -Xskip-metadata-version-check`     |
| KGP 自体の initialize 失敗          | `force` resolution が main classpath にも波及       | `force` は `testCompileClasspath` / `testRuntimeClasspath` 限定                     |

詳細な再現手順と Gradle スニペットは `./troubleshooting.md`。

---

## Step 4: ドキュメント更新

テストが全バージョン GREEN になったら:

1. **`docs/support-kotlin-versions.md`** (= compat 構成と current support matrix の SSOT-doc)
   - support matrix 表に新バージョン行追加
   - "Binary incompatibilities absorbed via reflection" セクションに新規 shim を追記 (該当時のみ)
2. **CHANGELOG / current.md**: 対象バージョンの status を更新
3. **commit**: 変更を論理単位で分割
   - SSOT (`scripts/supported-kotlin-versions.txt`) + CI matrix 更新 → 1 commit
   - compat module 追加 (delegation impl + Factory + META-INF/services + settings.gradle.kts + main build.gradle.kts の `add(embedded.name, ...)`) → 1 commit
   - README / docs 更新 → 1 commit

---

## チェックリスト

- [ ] 対象バージョンの operation と既存アーキテクチャを確認
- [ ] compat module / ソースセット の追加・修正 (必要な場合のみ。 delegation pattern を使う)
- [ ] SSOT (`scripts/supported-kotlin-versions.txt`) に対象バージョンを追加/削除
- [ ] CI matrix が SSOT 駆動になっていれば自動反映 (`fail-fast: false` を確認)
- [ ] kctfork version selection を確認 (新 minor で要更新の可能性)
- [ ] Compose マップを更新 (KMP/CMP プロジェクトの場合)
- [ ] README 両言語 + `docs/support-kotlin-versions.md` に反映
- [ ] 全バージョンでテスト GREEN (Beta / RC は self-skip 可で OK)
- [ ] reflection shim を追加した場合は `IrXxxCompat.kt` 等にコメントで原因 issue (KT-xxxxx) を明記
- [ ] capability flag を追加した場合は KDoc に "Implementations" の真偽表を記載

---

## 参考リンク

- kctfork リリース: https://github.com/ZacSweers/kotlin-compile-testing/releases
- Compose Multiplatform リリース: https://github.com/JetBrains/compose-multiplatform/releases
- Kotlin リリース: https://github.com/JetBrains/kotlin/releases
- Real-world examples:
  - [ZacSweers/metro](https://github.com/ZacSweers/metro) — `compiler-compat/` (CompatContext + KotlinToolingVersion 原典)
  - [TBSten/compose-preview-lab](https://github.com/TBSten/compose-preview-lab) — 8 compat module + capability flag + reflection shim + dynamic CI matrix の実例
  - [kitakkun/multi-kotlin-support-example](https://github.com/kitakkun/multi-kotlin-support-example) — source set separation の参考

## 詳細リファレンス (本 skill 内)

- [`./compat-module-setup.md`](./compat-module-setup.md) — `CompatContext` SPI / delegation pattern / ShadowJar 設定 / 新 compat module 追加コマンド
- [`./source-set-separation.md`](./source-set-separation.md) — Source set 分離アプローチの詳細
- [`./ci-matrix.md`](./ci-matrix.md) — SSOT 駆動 dynamic matrix の YAML テンプレート / per-version test script / classpath isolation
- [`./kotlin-tooling-version.md`](./kotlin-tooling-version.md) — `KotlinToolingVersion` with Maturity (STABLE > RC > BETA > ALPHA > MILESTONE > DEV > SNAPSHOT) の比較ロジック
- [`./version-gating.md`](./version-gating.md) — capability flag (`supportsXxx()`) の設計 + テストの `isAtLeast(...)` self-skip
- [`./reflection-shim.md`](./reflection-shim.md) — 小さな差分を新 compat module なしで吸収する reflection shim (例: `IrDeclarationOriginCompat`)
- [`./troubleshooting.md`](./troubleshooting.md) — 失敗パターン別の原因と対処 (NoSuchMethodError / metadata mismatch / KGP 初期化エラー / Java toolchain)
