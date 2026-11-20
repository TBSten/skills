# mobile / kmp（Kotlin Multiplatform・Compose Multiplatform）

検証状況: macOS 26.2・JDK 17・Kotlin 2.4.10・Compose Multiplatform 1.12.0-rc01（material3 1.12.0-alpha03）・AGP 9.1.1・Gradle 9.7.0。CMP ウィザードと同じ構成（androidApp・sharedUI・desktopApp・webApp・iosApp）で 2 回確かめた: (1) 画面 1 枚の最小アプリ、(2) この docs とひな形だけを読んだ別の担当が、新しいアプリ（画面の切り替え・30 行以上の LazyColumn・Material3 の Button / TextField / Scaffold・AlertDialog）に導入した。
- **Android**（エミュレータ API 36。`adb`）・**iOS**（iOS 26.2 シミュレータ。Maestro）・**Web**（wasmJs を Chrome で。Playwright の座標クリック）・**Desktop**（JVM。`:desktopApp:run -Pfeedback=true` で起動できることと、Compose の UI テスト `runDesktopComposeUiTest` で操作）の 4 つとも: 入る → 部品をタップ → 親を選ぶ → コメント → 送る → schema 1 の記録 + `window.png`・`element.png`。モード中は下の部品へのタップ・一覧のスクロールが届かない。スクロールした後の行の矩形・名前が合う。受け口が止まっているときは理由が出てモードが続く。
- 本番から外す: Android の release APK（R8 あり）・iOS の Release の `.app`・Desktop の `createDistributable`・Web の `wasmJsBrowserDistribution` のどれにも、フィードバックのコード・URL が残らない（スイッチを入れたビルドでは見つかる）。
- 選べないもの: `AlertDialog`・`Dialog`・`Popup` など別ウィンドウの中（4 つとも。開いているときに「FB」を押すと、ダイアログが閉じるだけ）。`android.md` の「選べないもの」と同じ。

未検証: iOS 実機・Desktop のウィンドウを人の操作で触ること・JS（Kotlin/JS）ターゲット。

ひな形（`assets/templates/adapters/mobile/` の下。並びは導入先と同じ）:
- `kmp/feedback/` → `:feedback` モジュールそのもの: `build.gradle.kts`（プラグイン・ターゲット・依存）と `src/<sourceSet>/kotlin/PlatformFeedback*.kt`（commonMain の `expect` と `platformFeedbackConfig`、各ターゲットの HTTP の送信・PNG・env）。**`compose/` の 5 枚（Android 単体と同じもの）も `src/commonMain/kotlin/` に置く**
- `kmp/shared/FeedbackHooks.kt` → 画面のある共有モジュール（`sharedUI` など）の `commonMain`。差し込み口だけで、フィードバックのコード・URL は持たない
- `kmp/setup/FeedbackSetup.kt`・`InstallFeedback.on.kt` → フィードバックを入れるビルドのソース、`InstallFeedback.off.kt` → 入れないビルドのソース（置き場はターゲットごと。下の表）

依存は Compose の runtime・ui・foundation と coroutines だけ（Ktor・kotlinx.serialization は足さない）。先に `common.md` を読む。

## 形

```
sharedUI（commonMain）: 画面。Modifier.fbTarget("…") と FeedbackWrap { … } だけを使う（FeedbackHooks 経由）
:feedback（KMP モジュール）: compose/ の本体 + 各ターゲットの送信・PNG
入れるビルドだけ: installFeedback() が FeedbackHooks に :feedback の中身を差し込む
```

- `settings.gradle.kts` に `include(":feedback")`。
- `App()` の一番外を `FeedbackWrap { … }` で包み、部品に `Modifier.fbTarget("画面/部品")` を付ける（`FeedbackHooks.kt` の package の方を import する）。
- 起動時に `installFeedback()` を 1 回呼ぶ（Android は `setContent` の前、iOS は `MainViewController()` の中、Desktop は `application` の前、Web は `ComposeViewport` の前）。

### package の置き換え

ひな形の package は例。変えてよい。変えるなら次をそろえる:

| ファイル | package | 合わせる import |
|---|---|---|
| `compose/*.kt`・`kmp/feedback/src/**/PlatformFeedback*.kt` | `com.example.feedback` → 例 `<アプリ>.feedback` | — |
| `kmp/shared/FeedbackHooks.kt`・`kmp/setup/FeedbackSetup.kt` | `com.example.app.debughooks` → 例 `<アプリ>.debughooks`（2 枚は同じ package） | `FeedbackSetup.kt` の `com.example.feedback.*`、画面の `fbTarget` / `FeedbackWrap` |
| `kmp/setup/InstallFeedback.{on,off}.kt` | iOS・Desktop・Web は**ルート package のまま**（呼ぶ側の `main.kt` がルート package）。**Android は呼ぶ Activity と同じ package を書く**（Kotlin は package 付きのファイルからルート package の宣言を参照できない） | `setupFeedback`・`platformFeedbackConfig` |

