---
name: kotlin-maven-central-publish
description: >
  Kotlin/KMP プロジェクトに Maven Central 公開設定を追加する。
  Vanniktech Maven Publish プラグインによる buildSrc convention plugin、
  GPG 署名、GitHub Actions CI/CD ワークフロー、Sonatype Central Portal 連携を
  一括でセットアップする。
  Use when requested: "Maven Central に公開したい", "ライブラリを publish したい",
  "Maven Central publishing をセットアップ", "publishToMavenLocal できるように",
  "Gradle で Maven Central 公開の設定", "Kotlin ライブラリを公開する設定を追加".
---

# kotlin-maven-central-publish

Kotlin/KMP プロジェクトに Maven Central 公開設定を追加する。

## 前提条件

- Kotlin プロジェクト (KMP または JVM)
- Gradle + version catalog (`gradle/libs.versions.toml`)
- `buildSrc/` ディレクトリが存在する（なければ作成する）
- GitHub でホスティングされていること（GitHub Actions を使用するため）

## Step 1: プロジェクト情報の収集

以下をユーザーに確認する。既に明確な場合はスキップしてよい。

1. **Maven Group ID** — 例: `com.example.mylib`
2. **アーティファクトバージョン** — 例: `0.1.0`
3. **ライセンス** — デフォルト: MIT License
4. **GitHub リポジトリ URL** — 例: `https://github.com/<owner>/<repo>`
5. **開発者情報** — ID, 名前, URL
6. **公開対象モジュール** — どのサブモジュールを Maven Central に公開するか

## Step 2: Version Catalog にプラグインを追加

`gradle/libs.versions.toml` に Vanniktech Maven Publish プラグインを追加する。

```toml
[versions]
mavenPublish = "0.30.0"

[plugins]
mavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "mavenPublish" }
```

## Step 3: buildSrc の設定

### buildSrc/build.gradle.kts

Vanniktech Maven Publish プラグインの依存を追加する。
`example/buildSrc-build.gradle.kts` を参照。

ポイント: `libs.plugins.mavenPublish.map { ... }` パターンで plugin ID を dependency 座標に変換する。

### publish-convention.gradle.kts

`buildSrc/src/main/kotlin/publish-convention.gradle.kts` を作成する。
`example/publish-convention.gradle.kts` をテンプレートとして使用し、Step 1 で収集した情報でプレースホルダーを置換する。

置換箇所:
- `<PROJECT_DESCRIPTION>` → プロジェクトの説明
- `<GITHUB_URL>` → GitHub リポジトリ URL
- `<INCEPTION_YEAR>` → プロジェクト開始年
- `<LICENSE_NAME>`, `<LICENSE_URL>` → ライセンス情報
- `<DEVELOPER_ID>`, `<DEVELOPER_NAME>`, `<DEVELOPER_URL>` → 開発者情報
- `<GITHUB_OWNER>`, `<GITHUB_REPO>` → GitHub owner/repo

## Step 4: 各モジュールに convention plugin を適用

公開対象モジュールの `build.gradle.kts` に以下を追加:

```kotlin
plugins {
    id("publish-convention")
}

group = "<GROUP_ID>"
version = libs.versions.<versionRef>.get()
```

## Step 5: GitHub Actions ワークフローの作成

`.github/workflows/publish.yml` を作成する。
`example/publish.yml` をテンプレートとして使用する。

このワークフローは:
- GitHub Release 作成時（released / prereleased）に自動実行
- 手動実行（workflow_dispatch）にも対応
- `publishAndReleaseToMavenCentral` タスクを実行

## Step 6: ローカル動作確認

```bash
./gradlew publishToMavenLocal
```

成功したら `~/.m2/repository/<group-path>/` にアーティファクトが生成されていることを確認。

## Step 7: ユーザーへの手動設定手順の案内

以下の手順をユーザーに案内する。詳細は `references/github-secrets.md` と `references/gpg-setup.md` を参照。

### 必要な GitHub Secrets (5つ)

| Secret 名 | 説明 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal ユーザートークンのユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal ユーザートークンのパスワード |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID（フィンガープリント末尾 8 桁） |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 |

### セットアップ手順（ユーザー作業）

1. **GPG 鍵の生成**: `references/gpg-setup.md` の手順に従う
2. **Sonatype Central Portal**: https://central.sonatype.com/ でアカウント作成、namespace 登録
3. **User Token の生成**: Central Portal → View Account → Generate User Token
4. **GitHub Secrets の登録**: リポジトリ Settings → Secrets and variables → Actions に 5 つの Secret を登録
5. **公開テスト**: GitHub Release を作成して publish ワークフローが成功することを確認
