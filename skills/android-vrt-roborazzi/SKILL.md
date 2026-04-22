---
name: android-vrt-roborazzi
description: >
  Set up Visual Regression Testing (VRT) for Android using Roborazzi.
  It includes Gradle plugin configuration, Compose Preview testing setup,
  and GitHub Actions workflow for automated PR reporting with screenshots.
  Use when requested: "Setup Roborazzi", "Configure VRT", "Add screenshot tests", "Automate VRT in CI".
---

# android-vrt-roborazzi

Android プロジェクトに Roborazzi を使用した Visual Regression Testing (VRT) を導入する。

## Usage
- Android Compose プロジェクトであることを確認する。
- `build-logic` (Convention Plugins) またはモジュールごとの `build.gradle.kts` に設定を適用する。

## 手順
1. **Roborazzi Plugin の作成**: `example/RoborazziPlugin.kt` を参考に、Gradle Plugin を作成する。
2. **Tester の作成**: `example/AppComposePreviewTester.kt` を `test` ソースセットに配置する。
3. **CI 設定**: `example/workflows/pull-request-vrt.yaml` を `.github/workflows/` に配置し、必要に応じてリポジトリ名やビルドコマンドを調整する。
4. **reg-suit 設定**: `example/regconfig.json` をルートディレクトリに配置する。
5. **PR Body 更新スクリプト**: `example/workflows/report-vrt-to-pr-body.js` を `.github/workflows/` に配置し、ワークフローから呼び出すように設定する。

## Resources
- `example/RoborazziPlugin.kt`: Roborazzi の設定を含む Gradle Plugin。
- `example/AppComposePreviewTester.kt`: Compose Preview をスキャンしてスクリーンショットを撮影するための Tester。
- `example/workflows/pull-request-vrt.yaml`: PR ごとに VRT を実行する GitHub Actions。
- `example/regconfig.json`: reg-suit の基本設定。
