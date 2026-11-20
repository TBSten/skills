# mobile / kmp（Kotlin Multiplatform・Compose Multiplatform）

検証状況: macOS 26.2・JDK 17・Kotlin 2.4.10・Compose Multiplatform 1.12.0-rc01・AGP 9.1.1 で、画面 1 枚の最小アプリ（androidApp・sharedUI・desktopApp・webApp・iosApp の構成）を 4 つのターゲットで動かして確かめた。
- **Android**（エミュレータ API 36.1。`adb` で操作）・**iOS**（Xcode 26.2・iOS 26.2 シミュレータ。Maestro 2.3.0 で操作）・**Web**（wasmJs を Chrome で開き、Playwright で座標クリック）: FB で入る → 部品をタップ（下のボタンにタップが届かない）→ 親を選ぶ → コメント → 送る → 受け口の Inbox に schema 1 の記録 + `window.png`・`element.png`。
- **Desktop**（JVM 25）: アプリは `./gradlew :desktopApp:run -Pfeedback=true` で起動するところまで。操作はウィンドウのクリックではなく、同じ `App()` + `installFeedback()` を Compose の UI テスト（`runDesktopComposeUiTest`）で動かし、実際の受け口に 1 件送って確かめた。
- 本番から外す: Android の release APK（R8 あり）・iOS の Release の `.app`・Desktop の `createDistributable`・Web の `wasmJsBrowserDistribution` のどれにも、フィードバックのコード・URL が残らない（スイッチを入れたビルドでは見つかることも確かめた）。
- Web は受け口に届かないとき、理由が吹き出しに出てモードが続くことも確かめた。

未検証: iOS 実機・Desktop のウィンドウを人の操作で触ること・JS（Kotlin/JS）ターゲット・別ウィンドウ（`Dialog`・`Popup`）の中・スクロールする一覧。

ひな形:
- `assets/templates/adapters/mobile/compose/`（5 枚。Android 単体と同じもの）→ `:feedback` モジュールの `commonMain`
- `assets/templates/adapters/mobile/kmp/feedback/<target>/` → `:feedback` の各ターゲットのソースセット: `PlatformFeedback.kt`（commonMain の `expect` と `platformFeedbackConfig`）、`PlatformFeedback.{android,ios,jvm,wasmJs}.kt`（HTTP の送信・PNG・env）
- `kmp/shared/FeedbackHooks.kt` → 画面のある共有モジュール（`sharedUI` など）の `commonMain`。差し込み口だけで、フィードバックのコード・URL は持たない
- `kmp/setup/FeedbackSetup.kt`・`InstallFeedback.on.kt` → フィードバックを入れるビルドのソース、`InstallFeedback.off.kt` → 入れないビルドのソース

依存は Compose の runtime・ui・foundation と coroutines だけ（Ktor・kotlinx.serialization は足さない）。先に `common.md` を読む。

## 形

```
sharedUI（commonMain）: 画面。Modifier.fbTarget("…") と FeedbackWrap { … } だけを使う（FeedbackHooks 経由）
:feedback（KMP モジュール）: compose/ の本体 + 各ターゲットの送信・PNG
入れるビルドだけ: installFeedback() が FeedbackHooks に :feedback の中身を差し込む
```

- `App()` の一番外を `FeedbackWrap { … }` で包み、部品に `Modifier.fbTarget("画面/部品")` を付ける（`com.example.app.debughooks` の方を import する）。
- 起動時に `installFeedback()` を 1 回呼ぶ（Android は `setContent` の前、iOS は `MainViewController()` の中、Desktop は `application` の前、Web は `ComposeViewport` の前）。

## 本番から外す（ターゲットごとのスイッチ）

| ターゲット | 入れるとき | 外し方 |
|---|---|---|
| Android | debug ビルド | `androidApp` の `debugImplementation(project(":feedback"))`。`src/debug/` に `InstallFeedback.on.kt` と `FeedbackSetup.kt`、`src/release/` に `InstallFeedback.off.kt`。manifest と cleartext は `android.md` と同じ（`src/debug/`） |
| iOS | Xcode の Debug | `sharedUI/build.gradle.kts` のスイッチで、`iosMain` に足すソースと依存を切り替える（下） |
| Desktop | `-Pfeedback=true` | `desktopApp` の `sourceSets.main` に足すソースと依存を同じように切り替える。配布物（`packageDmg` など）はスイッチ無しで作る |
| Web | `-Pfeedback=true` | `webApp` の `wasmJsMain` で同じように切り替える。公開用の `wasmJsBrowserDistribution` はスイッチ無しで作る |

