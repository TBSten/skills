---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

`@EnvConfig` を付けた interface から環境変数を読む実装クラスを生成する KSP processor を作りたいです。社内ツールなので KMP の統合テストモジュール (test/) と GitHub Actions の CI は要りません。.claude/rules の規約ファイルは置いてほしいです。

- プロジェクト名: env-config
- パッケージ: io.github.hoge.envconfig
- アノテーション: EnvConfig
- GitHub: hoge/env-config

./env-config ディレクトリにセットアップしてください (ビルド確認は不要)。あと、将来的に Maven Central にも出したいんですが、そのための GPG 鍵とか Secrets の設定ってどう進めればいいですか？
