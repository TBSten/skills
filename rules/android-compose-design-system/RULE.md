# android-compose-design-system

Android Compose において、MaterialTheme をラップし、プロジェクト固有の設定値（Colors, Typography, Shapes）を型安全に扱うためのデザインシステムを構築する。

## ルールの内容
- **インターフェースによる定義**: `AppColors`, `AppTextStyles`, `AppShapes` をインターフェースで定義し、具体的な設定値を `DefaultAppColors` 等の object で実装する。
- **CompositionLocal の使用**: 各設定値を `CompositionLocal` で提供し、`AppTheme.colors.primary` のように静的にアクセスできるようにする。
- **MaterialTheme とのブリッジ**: `asMaterial` 拡張プロパティを用意し、`AppTheme` の値を `MaterialTheme` (Material3) に変換して適用する。
- **ファイルの分離**: 以下の 4 つのファイルに分割して管理する。
    - `AppColors.kt`: 色の定義
    - `AppTextStyles.kt`: タイポグラフィの定義
    - `AppShapes.kt`: シェイプの定義
    - `AppTheme.kt`: Theme オブジェクトと CompositionLocalProvider の定義

## 実装時の注意
- プロジェクト固有のパッケージ名に調整すること。
- 必要に応じて Light/Dark モードの切り替えロジックを追加すること。
