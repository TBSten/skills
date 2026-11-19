# fuga-notes (Kotlin / Android)

## detekt 導入手順 (新規モジュール・新規リポジトリで 1 回だけ)

1. `gradle/libs.versions.toml` に `detekt = "1.23.7"` と plugin `io.gitlab.arturbosch.detekt` を追加
2. ルートの `build.gradle.kts` で `alias(libs.plugins.detekt) apply false`、各モジュールで `alias(libs.plugins.detekt)` を適用
3. `./gradlew detektGenerateConfig` で `config/detekt/detekt.yml` を生成し、`maxIssues: 0` と `formatting` ルールセットを有効化
4. 既存コードの違反は `./gradlew detektBaseline` で `config/detekt/baseline.xml` に逃がす
5. `.github/workflows/ci.yml` に `./gradlew detekt` ステップを追加 (社内ランナー `fuga-runner-01` を使う)
6. 導入したら #fuga-android に報告
