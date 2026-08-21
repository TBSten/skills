# ksp-plugin-setup プロンプト

[English](./ksp-plugin-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[ksp-plugin-setup スキル](../skills/ksp-plugin-setup.ja.md)の一回限りプロンプト版。スキルを
インストールせずに、KSP plugin (Symbol Processor) プロジェクトを
[cream.kt](https://github.com/TBSten/cream) が到達した構成 — runtime / ksp / test の 3 モジュール、
4 層の processor 設計、golden ファイルによるテスト基盤、生成先に配置するアーキテクチャルール —
で一式スキャフォールドする。

## Run

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/ksp-plugin-setup/PROMPT.md を取得して、その指示に従って実行して
```

## What it does

- 開始前にプロジェクト名・パッケージ名/Group ID・最初のアノテーション名・セットアップ範囲・Kotlin/KSP バージョンを確認
- リポジトリを sparse clone して `scripts/scaffold.sh` を実行する。script が `example/` のコピー・ソースセットへのディレクトリ再マッピング・全プレースホルダー置換・`Greeting*` / `Example*` 系の rename・`META-INF/services` の配置・path-scoped な `.claude/rules/*.md` 5 ファイルの配置を決定的に行う (AI が手作業でコピー・置換しない)
- スキャフォールドされる内容: Gradle 基盤 (SSoT の version catalog、included build `buildLogic`、`ksp.incremental=false`)、runtime モジュール (アノテーション宣言のみ・KMP 全ターゲット)、JVM only の ksp モジュール (root 3 ファイル + `feature/` · `core/` · `options/` · `util/`、依存は一方向)、KSP × KMP workaround 付きの `test` モジュール、kctfork + golden + Konsist のテスト基盤、GitHub Actions matrix CI
- 配置ファイル一覧とレイヤリングをレビューし、`scripts/verify.sh` (4 つのビルド確認、ログは `.local/tmp/`) を実行して golden の初回記録と中身の確認を行う

## Referenced files

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から sparse clone して使う:

- [skills/ksp-plugin-setup/scripts/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/scripts) — `scaffold.sh` (コピー / 置換 / rename の仕様の SSoT) と `verify.sh` (ビルド確認)
- [skills/ksp-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/example) — build ファイル、processor 骨組み、テスト基盤、CI workflow
- [skills/ksp-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/references) — ビルド/CI の詳細、processor の設計判断、テスト基盤ガイド
- [skills/ksp-plugin-setup/assets/rules/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/assets/rules) — `.claude/rules/*.md` のテンプレート

## Related

- スキル版: [skills/ksp-plugin-setup](../skills/ksp-plugin-setup.ja.md) — `gh skill install tbsten/skills ksp-plugin-setup` でインストール
- [kotlin-maven-central-publish](./kotlin-maven-central-publish.ja.md) — このプロンプトが委譲する Maven Central 公開の完全手順
