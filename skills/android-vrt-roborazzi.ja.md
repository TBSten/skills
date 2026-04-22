# android-vrt-roborazzi

Roborazzi を使用した Android 向け自動 Visual Regression Testing (VRT) 導入スキル。

## 特徴
- **Roborazzi 連携**: Robolectric を使用して Compose Preview のスクリーンショットを撮影します。
- **Compose Preview Scanner**: プロジェクト内のすべての Preview を自動的に検出し、テストを実行します。
- **GitHub Actions**: ベースブランチとヘッドブランチの両方でスクリーンショットを撮影し、比較レポートを作成します。
- **PR コメント連携**: 比較結果（スクリーンショット）を PR の説明文に自動的に反映します。

## 前提条件
- Jetpack Compose を使用した Android プロジェクト。
- CI に GitHub Actions を使用していること。
- `reg-suit` によるレポート生成に Node.js が必要です。

## 使い方
1. `npx skills add tbsten/skills --skill android-vrt-roborazzi` を実行します。
2. `SKILL.md` の指示に従い、Gradle プラグインと CI ワークフローを設定します。