```kotlin
// sharedUI/build.gradle.kts（iOS）。-Pfeedback が優先、無ければ Xcode の CONFIGURATION が Debug のときだけ on。既定は off（忘れても本番に入らない向き）
val feedbackOn = providers.gradleProperty("feedback").orNull?.toBoolean()
    ?: (providers.environmentVariable("CONFIGURATION").orNull == "Debug")
kotlin.sourceSets.iosMain {
    kotlin.srcDir(if (feedbackOn) "src/iosFeedbackOn/kotlin" else "src/iosFeedbackOff/kotlin")
    if (feedbackOn) dependencies { implementation(project(":feedback")) }
}
// desktopApp: sourceSets.main { kotlin.srcDir(…) } と dependencies { if (feedbackOn) implementation(project(":feedback")) }（既定 false）
// webApp:     kotlin.sourceSets.wasmJsMain { kotlin.srcDir(…); dependencies { if (feedbackOn) implementation(project(":feedback")) } }（既定 false）
```

- Xcode の Run Script（`./gradlew :sharedUI:embedAndSignAppleFrameworkForXcode`）には `CONFIGURATION` が渡るので、Debug / Release で framework の中身が変わる。
- AGP 9 の KMP ライブラリ（`com.android.kotlin.multiplatform.library`）は build type を持たないので、Android の切り替えはアプリのモジュール（`androidApp`）でする。
- 確かめる:
  - Android: `android.md` と同じ `scripts/check-android-release.sh`。
  - iOS: `xcodebuild -configuration Release -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build` のあと `scripts/check-ios-release.sh <…/Release-iphonesimulator/<App>.app> FeedbackRoot platformFeedbackConfig`。
  - Desktop: `createDistributable` の `Contents/app/*.jar` を展開して `scripts/check-no-feedback-in-prod.sh`（jar は圧縮されているので、展開せずに grep すると見逃す）。
  - Web: `wasmJsBrowserDistribution` の出力（`build/dist/wasmJs/productionExecutable`）に `check-no-feedback-in-prod.sh`。

## 差し込み口

`android.md` の表と同じ（`compose/` を共有している）。違うところだけ書く。

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 右下の「FB」。既存の debug 画面があれば `FeedbackConfig.showEntryButton = false` にして、`setupFeedback` に渡す `FeedbackRoot` の `controller` を debug 画面から呼ぶ |
| `name` | `sharedUI` の `Modifier.fbTarget`（`FeedbackHooks`）。入れないビルドでは何もしない |
| `block` | 全面の `pointerInput`。戻るは Android だけ `backHandler` に `BackHandler` を渡す |
| `capture` | `GraphicsLayer.toImageBitmap()` → Android は software の bitmap に写してから描く、iOS・Desktop・Web は Skia（`asSkiaBitmap()` → `Image.encodeToData(PNG)`） |
| `transport` | `httpTransport(endpoint, token)`: Android・Desktop は `HttpURLConnection`、iOS は `NSURLSession`、Web は `fetch`。送信先は Android エミュレータ `10.0.2.2`、iOS シミュレータ・Desktop・Web は `127.0.0.1` |

## この環境の注意

- **Kotlin/Native は文字列を UTF-16LE で持つ**。iOS の framework に入った URL・`/__feedback` は、ふつうの grep では見えない。`check-no-feedback-in-prod.sh` は UTF-16LE でも探す（perl が要る）。
- `FeedbackHooks` は Release にも残る（中身は no-op）。`fbTarget` に渡した部品の名前の文字列は、iOS の Release の実行ファイルに UTF-16LE で残っていた（Android の release は R8 で消えていた）。名前に秘密を書かない。
- Web: Kotlin の `RequestInit(method = …, body = …)` を使うと、渡していない項目も `null` で入り、`fetch` が「'cache' に null は不正」で失敗した。ひな形は `js("({ … })")` で必要な項目だけの object を作る。受け口は Origin が localhost 系なら CORS の preflight に答える。
- Web は canvas に描くので、DOM のセレクタでは操作できない。自動操作は座標クリック（Playwright の `page.mouse.click`）と `page.keyboard.type`（英数字）で行った。
- Desktop の UI テストでは、コメント欄が 2 つ（アプリの欄とシートの欄）あるので `onAllNodes(hasSetTextAction()).onLast()` で選んだ。
- Maestro は Android エミュレータと iOS シミュレータが両方起動していると Android を選ぶことがある。`maestro --device <udid>` で指定する。
