# android-convention-plugins

Android プロジェクト向けの拡張性の高い Gradle Convention Plugins 構成。

## 特徴
- **責務の分離**: 機能単位のプラグイン (Primitive) と構造単位のプラグイン (Module) を分けることで、再利用性と見通しを向上させます。
- **再利用可能な DSL**: Gradle Kotlin DSL をより簡潔かつ型安全に記述するための拡張関数を提供します。
- **ビルドロジックの集約**: 各モジュールの `build.gradle.kts` に散らばっている共通設定をプラグインとして集約します。

## 前提条件
- Gradle Kotlin DSL を使用した Android プロジェクト。
- 依存関係管理に Version Catalog (`libs.versions.toml`) を使用していること。

## 使い方
1. `npx skills add tbsten/skills --skill android-convention-plugins` を実行します。
2. `SKILL.md` の指示に従い、`build-logic` モジュールの作成とビルドロジックの移行を行います。
