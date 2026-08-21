# kotlin-compiler-plugin-setup プロンプト

[English](./kotlin-compiler-plugin-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[kotlin-compiler-plugin-setup スキル](../skills/kotlin-compiler-plugin-setup.ja.md)の一回限りプロンプト版。スキルをインストールせずに、Kotlin Compiler Plugin のマルチモジュール Gradle プロジェクト — buildSrc convention plugins、compiler-plugin (FIR + IR)、Gradle plugin ラッパー、Kotlin Multiplatform runtime、ユニットテスト (kctfork + Kotest)、インテグレーションテスト — を一式セットアップする。

## Run

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-compiler-plugin-setup/PROMPT.md を取得して、その指示に従って実行して
```

## What it does

- セットアップ開始前にプロジェクト名・Group ID・Plugin ID・セットアップ範囲・Kotlin/Java バージョンを確認
- リポジトリを sparse clone し、`scripts/scaffold.sh` をそのまま実行 (手動転記なし)。`example/` の skeleton から、settings + version catalog、buildSrc convention plugins、AutoService + KSP 登録クラス群 (CommandLineProcessor / CompilerPluginRegistrar、`supportsK2 = true`、no-op IR transformer、FIR registrar) 付き compiler-plugin、KMP runtime、gradle-plugin ラッパー (KotlinCompilerPluginSupportPlugin)、kctfork + Kotest のユニットテスト、`kotlinCompilerPluginClasspath` を使う JVM/KMP インテグレーションテストを、名前・パッケージ・ID を全置換した状態で一括生成
- 生成結果をレビュー (Plugin ID・パッケージ・バージョン・置換漏れ)
- ビルド確認 (`jvmTest` + インテグレーションテスト実行) を行い、必要に応じて Multi-Kotlin Version Support の戦略も案内

## Referenced files

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から取得する:

- [skills/kotlin-compiler-plugin-setup/scripts/scaffold.sh](https://github.com/TBSten/skills/blob/main/skills/kotlin-compiler-plugin-setup/scripts/scaffold.sh) — sparse clone 後にローカル実行する scaffold script
- [skills/kotlin-compiler-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/example) — 完全なプロジェクト skeleton (Gradle ビルドファイル + `Example` prefix の Kotlin ソース)
- [skills/kotlin-compiler-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/references) — plugin 登録、Gradle plugin 実装、テストパターン、publish convention、multi-version セットアップの設計解説

## Related

- スキル版: [skills/kotlin-compiler-plugin-setup](../skills/kotlin-compiler-plugin-setup.ja.md) — `gh skill install tbsten/skills kotlin-compiler-plugin-setup` でインストール
