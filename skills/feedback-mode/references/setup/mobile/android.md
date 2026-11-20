# mobile / android（Kotlin・Jetpack Compose）

検証状況: macOS 26.2・JDK 17・AGP 9.1.1・Kotlin 2.4.10・Compose（foundation 1.10.0・activity-compose 1.12.0）・Android エミュレータ（API 36.1、arm64）で、画面 1 枚の最小アプリを `adb shell input` と `uiautomator dump` で操作して確かめた。確かめたこと: 「FB」で入る → 部品をタップ → 「親を選ぶ」で広がる → コメント → 送る → 受け口（`core/feedback-dev-server.mjs`）の Inbox に schema 1 の記録ができ、`window.png`（赤枠）と `element.png`（周り 24dp）が付く → モードが終わる。モード中に下のボタンをタップしてもカウンタが増えない。戻るキーはモードを出るだけでアプリは閉じない。受け口に届かないと理由が出てモードが続く。キーボードが出てもシートが隠れない。`adb reverse` + `localhost` でも送れた（エミュレータで。USB の実機は未確認）。release の APK（R8 あり）に、コード・URL・cleartext の許可・debug だけの権限が残らない（`scripts/check-android-release.sh`）。
未検証: View（XML）の画面、`Dialog` / `ModalBottomSheet`（別ウィンドウ）の中、スクロールする一覧、R8 を切った release、R8（フォーカス）を戻すこと（外すだけ）、実際のログ基盤をつなぐこと。

ひな形:
- `assets/templates/adapters/mobile/compose/`（共通。プラットフォームの API を使わない Compose のコード 5 枚。KMP の commonMain にもそのまま使う）: `FeedbackRoot.kt`（入口・オーバーレイ・シート・送信）、`FbTarget.kt`（`Modifier.fbTarget` と registry）、`FeedbackShots.kt`（赤枠と切り抜き）、`FeedbackModels.kt`（`FeedbackConfig`・`FeedbackTransport`・返事の読み取り）、`FeedbackJson.kt`
- `assets/templates/adapters/mobile/android/debug/`（`src/debug/` と同じ並び）: `kotlin/FeedbackHost.kt`（HTTP の transport・PNG・env を埋めた入口）、`AndroidManifest.xml`、`res/xml/feedback_network_security_config.xml`
- `assets/templates/adapters/mobile/android/release/kotlin/FeedbackHost.kt`: 同じ形の no-op

依存は Compose の foundation と activity-compose だけ（material3・OkHttp・kotlinx.serialization は足さない）。KMP（Compose Multiplatform）なら `kmp.md`。先に `common.md` を読む。

## 置き方

1. `compose/*.kt` と `android/debug/kotlin/FeedbackHost.kt` を `app/src/debug/kotlin/<package>/feedback/` に、`android/release/kotlin/FeedbackHost.kt` を `app/src/release/kotlin/<package>/feedback/` に置く。`package` と `FeedbackHost.kt` の `import …BuildConfig` を導入先に合わせる。
2. `android/debug/AndroidManifest.xml` を `app/src/debug/`、`feedback_network_security_config.xml` を `app/src/debug/res/xml/` に置く。
3. `app/build.gradle.kts` の debug にだけ送信先を渡す（既定値を持たない）。開発機の `~/.gradle/gradle.properties` に `feedback.endpoint=http://10.0.2.2:7373/__feedback`（コミットしない）。

   ```kotlin
   android {
       buildFeatures { buildConfig = true }
       buildTypes { debug { buildConfigField("String", "FEEDBACK_ENDPOINT", "\"${providers.gradleProperty("feedback.endpoint").orNull ?: ""}\"") } }
   }
   ```

