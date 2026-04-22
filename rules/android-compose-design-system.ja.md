# android-compose-design-system

Jetpack Compose におけるデザインシステムの構造化に関する規約。

## 特徴
- **責務の分離**: 色、タイポグラフィ、シェイプを個別のインターフェースとして定義します。
- **CompositionLocal**: `AppTheme.colors.primary` のように、テーマの設定値に静的にアクセスできます。
- **Material 3 連携**: `MaterialTheme` のカラースキームやタイポグラフィへの自動変換（ブリッジ）が含まれています。

## 使い方
1. `curl -fsSL https://rules.tbsten.me/i | bash -s -- android-compose-design-system` を実行します。
2. `ui/designSystem/` ディレクトリに参照ファイルが作成されます。
3. パッケージ名を調整し、プロジェクト固有のブランドカラーやスタイルを定義してください。
