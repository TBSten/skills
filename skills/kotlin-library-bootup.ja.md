# Kotlin Library Bootup Skill

[English](./kotlin-library-bootup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

普通の Kotlin / Kotlin Multiplatform ライブラリのプロジェクトを、実運用している OSS ライブラリ群
(koma-strict、katachi、compose-preview-lab、cream など) が到達した構成で一式立ち上げる
[Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill です。convention plugin、
バイナリ互換性チェック、Maven Central への publish、integrationTest ビルド、docs サイト、CI までを生成し、
ローカルでビルド・テスト・publish が通ることまで確認します。

テンプレートのコピー、ターゲット別ブロックの取捨、プレースホルダーの置換、rename、置換漏れの検証といった
機械的な作業は `scripts/scaffold.sh` が決定的に行います。`scripts/verify.sh` は Gradle wrapper を用意し、
ビルド確認を実行してログを `.local/tmp/` に保存します。エージェントは入力値の確認、script の実行、結果のレビューだけを担います。

## クイックスタート

### 1. スキルをインストール:

```bash
gh skill install tbsten/skills kotlin-library-bootup
```

### 2. AI エージェントに依頼:

```
Kotlin Multiplatform ライブラリを新規作成して
```

## ターゲット

| `--targets` | ターゲット |
|---|---|
| `standard` (既定) | android / jvm / js / wasmJs / iosArm64 / iosSimulatorArm64 |
| `full` | standard + macOS / watchOS / tvOS / Linux / Windows / androidNative / wasmWasi |
| `jvm` | `kotlin("jvm")` のみ。Android / Apple の設定と CI job は生成しない |

macOS では Xcode が入っている時だけ Apple ターゲットを有効にするので、Command Line Tools だけの環境でもビルドできます。
`-PenableAppleTargets=true` / `-PdisableAppleTargets=true` で上書きできます。

## セットアップされる内容

### プロジェクト構成

| パス | 説明 |
|---|---|
| `<name>-core/` | 最初のライブラリモジュール。`explicitApi()`、`@Internal<Name>Api` / `@Experimental<Name>Api` の opt-in マーカー、サンプル API と kotest の spec |
| `build-logic/` | convention plugin (`<name>.kmp` / `.jvm` / `.publish` / `.lint` / `.test`) を置く included build。ルートの version catalog を共有 |
| `integrationTest/` | Maven 座標でライブラリに依存し、`includeBuild("../")` でローカルに置き換える独立 Gradle ビルド (任意) |
| `docs/` | Astro Starlight のサイト (en / ja)。Dokka の API リファレンスを `/api-docs/` に置く (任意) |
| `.github/workflows/` | `ci.yml`、`publish.yml`、`docs.yml`、`clear-ci-cache.yml` (任意) |
| `README.md` / `README.ja.md` / `CLAUDE.md` / `.claude/rules/` | 人とエージェント向けのドキュメントと規約 |
| `.claude/skills/` | `bump-library-version` / `release-note` / `verify-changes` (任意) |

### ビルドと品質

| 要素 | 説明 |
|---|---|
| version catalog | ライブラリ自身の版とビルド定数まで含めた SSoT |
| kotest | `commonTest` の FreeSpec を全ターゲットで実行 (JS / Wasm / Native は KSP + kotest plugin、Android は host test を有効化) |
| binary-compatibility-validator | `apiDump` / `apiCheck`。KMP は klib も検証 |
| ktlint | ルールは `.editorconfig` に置く |
| 下位互換の床 | JVM / Android では Kotlin 2.2 から読める metadata で公開 |
| vanniktech maven-publish | project path から座標を導出、Dokka の javadoc jar、`publishToMavenLocal` 時は署名をスキップ |

### CI とリリース

| workflow | 説明 |
|---|---|
| `ci.yml` | lint / API チェック / プラットフォーム別テスト / publish の予行 / integrationTest / docs ビルドを並列に回し、`final` job で集約 |
| `publish.yml` | GitHub Release の `published` で起動。tag と catalog の版の一致と非 SNAPSHOT を確認し、CI を通してから Maven Central に publish |
| `docs.yml` | Dokka + Starlight をビルドして GitHub Pages にデプロイ |

## キーコンセプト

### 重複させずに委譲する

このスキルが作るのは普通のライブラリの土台だけです。KSP processor、compiler plugin、snapshot テスト、
Maven Central の Secrets、IntelliJ plugin は下記の専用 skill に任せます。ここで生成したプロジェクトの上に併用できます。

### すべての選択に出典がある

生成物の各手法は実運用しているライブラリから持ってきたもので、どこから来たか・なぜあるかを
`references/techniques.md` に記録しています。

### KMP での Kotlin の床

`coreLibrariesVersion` を下げると JS / Wasm のコンパイルが壊れます。stdlib の klib がコンパイラの ABI と
一致している必要があるためです。そこで JS / Wasm だけ stdlib をコンパイラの版で解決し、公開 metadata は床のまま
にしています。利用側に必要な Kotlin は、JVM / Android なら 2.2 以上、JS / Wasm / Native ならライブラリのビルドに
使った版以上です。

## 関連スキル

- [`ksp-plugin-setup`](./ksp-plugin-setup.ja.md) — KSP の symbol processor を作る時
- [`kotlin-compiler-plugin-setup`](./kotlin-compiler-plugin-setup.ja.md) / [`kotlin-compiler-plugin-dev`](./kotlin-compiler-plugin-dev.ja.md) — Kotlin の compiler plugin を作る時
- [`kmp-snapshot-testing-setup`](./kmp-snapshot-testing-setup.ja.md) — snapshot テストと property-based テスト
- [`kotlin-maven-central-publish`](./kotlin-maven-central-publish.ja.md) — publish 用の GPG 鍵と GitHub Secrets
- [`intellij-plugin-dev`](./intellij-plugin-dev.ja.md) — IntelliJ plugin を作る時
