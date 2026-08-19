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
- Gradle 基盤を作成: `settings.gradle.kts`、自プロジェクトの version も持つ version catalog、その catalog を共有する included build `buildLogic`、`ksp.incremental=false` を含む `gradle.properties`
- runtime モジュール (アノテーション宣言のみ・KMP 全ターゲット・`explicitApi()`) と、`-Xcontext-parameters` 付きの JVM only な ksp モジュールを作成
- processor を root 3 ファイル + `feature/` · `core/` · `options/` · `util/` でスキャフォールド。依存は一方向、context は層ごとに絞る
- KSP × KMP workaround 付きの `test` モジュールを作成 (kotest の per-target launcher 用に `*Test` の ksp タスクは残す)
- テスト基盤をコピー: kctfork による e2e コンパイル、facet 形式の Markdown golden、generator 駆動の snapshot matrix、診断の golden、Konsist アーキテクチャテスト
- GitHub Actions の matrix CI (と Release トリガの publish workflow) を追加
- path-scoped な `.claude/rules/*.md` を 5 ファイル生成先に配置し、以後もレイヤリングが強制されるようにする
- ビルドを確認し、golden の初回記録を行う

## Referenced files

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から取得する:

- [skills/ksp-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/example) — build ファイル、processor 骨組み、テスト基盤、CI workflow
- [skills/ksp-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/references) — ビルド/CI の詳細、processor の設計判断、テスト基盤ガイド
- [skills/ksp-plugin-setup/assets/rules/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/assets/rules) — `.claude/rules/*.md` のテンプレート

## Related

- スキル版: [skills/ksp-plugin-setup](../skills/ksp-plugin-setup.ja.md) — `gh skill install tbsten/skills ksp-plugin-setup` でインストール
- [kotlin-maven-central-publish](./kotlin-maven-central-publish.ja.md) — このプロンプトが委譲する Maven Central 公開の完全手順
