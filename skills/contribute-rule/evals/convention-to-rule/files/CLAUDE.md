# hoge-shop-kmp

Kotlin Multiplatform (Android / iOS) の EC アプリ。社内ポータル: https://portal.hoge-shop.co.jp

## ビルド

- `./gradlew :composeApp:assembleDebug`

## Repository 層の戻り値規約

- Repository の public 関数は例外を投げず、`Result<T>` ではなく自前の `sealed interface Outcome<out T>` (`Success` / `Failure(error: AppError)`) を返す
- `AppError` は `Network` / `Unauthorized` / `NotFound` / `Unknown(cause)` の 4 種に限定する。新しい種類が必要なら `AppError` を拡張する PR を先に出す
- `runCatching` は `CancellationException` を握りつぶすので使わない。`try { } catch (e: CancellationException) { throw e } catch (e: Exception) { ... }` で書く
- ViewModel では `Outcome` を `when` で網羅し、`else` 分岐を書かない
- HTTP ステータスと `AppError` の対応表は Repository の基底クラス `HogeBaseRepository` に集約する (担当: 鈴木)