## 本番から外す（ターゲットごとのスイッチ）

| ターゲット | 入れるとき | 外し方 | on / off のソースの置き場 |
|---|---|---|---|
| Android | debug ビルド | `androidApp` の `debugImplementation(project(":feedback"))`。manifest と cleartext は `android.md` と同じ（`src/debug/`） | `androidApp/src/debug/kotlin/<package>/`（on + `FeedbackSetup.kt`）・`src/release/kotlin/<package>/`（off） |
| iOS | Xcode の Debug | `sharedUI/build.gradle.kts` のスイッチで `iosMain` に足すソースと依存を切り替える | `sharedUI/src/iosFeedbackOn/kotlin/`・`src/iosFeedbackOff/kotlin/` |
| Desktop | `-Pfeedback=true` | `desktopApp/build.gradle.kts` のスイッチ。配布物（`packageDmg`・`createDistributable`）はスイッチ無しで作る | `desktopApp/src/feedbackOn/kotlin/`・`src/feedbackOff/kotlin/` |
| Web | `-Pfeedback=true` | `webApp/build.gradle.kts` のスイッチ。公開用の `wasmJsBrowserDistribution` はスイッチ無しで作る | `webApp/src/wasmFeedbackOn/kotlin/`・`src/wasmFeedbackOff/kotlin/` |

```kotlin
// sharedUI/build.gradle.kts（iOS）。-Pfeedback が優先、無ければ Xcode の CONFIGURATION が Debug のときだけ on。既定は off（忘れても本番に入らない向き）
val feedbackOn = providers.gradleProperty("feedback").orNull?.toBoolean()
    ?: (providers.environmentVariable("CONFIGURATION").orNull == "Debug")
kotlin.sourceSets.iosMain {
    kotlin.srcDir(if (feedbackOn) "src/iosFeedbackOn/kotlin" else "src/iosFeedbackOff/kotlin")
    if (feedbackOn) dependencies { implementation(project(":feedback")) }
}

// desktopApp/build.gradle.kts（kotlin("jvm") のモジュール）。既定は off
val feedbackOn = providers.gradleProperty("feedback").orNull?.toBoolean() ?: false
sourceSets.main { kotlin.srcDir(if (feedbackOn) "src/feedbackOn/kotlin" else "src/feedbackOff/kotlin") }
dependencies { if (feedbackOn) implementation(project(":feedback")) }

// webApp/build.gradle.kts（kotlin { wasmJs { browser(); binaries.executable() } } の中）。既定は off
val feedbackOn = providers.gradleProperty("feedback").orNull?.toBoolean() ?: false
sourceSets.wasmJsMain {
    kotlin.srcDir(if (feedbackOn) "src/wasmFeedbackOn/kotlin" else "src/wasmFeedbackOff/kotlin")
    dependencies { if (feedbackOn) implementation(project(":feedback")) }
}
```

- Xcode の Run Script（`./gradlew :sharedUI:embedAndSignAppleFrameworkForXcode`）には `CONFIGURATION` が渡るので、Debug / Release で framework の中身が変わる。
- AGP 9 の KMP ライブラリ（`com.android.kotlin.multiplatform.library`）は build type を持たないので、Android の切り替えはアプリのモジュール（`androidApp`）でする。
- 送信先: on のソースはフィードバックを入れるビルドにしか入らないので、そこに開発機の URL を書いてよい（iOS シミュレータ・Web は `http://127.0.0.1:7373/__feedback`）。Android は `android.md` と同じ debug の `buildConfigField`（`BuildConfig.FEEDBACK_ENDPOINT`）。Desktop は `System.getenv("FEEDBACK_ENDPOINT") ?: ""` か直書き。
- 動かす: Android は `./gradlew :androidApp:installDebug -Pfeedback.endpoint=http://10.0.2.2:7373/__feedback`、Desktop は `FEEDBACK_ENDPOINT=http://127.0.0.1:7373/__feedback ./gradlew :desktopApp:run -Pfeedback=true`、Web は `./gradlew :webApp:wasmJsBrowserDevelopmentRun -Pfeedback=true`（`http://localhost:8080/`）、iOS は Xcode の Debug。
- 確かめる（本番の成果物）:
  - Android: `android.md` と同じ `scripts/check-android-release.sh`。
  - iOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' ARCHS=arm64 ONLY_ACTIVE_ARCH=YES -derivedDataPath build CODE_SIGNING_ALLOWED=NO build` のあと `scripts/check-ios-release.sh --kmp build/Build/Products/Release-iphonesimulator/<App>.app FeedbackRoot platformFeedbackConfig`。`--kmp` は `FeedbackHooks` の no-op の `fbTarget` を見ない（Release にも残る設計のため）。`ARCHS` を付けないと x86_64 で失敗し、空の `.app` が残る（検査は空の成果物をエラーにする）。
  - Desktop: `./gradlew :desktopApp:createDistributable` のあと `scripts/check-no-feedback-in-prod.sh desktopApp/build/compose/binaries/main/app/<App>.app/Contents/app/*.jar`（jar を渡せば展開して見る）。
  - Web: `./gradlew :webApp:wasmJsBrowserDistribution` のあと `scripts/check-no-feedback-in-prod.sh webApp/build/dist/wasmJs/productionExecutable`。

