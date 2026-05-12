# サポート Kotlin バージョン追加スキル

[English](./add-support-kotlin-version.md)

Kotlin Compiler Plugin プロジェクトのサポート対象 Kotlin バージョンを追加・削除する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
npx skills add tbsten/skills \
  --skill add-support-kotlin-version
```

### 2. AI エージェントに依頼:

```
このコンパイラプラグインプロジェクトに Kotlin 2.4.0 のサポートを追加して。
```

## このスキルでできること

複数バージョン対応基盤がすでにあるコンパイラプラグインプロジェクトに対し:

1. 対象 Kotlin バージョンが既存 compat module の範囲に収まるか、 reflection shim で済むか、 新 module が必要かを判定
2. 新 compat module は **delegation pattern** (`CompatContext by k230.CompatContextImpl()`) で 1 module 10〜30 行に収める
3. **SSOT** (`scripts/supported-kotlin-versions.txt`) を更新 → dynamic CI matrix (`fromJSON`) / kctfork version 選択 / version catalog / README に伝搬
4. `CompatContext` SPI の **capability flag** に新フラグを追加 (= 古い Kotlin で plugin が startup 失敗せず gracefully degrade)
5. SSOT の全エントリで per-version test スクリプトを実行して検証

## 対応アーキテクチャ

### A: Compat Module Layer (metro スタイル + capability flag)

`compiler-plugin/compat-kXX/` モジュールを持つプロジェクト。 ShadowJar で全 compat module を 1 jar にバンドルし、 `mergeServiceFiles()` で `META-INF/services` をマージ。 ServiceLoader が runtime の Kotlin 版に合致する `CompatContext` 実装を選ぶ。 ランクは `KotlinToolingVersion` (Maturity 付き: STABLE > RC > BETA > ALPHA > MILESTONE > DEV > SNAPSHOT) で決定。

`CompatContext` は 2 種類のメンバを持つ:

- **API shim** — `irCall(IrBuilderWithScope, ...)` のように JVM signature だけが版間で違うもの。 各 compat module は自分の baseline で compile されるため bytecode が runtime と一致する。
- **Capability flag** — `supportsFirHintGeneration()` / `supportsFirCheckers()` / `supportsKlibCrossModuleHint()` 等。 古い Kotlin で **extension の登録自体をスキップ** して FIR session crash を回避する。

### B: Source Set Separation (ソースセット分離)

`src/v2_0_0/kotlin/` や `src/pre_2_0_0/kotlin/` 形式のディレクトリを持つプロジェクト。 Gradle がビルド時に Kotlin バージョンに応じてソースディレクトリを切り替える。

K1 (PSI/ComponentRegistrar) と K2 (FIR/CompilerPluginRegistrar) の大きな断絶を吸収するのに最適。

## 主要コンセプト

### 新 compat module は delegation pattern

```kotlin
class CompatContextImpl : CompatContext by K230Impl() {
    override fun supportsFirHintGeneration(): Boolean = true   // 差分だけ
    override fun supportsFirCheckers(): Boolean = true

    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.3.20"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
```

新 compat module が必要になるのは:

- 対象バージョンが既存 module の範囲外、 かつ
- `compat/` レベルの reflection shim (例: `IrDeclarationOriginCompat`) で吸収できない、 かつ
- `NoSuchMethodError` / `NoClassDefFoundError` / `IncompatibleClassChangeError` が出て binary boundary が必要なとき

### SSOT 駆動の動的 CI Matrix

```yaml
resolve-supported-kotlin-versions:
  outputs:
    list: ${{ steps.read.outputs.list }}
  steps:
    - uses: actions/checkout@v4
    - id: read
      run: |
        list=$(grep -vE '^[[:space:]]*(#|$)' scripts/supported-kotlin-versions.txt | jq -R . | jq -sc .)
        echo "list=$list" >> "$GITHUB_OUTPUT"

compiler-plugin-test:
  needs: resolve-supported-kotlin-versions
  strategy:
    fail-fast: false           # 必須 — Beta/RC が落ちても stable の結果を視認可能
    matrix:
      kotlin: ${{ fromJSON(needs.resolve-supported-kotlin-versions.outputs.list) }}
  steps:
    - run: ./scripts/compiler-plugin-test.sh "${{ matrix.kotlin }}"
```

`scripts/supported-kotlin-versions.txt` に 1 行追加するだけで matrix が自動展開される。

### baseline vs runtime、 test classpath 隔離

main `compiler-plugin` モジュールは **1 つの baseline Kotlin** (= `libs.versions.kotlin`) で compile される。 `CompatContext` でカバーしていない API drift が runtime で `NoSuchMethodError` として顕在化するので、 これを skill が追跡する。

per-version test では `force` resolution を `testCompileClasspath` / `testRuntimeClasspath` 限定にする必要がある (= main classpath に波及すると KGP の buildtools 初期化が壊れる)。 `compileTestKotlin` には `testKotlinVersion != baseline` の時だけ `-Xskip-prerelease-check -Xskip-metadata-version-check` を付ける (常時付けると安全網が無効化)。

### kctfork バージョンマップ

```kotlin
val kctforkVersion = when {
    testKotlinVersion.startsWith("2.1") -> "0.10.0"           // 2.2+ で K2JVMCompilerArguments の差分
    testKotlinVersion.startsWith("2.4") -> "0.13.0-alpha01"   // 2.4 系 API 変更追従
    else -> libs.versions.kctfork.get()
}
```

kctfork test JVM は Java 21 で固定する (`javaLauncher.set(...)`) — kctfork bundle 中の Kotlin compiler が JDK 26 version string をパースできない。

### CI での `fail-fast: false`

GitHub Actions の matrix で `fail-fast: false` を設定すると、 実験的 / RC バージョンが失敗しても安定版のテスト結果が隠れずに確認できる。

## 前提条件

このスキルは複数バージョン対応基盤が既にあるプロジェクト向け。 初期セットアップは `kotlin-compiler-plugin-setup` スキルの Step 10 を参照。

## 参考リンク

- kctfork リリース: https://github.com/ZacSweers/kotlin-compile-testing/releases
- Compose Multiplatform リリース: https://github.com/JetBrains/compose-multiplatform/releases
- Kotlin リリース: https://github.com/JetBrains/kotlin/releases
- 実装例:
  - [ZacSweers/metro](https://github.com/ZacSweers/metro) — `CompatContext` / `KotlinToolingVersion` 原典
  - [TBSten/compose-preview-lab](https://github.com/TBSten/compose-preview-lab) — 8 compat module + capability flag + reflection shim + SSOT 駆動 CI matrix
  - [kitakkun/multi-kotlin-support-example](https://github.com/kitakkun/multi-kotlin-support-example) — source set 分離の参考
