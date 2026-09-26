# kotlin-maven-central-publish

Kotlin/KMP プロジェクトに Maven Central 公開設定を追加するスキル。Vanniktech Maven Publish プラグイン、GPG 署名、GitHub Actions CI/CD を一括セットアップする。

## インストール

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

## 概要

このスキルは Kotlin / Kotlin Multiplatform プロジェクトの Maven Central 公開に必要な設定を自動生成する。機械的なセットアップは同梱の 2 つの script が担い、エージェントは script を読解・書き換え・再実装せずそのまま実行する:

- **`scripts/setup-publish.sh`** — version catalog への Vanniktech Maven Publish プラグイン (Gradle wrapper / Kotlin の版から互換バージョンを自動選択) と自ライブラリのバージョンの冪等追記、`buildSrc` または `build-logic` 等の included build への convention plugin (`publish-convention.gradle.kts`) のプレースホルダー置換済み生成（GitHub URL・ライセンス・開発者情報は `git remote` と `LICENSE` ファイルから自動推定）、GitHub Actions ワークフローの生成 (runner は Apple ターゲットの有無で自動選択) を一括で行う。構成に応じて書き換えが必要なプラグイン要求も報告する。再実行しても安全。既存ファイルは `--force` なしで上書きしない。結果は 1 行 JSON で出力
- **`scripts/setup-secrets.sh`** — GPG 鍵の生成 → 公開鍵のキーサーバー送信 → 秘密鍵 export → `gh secret set` による 5 つの GitHub Secrets 登録までを自動化する対話 script。残るユーザー手作業は Sonatype Central Portal の User Token 発行だけ。`--dry-run` 対応

## 生成されるファイル

| ファイル | 説明 |
|---|---|
| `<convention-dir>/src/main/kotlin/publish-convention.gradle.kts` | Central Portal 連携・条件付き署名・フラットな artifactId・POM メタデータの convention plugin (`<convention-dir>` は既定 `buildSrc`、`build-logic` 等も可) |
| `<convention-dir>/build.gradle.kts` | Vanniktech Maven Publish と Kotlin (/ Android) Gradle plugin を convention ビルドの classpath に載せる |
| `<convention-dir>/settings.gradle.kts` | ルートの version catalog の import と repositories 定義 |
| `gradle/libs.versions.toml` | Maven Publish プラグインと (任意で) 自ライブラリのバージョンを追加 |
| `.github/workflows/publish.yml` | Release 駆動の公開。tag とバージョンの一致・非 SNAPSHOT を検証、手動実行は dry-run / stage-only 対応 |
| `.github/workflows/publish-check.yml` | PR ごとに `publishToMavenLocal` を回し、公開設定の破損を早期検出 |

## 前提条件

- Gradle + version catalog を使用した Kotlin プロジェクト
- GitHub リポジトリ
- Sonatype Central Portal アカウント（namespace 登録済み）
- GPG 鍵（アーティファクト署名用。`scripts/setup-secrets.sh` で生成可能）

## 使い方

インストール後、以下のフレーズで呼び出す:
- 「Maven Central に公開したい」
- 「ライブラリを publish できるようにして」
- 「publishToMavenLocal できるようにして」

スキルはプロジェクト情報を収集し、`scripts/setup-publish.sh` で設定ファイルを生成、公開対象モジュールに convention plugin を適用し、`scripts/setup-secrets.sh` で GPG 鍵と GitHub Secrets をセットアップする。`references/gpg-setup.md` / `references/github-secrets.md` は script が使えない環境向けのフォールバック手動手順。

その他の参照ドキュメント:

- `references/convention-options.md` — buildSrc と build-logic の違い、署名方式、artifactId / coordinates、バージョンの SSoT (catalog か `VERSION_NAME` か、SNAPSHOT を使わない運用)、Dokka による javadoc、古い Kotlin 利用者向けの `languageVersion` / `apiVersion` 床
- `references/release-flow.md` — ワークフローの設計理由と、tag push 駆動 + GitHub Release 自動生成の代替フロー
- `references/troubleshooting.md` — よくあるエラー (classpath 競合、`SonatypeHost` 削除、`is final`、401、`BAD_PASSPHRASE` など)

## 技術的なポイント

- **Vanniktech Maven Publish** プラグイン (既定 0.37.0。古い Gradle / Kotlin では 0.35.0 / 0.34.0) — `SonatypeHost` は 0.34.0 で削除されたので `publishToMavenCentral()` を引数なしで呼ぶ
- **Sonatype Central Portal** をターゲット（レガシー OSSRH ではない）
- GPG 署名は**条件付き** — `*ToMavenLocal` タスクの時だけスキップ (path の末尾セグメントで判定)。CI で Secrets が欠けていれば即失敗する
- artifactId は `project.path` から導出 (`:ksp:processor` → `ksp-processor`)。各モジュールは POM の name / description だけ上書きする
- Kotlin Gradle plugin を Vanniktech と同じ convention ビルドの classpath に載せる (0.36 以降必須)
- `publishAndReleaseToMavenCentral` タスクで `--no-configuration-cache` 付きで実行

## 必要な GitHub Secrets

| Secret 名 | 説明 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal ユーザートークンのユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal ユーザートークンのパスワード |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID（フィンガープリント末尾 8 桁） |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 |
