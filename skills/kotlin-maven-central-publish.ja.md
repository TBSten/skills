# kotlin-maven-central-publish

Kotlin/KMP プロジェクトに Maven Central 公開設定を追加するスキル。Vanniktech Maven Publish プラグイン、GPG 署名、GitHub Actions CI/CD を一括セットアップする。

## インストール

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

## 概要

このスキルは Kotlin / Kotlin Multiplatform プロジェクトの Maven Central 公開に必要な設定を自動生成する。機械的なセットアップは同梱の 2 つの script が担い、エージェントは script を読解・書き換え・再実装せずそのまま実行する:

- **`scripts/setup-publish.sh`** — version catalog への Vanniktech Maven Publish プラグインの冪等追記、buildSrc convention plugin (`publish-convention.gradle.kts`) のプレースホルダー置換済み生成（GitHub URL・ライセンス・開発者情報は `git remote` と `LICENSE` ファイルから自動推定）、GitHub Actions ワークフローの生成を一括で行う。再実行しても安全。既存ファイルは `--force` なしで上書きしない。結果は 1 行 JSON で出力
- **`scripts/setup-secrets.sh`** — GPG 鍵の生成 → 公開鍵のキーサーバー送信 → 秘密鍵 export → `gh secret set` による 5 つの GitHub Secrets 登録までを自動化する対話 script。残るユーザー手作業は Sonatype Central Portal の User Token 発行だけ。`--dry-run` 対応

## 生成されるファイル

| ファイル | 説明 |
|---|---|
| `buildSrc/src/main/kotlin/publish-convention.gradle.kts` | Sonatype Central Portal 連携、署名、POM メタデータの convention plugin |
| `buildSrc/build.gradle.kts` | Vanniktech Maven Publish 依存を追加 |
| `buildSrc/settings.gradle.kts` | ルートの version catalog の import と buildSrc 用 repositories 定義 |
| `gradle/libs.versions.toml` | Maven Publish プラグインのバージョンを追加 |
| `.github/workflows/publish.yml` | 自動公開用 GitHub Actions ワークフロー |

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
