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
- マルチモジュールのプロジェクトルート (`settings.gradle.kts`) と version catalog (`gradle/libs.versions.toml`) を作成
- buildSrc convention plugins (kotlin-jvm、JUnit5 + テストログ設定) をセットアップ
- AutoService + KSP 登録付きの compiler-plugin モジュールを作成 (CommandLineProcessor / CompilerPluginRegistrar、`supportsK2 = true`)
- runtime モジュール (Kotlin Multiplatform API 宣言) と gradle-plugin モジュール (KotlinCompilerPluginSupportPlugin) を作成
- kctfork (インメモリ KotlinCompilation) + Kotest のユニットテストと、`kotlinCompilerPluginClasspath` を使う JVM/KMP インテグレーションテストモジュールをセットアップ
- ビルド確認 (`jvmTest` + インテグレーションテスト実行) を行い、必要に応じて Multi-Kotlin Version Support の戦略も案内

## Referenced files

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から取得する:

- [skills/kotlin-compiler-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/example) — Gradle ビルドファイルのテンプレート (settings、version catalog、buildSrc、各モジュール)
- [skills/kotlin-compiler-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/references) — plugin 登録、Gradle plugin 実装、テストパターン、publish convention、multi-version セットアップ

## Related

- スキル版: [skills/kotlin-compiler-plugin-setup](../skills/kotlin-compiler-plugin-setup.ja.md) — `gh skill install tbsten/skills kotlin-compiler-plugin-setup` でインストール
