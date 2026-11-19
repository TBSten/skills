# moge-compose-app (Kotlin / Compose Multiplatform)

## A. 新規 feature モジュールの作成手順 (モジュールを作るときに 1 回)

1. `feature/<name>/` を作り、`build.gradle.kts` に `id("moge.feature")` convention plugin を適用
2. `settings.gradle.kts` に `include(":feature:<name>")` を追加
3. `feature/<name>/src/commonMain/kotlin/.../<Name>Screen.kt` と `<Name>ViewModel.kt` の雛形を作成
4. `app` モジュールの navigation graph に route を登録

## B. Composable の命名・引数規約 (常に守る)

- 画面ルートは `XxxScreen`、状態を持たない部品は名詞で命名し `@Composable` 関数は PascalCase
- 第 1 引数以外の省略可能引数の先頭に `modifier: Modifier = Modifier` を置く
- `ViewModel` を受け取るのは `XxxScreen` だけ。部品には state と lambda を渡す

## C. スクリーンショットテストの更新フロー (UI を変えるたびに毎回)

1. `./gradlew recordRoborazziDebug` で差分を記録
2. `build/outputs/roborazzi/` の差分画像を確認し、意図しない差分があれば修正
3. 意図した差分なら `./gradlew verifyRoborazziDebug` が通ることを確認してコミット
4. PR 本文に before / after 画像を貼る

## D. 社内 VPN と検証環境への接続

- `vpn.moge-corp.internal` に GlobalProtect で接続し、`stg-api.moge-corp.internal` を hosts に追加する
- 検証用アカウントは 1Password の「moge-stg」Vault を参照
