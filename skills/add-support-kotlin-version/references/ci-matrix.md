# CI Matrix と kctfork バージョンマップの設定詳細

## GitHub Actions Matrix の構成

### 基本構成

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  # 1. サポートバージョンリストの生成 (SSOT が別ファイルの場合)
  versions:
    runs-on: ubuntu-latest
    outputs:
      list: ${{ steps.read.outputs.list }}
    steps:
      - uses: actions/checkout@v4
      - id: read
        run: |
          # scripts/supported-kotlin-versions.txt から JSON 配列を生成
          LIST=$(cat scripts/supported-kotlin-versions.txt | jq -R . | jq -sc .)
          echo "list=$LIST" >> "$GITHUB_OUTPUT"

  # 2. 固定バージョンでの高速テスト
  unit-pinned:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew :compiler-plugin:test :runtime:jvmTest

  # 3. 全バージョンマトリクステスト
  unit-matrix:
    needs: versions
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false   # 必須: 一部失敗しても他バージョンの結果を確認
      matrix:
        kotlin: ${{ fromJSON(needs.versions.outputs.list) }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew :compiler-plugin:test -Ptest.kotlin=${{ matrix.kotlin }}

  # 4. Integration test (smoke test)
  integration:
    needs: versions
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        kotlin: ${{ fromJSON(needs.versions.outputs.list) }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./scripts/smoke-test.sh ${{ matrix.kotlin }}
```

### バージョンリスト SSOT ファイル

`scripts/supported-kotlin-versions.txt` (新しい順):
```
2.4.0-Beta1
2.3.20
2.3.10
2.2.20
2.2.0
2.1.21
2.1.20
2.0.21
2.0.0
```

---

## kctfork バージョンマップ

unit test で kctfork (ZacSweers/kotlin-compile-testing) を使う場合、テスト対象の Kotlin バージョンに対応した kctfork が必要。

### Gradle での設定例

```kotlin
// compiler-plugin/build.gradle.kts

val kctforkForKotlin = mapOf(
    "2.0.0"        to "0.5.0",
    "2.0.20"       to "0.5.0",
    "2.0.21"       to "0.6.0",
    "2.1.0"        to "0.7.1",
    "2.1.20"       to "0.7.1",
    "2.1.21"       to "0.7.1",
    "2.2.0"        to "0.8.0",
    "2.2.10"       to "0.9.0",
    "2.2.20"       to "0.11.0",
    "2.2.21"       to "0.11.0",
    "2.3.0"        to "0.12.1",
    "2.3.10"       to "0.12.1",
    "2.3.20"       to "0.12.1",
    "2.4.0-Beta1"  to "0.12.1",  // RC/Beta は直近安定版を流用
)

val kotlinTestVersion = providers.gradleProperty("test.kotlin").orNull
    ?: libs.versions.kotlin.get()

// startsWith で minor 系統を吸収 (2.3.21 等の新 patch を自動対応)
val kctforkVersion = kctforkForKotlin[kotlinTestVersion]
    ?: kctforkForKotlin.entries
        .filter { (k, _) -> kotlinTestVersion.startsWith(k.substringBeforeLast(".")) }
        .maxByOrNull { (k, _) -> k }?.value
    ?: kctforkForKotlin.values.last()

dependencies {
    testImplementation("com.github.ZacSweers.kotlin-compile-testing:kotlin-compile-testing-core:$kctforkVersion")
}
```

**新 minor バージョン追加時**: ZacSweers/kotlin-compile-testing の Releases ページで対応バージョンを確認。
RC/Beta は通常直近 stable の kctfork をそのまま流用できる。

---

## Smoke Test スクリプトの例

```bash
#!/usr/bin/env bash
# scripts/smoke-test.sh <kotlin-version>
set -e

KOTLIN_VERSION="${1:?Usage: smoke-test.sh <kotlin-version>}"

echo "=== Smoke test for Kotlin $KOTLIN_VERSION ==="

# Step 1: プラグインをローカル Maven に publish
./gradlew publishToMavenLocal -Pplugin.version="0.0.0-smoke"

# Step 2: integration-test を対象バージョンでビルド
cd integration-test
./gradlew check \
  -Pintegration.kotlin="$KOTLIN_VERSION" \
  -Pplugin.version="0.0.0-smoke" \
  --refresh-dependencies

# Step 3: (任意) バイトコードでシンボル確認
CLASSFILE=$(find . -name "*.class" | grep -i "example" | head -1)
if [ -n "$CLASSFILE" ]; then
  javap -p "$CLASSFILE" | grep -q "expectedSymbol" || {
    echo "ERROR: expected symbol not found in bytecode"
    exit 1
  }
fi

echo "=== Smoke test PASSED for Kotlin $KOTLIN_VERSION ==="
```

---

## 環境変数によるバージョン上書き

`settings.gradle.kts` に以下を追加すると、CI と手動テストの両方で `KOTLIN_VERSION` 環境変数による切り替えが可能になる:

```kotlin
// settings.gradle.kts
val kotlinOverride = providers.environmentVariable("KOTLIN_VERSION").orNull
    ?: providers.gradleProperty("integration.kotlin").orNull

if (kotlinOverride != null) {
    dependencyResolutionManagement {
        versionCatalogs {
            named("libs") {
                version("kotlin", kotlinOverride)
            }
        }
    }
}
```

手動での任意バージョンテスト:
```bash
KOTLIN_VERSION=2.1.0 ./gradlew :compiler-plugin:test
# または
./gradlew :compiler-plugin:test -Ptest.kotlin=2.1.0
```

---

## Gradle キャッシュの考慮

CI でキャッシュが古い結果を返す場合、`--rerun-tasks` または `--refresh-dependencies` を使う:

```bash
./gradlew :compiler-plugin:test -Ptest.kotlin=X.Y.Z --rerun-tasks
```

テスト結果が `UP-TO-DATE` と表示される場合は必ず `--rerun-tasks` を付けてキャッシュをバイパスする。
