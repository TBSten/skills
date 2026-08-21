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
- [`scripts/setup-publish.sh`](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-publish.sh) を `curl` で取得して実行 — `gradle/libs.versions.toml` への Vanniktech Maven Publish プラグインの冪等追記、buildSrc convention plugin (`publish-convention.gradle.kts`) のプレースホルダー置換済み生成 (GitHub URL・ライセンス・開発者情報は `git remote` と `LICENSE` ファイルから自動推定)、`.github/workflows/publish.yml` の生成を一括で行う
- 公開対象の各モジュールに convention plugin を適用 (エージェントの判断)
- `./gradlew publishToMavenLocal` でローカル動作確認
- [`scripts/setup-secrets.sh`](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-secrets.sh) を取得して実行 — GPG 鍵の生成、キーサーバー送信、秘密鍵 export、`gh secret set` による 5 つの GitHub Secrets 登録までを対話で自動化。残る手作業は Sonatype Central Portal の User Token 発行だけ

## 参照ファイル

ローカルへのスキルインストールの代わりに、以下の script を GitHub から取得して実行する (script は読解・書き換え・再実装せずそのまま実行する)。setup script は必要な `example/` テンプレートを GitHub raw から自動取得する:

- [scripts/setup-publish.sh](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-publish.sh) — catalog 追記・buildSrc convention plugin・publish ワークフローの一括セットアップ
- [scripts/setup-secrets.sh](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-secrets.sh) — GPG 鍵 + GitHub Secrets の対話セットアップ (`--dry-run` 対応)
- [references/github-secrets.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/github-secrets.md) / [references/gpg-setup.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/gpg-setup.md) — script が使えない環境向けのフォールバック手動手順

## 関連

- スキル版: [skills/kotlin-maven-central-publish](../skills/kotlin-maven-central-publish.ja.md) — `gh skill install tbsten/skills kotlin-maven-central-publish` でインストール
