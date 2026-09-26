# example-lib

[![Maven Central](https://img.shields.io/maven-central/v/<group-id>/example-lib-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/<group-id>/example-lib-core)
[![CI](https://github.com/<owner>/<repo>/actions/workflows/ci.yml/badge.svg)](https://github.com/<owner>/<repo>/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

[English](./README.md) |
日本語 |
[ドキュメント](https://<owner>.github.io/<repo>/ja/) |
[API リファレンス](https://<owner>.github.io/<repo>/api-docs/)

**目次:**
[セットアップ](#セットアップ) ·
[クイックスタート](#クイックスタート) ·
[モジュール](#モジュール) ·
[ドキュメント](#ドキュメント) ·
[開発](#開発) ·
[ライセンス](#ライセンス)

---

<description>

> [!IMPORTANT]
> example-lib は実験的なライブラリです。1.0.0 に達するまでは、マイナーリリースでも破壊的変更を含むことがあります。

## セットアップ

|                          |                                                                                                                  |
|--------------------------|------------------------------------------------------------------------------------------------------------------|
| `<example-lib-version>` | ![Maven Central](https://img.shields.io/maven-central/v/<group-id>/example-lib-core.svg?label=%20) |

Kotlin 2.2 以降が必要です。

### Kotlin Multiplatform

```kts
// module/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("<group-id>:example-lib-core:<example-lib-version>")
        }
    }
}
```

### JVM / Android

```kts
// module/build.gradle.kts
dependencies {
    implementation("<group-id>:example-lib-core:<example-lib-version>")
}
```

### Version catalog

```toml
# gradle/libs.versions.toml
[versions]
example-lib = "<example-lib-version>"

[libraries]
example-lib-core = { module = "<group-id>:example-lib-core", version.ref = "example-lib" }
```

## クイックスタート

```kt
import com.example.kotlinlibrarybootup.*

// TODO: example-lib の最小の利用例をここに書く。
```

| やりたいこと | API | ドキュメント |
|---|---|---|
| TODO: ユースケースを書く | `TODO()` | [ガイド](https://<owner>.github.io/<repo>/ja/guides/basic-usage/) |

## モジュール

| モジュール | 説明 | 公開 |
|---|---|:---:|
| `example-lib-core` | example-lib の中核 API | ✓ |
| `integrationTest` | 公開された Maven 座標経由で、利用者と同じ形で example-lib をテストする | ✗ |
| `build-logic` | 各モジュールで共有する convention plugin | ✗ |
| `docs` | ドキュメントサイト (Astro Starlight) | ✗ |

## ドキュメント

- **ドキュメント:** https://<owner>.github.io/<repo>/ja/
- **API リファレンス (Dokka):** https://<owner>.github.io/<repo>/api-docs/
- **AI エージェント向け:** https://<owner>.github.io/<repo>/llms.txt

## 開発

```bash
./gradlew build                  # 全体のビルドとテスト
./gradlew jvmTest                # 素早く確認: JVM テストのみ
./gradlew ktlintCheck apiCheck   # lint とバイナリ互換性チェック (CI で実行)
./gradlew apiDump ktlintFormat   # push 前に API dump を更新しフォーマット
./gradlew publishToMavenLocal    # ローカル確認用に ~/.m2 へ publish
./gradlew -p integrationTest test  # 結合テストを実行
./gradlew generateApiDocs        # Dokka HTML を docs/public/api-docs に生成
(cd docs && pnpm install && pnpm dev)  # ドキュメントサイトをプレビュー
```

## ライセンス

```
MIT License

Copyright (c) <year> <developer-name>
```

全文は [LICENSE](./LICENSE) を参照してください。
