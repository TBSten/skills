# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

example-lib は Kotlin ライブラリ。<description>

- 公開 artifact: `<group-id>:example-lib-core`
- パッケージ: `com.example.kotlinlibrarybootup`
- リポジトリ: https://github.com/<owner>/<repo>
<!-- @bootup:if docs-site -->
- ドキュメント: https://<owner>.github.io/<repo>/ (API リファレンスは `/api-docs/`)
<!-- @bootup:end -->

## Commands

```bash
# ビルドと全テスト
./gradlew build

# テスト (素早く回すなら jvmTest。全ターゲットは allTests)
./gradlew jvmTest
./gradlew allTests
# @bootup:if kmp
./gradlew :example-lib-core:jvmTest --tests "com.example.kotlinlibrarybootup.SomeSpec"
# @bootup:end
# @bootup:if jvm
./gradlew :example-lib-core:test --tests "com.example.kotlinlibrarybootup.SomeSpec"
# @bootup:end

# Lint (ktlint)
./gradlew ktlintCheck
./gradlew ktlintFormat

# バイナリ互換性 (BCV)。公開 API を変えたら apiDump して *.api の差分をコミットする
./gradlew apiCheck
./gradlew apiDump

# Maven Local へ publish (手元のアプリで試すとき)
./gradlew publishToMavenLocal

# @bootup:if integration-test
# 結合テスト (独立した Gradle ビルド。includeBuild("../") で本体を取り込む)
./gradlew -p integrationTest test

# @bootup:end
# @bootup:if docs-site
# API リファレンス (Dokka) を docs/public/api-docs に生成
./gradlew generateApiDocs

# ドキュメントサイト
(cd docs && pnpm install && pnpm build)
# @bootup:end
# @bootup:if !docs-site
# API リファレンス (Dokka) を build/api-docs に生成
./gradlew generateApiDocs
# @bootup:end
```

push 前は `./gradlew apiDump ktlintFormat` (IDE の `🟣 prepare push` run config) を回す。
<!-- @bootup:if ai-skills -->
変更箇所ごとにどのタスクを回すかは `.claude/skills/verify-changes/SKILL.md` を見る。
<!-- @bootup:end -->

## Modules

| モジュール | 役割 | 公開 |
|---|---|:---:|
| `example-lib-core` | ライブラリ本体。`explicitApi()` 有効 | ✓ |
<!-- @bootup:if integration-test -->
| `integrationTest/` | 独立 Gradle ビルド。Maven 座標で本体に依存し、利用者と同じ経路で検証する | ✗ |
<!-- @bootup:end -->
| `build-logic/` | convention plugin を置く included build | ✗ |
<!-- @bootup:if docs-site -->
| `docs/` | Astro Starlight のドキュメントサイト (Gradle とは独立。pnpm) | ✗ |
<!-- @bootup:end -->

モジュールを追加したら `settings.gradle.kts` の `include`、README (en/ja) の Modules 表、
`build.gradle.kts` の Dokka 集約 (`dokka(project(...))`) を合わせて更新する。

## Build logic

- `build-logic/` は `pluginManagement { includeBuild("build-logic") }` で取り込む convention plugin 群
<!-- @bootup:if kmp -->
  - `example-lib.kmp` / `example-lib.jvm`: Kotlin のターゲット・toolchain・`explicitApi()`・opt-in 設定
<!-- @bootup:end -->
<!-- @bootup:if jvm -->
  - `example-lib.jvm`: Kotlin の toolchain・`explicitApi()`・opt-in 設定・下位互換の床 (Kotlin 2.2)
<!-- @bootup:end -->
  - `example-lib.publish`: vanniktech maven-publish による Maven Central 向け POM・署名
  - `example-lib.lint`: ktlint
  - `example-lib.test`: kotest (JUnit Platform)
- バージョンの SSoT は `gradle/libs.versions.toml`。自ライブラリの版 (`example-lib`) もここにある
- ビルド定数 (jvmToolchain / compileSdk / minSdk など) も catalog に置き、値の理由をコメントで残す
- 詳細な規約は `.claude/rules/build-logic.md`

## Testing

- フレームワークは **kotest** (`FreeSpec`)。テスト名は日本語で intent を書き、連番を付けない
<!-- @bootup:if kmp -->
- テストは `commonTest` に置けるなら `commonTest` に置く
<!-- @bootup:end -->
<!-- @bootup:if jvm -->
- テストは `src/test/kotlin` に置く (`jvmTest` / `allTests` は `test` のエイリアス)
<!-- @bootup:end -->
- 詳細は `.claude/rules/test.md`

## Release

<!-- @bootup:if ai-skills -->
1. `gradle/libs.versions.toml` の `example-lib` を bump する (`.claude/skills/bump-library-version/`)
2. リリースノートを用意する (`.claude/skills/release-note/`)
<!-- @bootup:end -->
<!-- @bootup:if !ai-skills -->
1. `gradle/libs.versions.toml` の `example-lib` を bump する (README / docs の Maven 座標も合わせる)
2. リリースノートを用意する
<!-- @bootup:end -->
3. tag `v<version>` で GitHub Release を published にする
4. `.github/workflows/publish.yml` が走り、Maven Central に publish される (tag と catalog の版が一致しないと失敗する)

Maven Central / GPG の Secrets は `ORG_GRADLE_PROJECT_*` で渡す。登録手順はリポジトリの外 (kotlin-maven-central-publish skill) にある。

<!-- @bootup:if docs-site -->
## Docs

- `docs/` は Astro Starlight。英語が root、日本語が `/ja/`
- API リファレンスは `./gradlew generateApiDocs` が `docs/public/api-docs/` に出力する (gitignore 済み)
- `.github/workflows/docs.yml` が Dokka → Starlight をビルドして GitHub Pages にデプロイする
- 方針は `docs/CLAUDE.md`

<!-- @bootup:end -->
## Rules

`.claude/rules/` のルールに従うこと:

- [`kotlin.md`](.claude/rules/kotlin.md) — 可視性・explicitApi・opt-in アノテーション・例外・KDoc
- [`test.md`](.claude/rules/test.md) — kotest FreeSpec、テスト名、テストの置き場所
- [`build-logic.md`](.claude/rules/build-logic.md) — version catalog SSoT、convention plugin、コメント文化
- [`markdown.md`](.claude/rules/markdown.md) — README / docs の en/ja 同期

`.local/` は作業用 (チケット・ログ・一時ファイル)。コミットしない。
