---
paths:
  - "app/**/*.kt"
  - "ui/**/*.kt"
  - "domain/**/*.kt"
  - "data/**/*.kt"
---

App / UI / Domain / Data 層のコードを変更する際は、必ず以下のアーキテクチャドキュメントを読んでから作業すること。

- @docs/architecture/README.md
- 変更対象の層に対応するドキュメント:
  - アプリ全体 (app/**/*.kt) → @docs/architecture/app.md
  - UI・各画面のコード(ui/**/*.kt) → @docs/architecture/ui.md
  - UseCase/Domain モデルなどドメイン(domain/**/*.kt) → @docs/architecture/domain.md
  - API 通信・ローカル保存等のデータ操作 (data/**/*.kt) → @docs/architecture/data.md

## 新規 feature (画面) の追加

雛形を手書きせず、scaffold script を実行すること:

```sh
bash tools/kmp-layered-architecture/new-feature.sh <FeatureName> <package>
# 例: bash tools/kmp-layered-architecture/new-feature.sh Home com.example.app.ui.feature.home
```

script が追記できない既存ファイル (settings.gradle.kts / ui/navigation の Screen.kt /
AppNavigator.kt / AppNavigation.kt / DI Providers) への変更は、script が stdout に印字する
スニペットに従って追記する。

## 規約の機械検証

層間依存・`Providers` 命名・`Impl`/`Fake` 命名の規約は
`docs/architecture/templates/ArchitectureConventionTest.kt` (Konsist テストテンプレート) を
jvmTest に配置して機械検証する。未導入ならテンプレート冒頭の TODO 定数を差し替えて導入すること。
