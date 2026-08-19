# kotlin-maven-central-publish Prompt

[English](./kotlin-maven-central-publish.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[kotlin-maven-central-publish スキル](../skills/kotlin-maven-central-publish.ja.md)の一回限りプロンプト版。Kotlin/KMP プロジェクトに Maven Central 公開設定 — Vanniktech Maven Publish による buildSrc convention plugin、GPG 署名、Sonatype Central Portal 向け GitHub Actions CI/CD ワークフロー — をスキルのインストールなしでセットアップする。

## 実行方法

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-maven-central-publish/PROMPT.md を取得して、その指示に従って実行して
```

## やること

- プロジェクト情報の収集 (Group ID、バージョン、ライセンス、GitHub URL、開発者情報、公開対象モジュール)
- `gradle/libs.versions.toml` に Vanniktech Maven Publish プラグインを追加
- buildSrc convention plugin (`publish-convention.gradle.kts`) を作成 — Sonatype Central Portal 連携、条件付き GPG 署名、POM メタデータ
- 公開対象の各モジュールに convention plugin を適用
- GitHub Release 作成時にトリガーされる `.github/workflows/publish.yml` を作成 (`workflow_dispatch` にも対応)
- `./gradlew publishToMavenLocal` でローカル動作確認
- 手動設定手順の案内: GPG 鍵の生成、Sonatype Central Portal アカウント、必要な 5 つの GitHub Secrets

## 参照ファイル

ローカルへのスキルインストールの代わりに、以下を GitHub から取得する:

- [example/buildSrc-build.gradle.kts](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/example/buildSrc-build.gradle.kts) — Vanniktech Maven Publish 依存を追加した buildSrc ビルドスクリプト
- [example/publish-convention.gradle.kts](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/example/publish-convention.gradle.kts) — プレースホルダー付き convention plugin テンプレート
- [example/publish.yml](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/example/publish.yml) — GitHub Actions 公開ワークフローのテンプレート
- [references/github-secrets.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/github-secrets.md) — 必要な GitHub Secrets と取得方法
- [references/gpg-setup.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/gpg-setup.md) — GPG 鍵の生成・エクスポート手順

## 関連

- スキル版: [skills/kotlin-maven-central-publish](../skills/kotlin-maven-central-publish.ja.md) — `gh skill install tbsten/skills kotlin-maven-central-publish` でインストール