4. main から呼ぶのは 2 つだけ: `setContent { FeedbackHost(appName = "…", getScreen = { … }) { App() } }` と、部品の `Modifier.fbTarget("画面/部品")`。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 右下の「FB」（`FeedbackConfig.showEntryButton`）。既存の debug drawer・debug 画面があれば `showEntryButton = false` にし、その項目から `FeedbackController.enter()` を呼ぶ（`FeedbackRoot(controller = …)` で渡す） |
| `name` | `Modifier.fbTarget("ホーム/ボタン列/保存")`。道筋はそのまま書く（入れ子から組み立てない）。`testTag` は付けない（既存の UI テストを壊さないため） |
| `pick` | タップ位置を含む `fbTarget` のうち最も小さい矩形（`boundsInWindow`）。「親を選ぶ」で次に大きいものへ。どれにも入らなければ「画面全体」（`kind: page`） |
| `block` | `FeedbackRoot` の中で、アプリの上に全面の `pointerInput { detectTapGestures }` を重ねる。重なった兄弟の下側にはタップが渡らないので、アプリに届かない。戻るは `BackHandler` でモードを出るだけ |
| `collect` | `FeedbackHost` の `getScreen`・`getView`（モデルから）。`env`（OS・端末・版）・`viewport`（dp と density）・`rect`（dp）は自動 |
| `logs` | `getRecentLogs = { … }`（Timber の Tree で溜めたバッファなど。`List<FeedbackLog>`、古い順）。作るのは別タスク（`../setup.md` §4） |
| `capture` | アプリの中身を `GraphicsLayer` に記録しておき、送るときに `toImageBitmap()`。モードの UI は記録の外なので写らない。software の bitmap に写してから赤枠と切り抜きを描く（`prepareShot`）。倍率は `min(density, 2)`。失敗は `shotError` |
| `transport` | `httpTransport(BuildConfig.FEEDBACK_ENDPOINT)`（`HttpURLConnection` で 1 件ごとに POST）。鍵は第 2 引数（`x-feedback-token`） |
| `pointer` | `FeedbackHost.kt` の `pointer` |

## 本番から外す

- フィードバックのコードは全部 `src/debug/`。main は `FeedbackHost` と `fbTarget` だけを呼び、release では `src/release/` の no-op（`fbTarget` は `inline` で `this` を返す）が使われる。
- INTERNET（main に無い場合）・`networkSecurityConfig`（cleartext は `10.0.2.2`・`localhost`・`127.0.0.1` だけ）は `src/debug/AndroidManifest.xml` だけ。main に `networkSecurityConfig` があるなら、debug 版を `src/debug/res/xml/` に同じ名前で置いて上書きする。
- 送信先は debug の `buildConfigField` だけ。
- 確かめる: `./gradlew :app:assembleRelease :app:assembleDebug && bash "${CLAUDE_SKILL_DIR}/scripts/check-android-release.sh" app/build/outputs/apk/release/app-release.apk app/build/outputs/apk/debug/app-debug.apk`。dex・resources・manifest を grep し、release の manifest に cleartext の許可が無いこと、debug だけの権限が release に無いことを見る。検証では R8 ありの release で、部品の名前の文字列（`"ホーム/ボタン列"`）も消えていた。debug の APK に掛けると見つかる（検査が効いていることの確認）。

## この環境の注意

- `GraphicsLayer.toImageBitmap()` の画像は hardware bitmap で、そのまま `Canvas(ImageBitmap)` に描くと「Software rendering doesn't support hardware bitmaps」で失敗した。`prepareShot` で `Bitmap.copy(ARGB_8888)` してから描く。
- シートは `imePadding()` でキーボードの上に出る（`windowSoftInputMode="adjustResize"` で確認）。
- 上の帯（「部品をタップ」）は画面の上端の部品に重なる。重なった部品も帯の外側をタップすれば選べる。
- `Dialog`・`Popup`・`ModalBottomSheet` は別ウィンドウなので `FeedbackRoot` の外にある（未検証。中も選びたいなら中身も `FeedbackRoot` で包む）。
- 自動操作: `uiautomator dump` で Compose の `BasicText` の文字と bounds が取れる。日本語の `adb shell input text` は通らないので、確認のコメントは英数字にした。
- 検証では既存の AVD の空き容量が足りず「not enough space」でインストールに失敗した。既存の AVD は消さず、`disk.dataPartition.size` を大きくした使い捨ての AVD を作った。