## 差し込み口

`android.md` の表と同じ（`compose/` を共有している）。違うところだけ書く。

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 右下の「FB」。既存の debug 画面があれば `platformFeedbackConfig(…, showEntryButton = false)` にし、`setupFeedback(config, controller = fb)` に渡した `fb`（`FeedbackController`）の `enter()` を debug 画面から呼ぶ |
| `name` | `sharedUI` の `Modifier.fbTarget`（`FeedbackHooks`）。入れないビルドでは何もしない |
| `block` | 全面の `pointerInput` で全部のポインタを consume する。戻るは Android だけ `backHandler` に `BackHandler` を渡す |
| `capture` | `GraphicsLayer.toImageBitmap()` → Android は software の bitmap に写してから描く、iOS・Desktop・Web は Skia（`asSkiaBitmap()` → `Image.encodeToData(PNG)`） |
| `transport` | `httpTransport(endpoint, token)`: Android・Desktop は `HttpURLConnection`、iOS は `NSURLSession`、Web は `fetch`（届かないときは「受け口に届きません（URL）」）。送信先は Android エミュレータ `10.0.2.2`、iOS シミュレータ・Desktop・Web は `127.0.0.1` |

## Desktop の UI テスト（ウィンドウを触らずに確かめる）

```kotlin
// desktopApp/build.gradle.kts
dependencies { testImplementation(kotlin("test")); testImplementation(libs.compose.ui.test) /* ui-test。compose.desktop.uiTestJUnit4 でもよい */ }
tasks.withType<Test>().configureEach { environment("FEEDBACK_ENDPOINT", "http://127.0.0.1:7373/__feedback") }
```

テストは `desktopApp/src/test/kotlin/` に置き、`./gradlew :desktopApp:test -Pfeedback=true` で動かす（off だと `installFeedback()` が no-op）。`runDesktopComposeUiTest { installFeedback(); setContent { App() }; onNodeWithText("FB").performClick(); … }`。コメント欄はアプリの欄とシートの欄の 2 つになるので、`onAllNodes(hasSetTextAction()).onLast()` で選ぶ。

## この環境の注意

- **Kotlin/Native は文字列を UTF-16LE で持つ**。iOS の framework に入った URL・`/__feedback` は、ふつうの grep では見えない。`check-no-feedback-in-prod.sh` は UTF-16LE でも探す（perl が要る）。
- `FeedbackHooks` は Release にも残る（中身は no-op）。`fbTarget` に渡した部品の名前の文字列は、iOS の Release の実行ファイルに UTF-16LE で残っていた（Android の release は R8 で消えていた）。名前に秘密を書かない。iOS の検査は `--kmp` で `fbTarget` を外して見る。
- Web: Kotlin の `RequestInit(method = …, body = …)` を使うと、渡していない項目も `null` で入り、`fetch` が「'cache' に null は不正」で失敗した。ひな形は `js("({ … })")` で必要な項目だけの object を作る。受け口は Origin が localhost 系なら CORS の preflight に答える。
- Web は canvas に描くので、DOM のセレクタでは操作できない。自動操作は座標クリック（Playwright の `page.mouse.click`）と `page.keyboard.type`（英数字）で行った。
- Maestro は Android エミュレータと iOS シミュレータが両方起動していると Android を選ぶことがある。`maestro --device <udid>` で指定する。
