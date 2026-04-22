---
name: android-convention-plugins
description: >
  Set up a scalable Gradle Convention Plugins structure for Android.
  It uses a "Primitive" (functional units like Compose, Hilt, Room) and 
  "Module" (structural types like Feature, Library, Application) hierarchy.
  Use when requested: "Introduce convention plugins", "Set up build-logic", "Standardize module build logic".
---

# android-convention-plugins

Gradle Convention Plugins を使用して、Android プロジェクトのビルドロジックを共通化・構造化する。

## 構成の考え方
1. **DSL**: Gradle Kotlin DSL を使いやすくするための拡張関数。
2. **Primitive Plugins**: Hilt, Compose, Room などの「機能」単位のプラグイン。
3. **Module Plugins**: Library, Feature, Application などの「モジュール種別」単位のプラグイン。Module Plugins は複数の Primitive Plugins を組み合わせて構成する。

## 手順
1. `build-logic` モジュールを作成し、`settings.gradle.kts` で `includeBuild("build-logic")` する。
2. `build-logic/convention/src/main/kotlin` に DSL およびプラグインを配置する。
3. `libs.versions.toml` に必要なプラグインやライブラリを定義する。
4. 各モジュールの `build.gradle.kts` で `plugins { id("your.project.library") }` のように適用する。

## Resources
- `example/dsl/`: `AndroidDsl.kt`, `VersionCatalogDsl.kt` などの共通ユーティリティ。
- `example/primitive/`: `ComposePlugin.kt` などの機能別プラグイン例。
- `example/module/`: `AndroidLibraryModulePlugin.kt` などのモジュール別プラグイン例。
