# kotlin-maven-central-publish

Kotlin/KMP プロジェクトに Maven Central 公開設定を追加するスキル。Vanniktech Maven Publish プラグイン、GPG 署名、GitHub Actions CI/CD を一括セットアップする。

## インストール

```sh
npx skills add tbsten/skills \
  --skill kotlin-maven-central-publish
```

## 概要

このスキルは Kotlin / Kotlin Multiplatform プロジェクトの Maven Central 公開に必要な設定を自動生成する:

- **buildSrc convention plugin** (`publish-convention.gradle.kts`) — Vanniktech Maven Publish プラグインを使用
- **GitHub Actions ワークフロー** (`publish.yml`) — GitHub Release 作成時に自動公開
- **POM メタデータ** — ライセンス、開発者情報、SCM 情報
- **条件付き GPG 署名** — 鍵が利用可能な場合のみ有効

## 生成されるファイル

| ファイル | 説明 |
|---|---|
| `buildSrc/src/main/kotlin/publish-convention.gradle.kts` | Sonatype Central Portal 連携、署名、POM メタデータの convention plugin |
| `buildSrc/build.gradle.kts` | Vanniktech Maven Publish 依存を追加 |
| `gradle/libs.versions.toml` | Maven Publish プラグインのバージョンを追加 |
| `.github/workflows/publish.yml` | 自動公開用 GitHub Actions ワークフロー |

## 前提条件

- Gradle + version catalog を使用した Kotlin プロジェクト
- GitHub リポジトリ
- Sonatype Central Portal アカウント（namespace 登録済み）
- GPG 鍵（アーティファクト署名用）

## 使い方

インストール後、以下のフレーズで呼び出す:
- 「Maven Central に公開したい」
- 「ライブラリを publish できるようにして」
- 「publishToMavenLocal できるようにして」

スキルがプロジェクト情報の収集、設定ファイルの生成、Secrets・認証情報の手動設定手順の案内を行う。

## 技術的なポイント

- **Vanniktech Maven Publish** プラグイン (v0.30.0+) で Maven Central 連携を簡略化
- **Sonatype Central Portal** をターゲット（レガシー OSSRH ではない）
- GPG 署名は**条件付き** — ローカル開発時はスキップ、CI で Secrets が提供された場合のみ有効
- `libs.plugins.mavenPublish.map { ... }` パターンで plugin ID を buildSrc の依存座標に変換
- `publishAndReleaseToMavenCentral` タスクで `--no-configuration-cache` 付きで実行

## 必要な GitHub Secrets

| Secret 名 | 説明 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal ユーザートークンのユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal ユーザートークンのパスワード |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID（フィンガープリント末尾 8 桁） |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 |
