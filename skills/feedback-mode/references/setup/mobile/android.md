# mobile / android（Kotlin・Jetpack Compose）

検証状況: macOS 26.2・JDK 17・AGP 9.1.1・Kotlin 2.4.10・Compose（ui / foundation 1.10.0・material3 1.4.0・activity-compose 1.12.0・navigation-compose 2.9.8）・Android エミュレータ（API 36.1、arm64）。`adb shell input` と `uiautomator dump` で操作し、2 回確かめた: (1) 画面 1 枚の最小アプリ、(2) この docs とひな形だけを読んだ別の担当が、新しいアプリ（Navigation で 3 画面・40 行の LazyColumn・AlertDialog・ModalBottomSheet・DropdownMenu・OutlinedTextField・Material3・edge-to-edge）に導入した。
- 通った: 「FB」で入る → 部品をタップ → 「親を選ぶ」→ コメント → 送る → 受け口の Inbox に schema 1 の記録と `window.png`（赤枠）・`element.png`（周り 24dp）。モード中はタップ・一覧のスクロール・画面遷移がアプリに届かない。戻るキーはモードを出るだけ。スクロールした後の行の矩形・名前、画面遷移の後（前の画面の部品は残らない）、受け口が止まっているとき（理由が出てモードが続く）、キーボードとジェスチャナビの帯に下のシートが重ならない、回転・テーマの切り替えでモードと書きかけが残る、`adb reverse` + `localhost`（エミュレータで）。release の APK（R8 あり）にコード・URL・cleartext の許可・debug だけの権限が残らない（`scripts/check-android-release.sh`。debug の APK では見つかる）。
- 選べないもの: `Dialog`・`AlertDialog`・`Popup`・`DropdownMenu`・`ModalBottomSheet`（別ウィンドウ）の中。開いている間は「FB」を押しても外側のタップとして閉じるだけで、モードに入れない。中身を `FeedbackHost` で包むと、ウィンドウの大きさ・座標・スクショが崩れたので勧めない。
- 未実装: 出たときにフォーカスを戻す（R8 のうち「戻す」。Compose に今のフォーカスを覚えて戻す API が無い。入るときに外し、文字は残る）。
- 未検証: View（XML）の画面、R8 を切った release、USB の実機、実際のログ基盤をつなぐこと。

ひな形:
- `assets/templates/adapters/mobile/compose/`（共通。プラットフォームの API を使わない Compose のコード 5 枚。KMP の commonMain にもそのまま使う）: `FeedbackRoot.kt`（入口・オーバーレイ・シート・送信）、`FbTarget.kt`（`Modifier.fbTarget` と registry）、`FeedbackShots.kt`（赤枠と切り抜き）、`FeedbackModels.kt`（`FeedbackConfig`・`FeedbackTransport`・返事の読み取り）、`FeedbackJson.kt`
- `assets/templates/adapters/mobile/android/debug/`（`src/debug/` と同じ並び）: `kotlin/FeedbackHost.kt`（HTTP の transport・PNG・env を埋めた入口）、`AndroidManifest.xml`、`res/xml/feedback_network_security_config.xml`
- `assets/templates/adapters/mobile/android/release/kotlin/FeedbackHost.kt`: 同じ形の no-op（`FeedbackController` も no-op）

依存は Compose の foundation と activity-compose だけ（material3・OkHttp・kotlinx.serialization は足さない）。KMP（Compose Multiplatform）なら `kmp.md`。先に `common.md` を読む。

## 置き方

1. `compose/*.kt` と `android/debug/kotlin/FeedbackHost.kt` を `app/src/debug/kotlin/<namespace>/feedback/` に、`android/release/kotlin/FeedbackHost.kt` を `app/src/release/kotlin/<namespace>/feedback/` に置く。どのファイルも `package com.example.feedback` を `<namespace>.feedback` に、`FeedbackHost.kt` の `import com.example.fbdemo.BuildConfig` を `<namespace>.BuildConfig` に変える（BuildConfig の package は `namespace` で決まる）。
2. `android/debug/AndroidManifest.xml` を `app/src/debug/`、`feedback_network_security_config.xml` を `app/src/debug/res/xml/` に置く。
3. `app/build.gradle.kts` の debug にだけ送信先と鍵を渡す（既定値を持たない。`FeedbackHost.kt` が両方を読むので、2 行とも要る）。

   ```kotlin
   android {
       buildFeatures { buildConfig = true }
       buildTypes {
           debug {
               buildConfigField("String", "FEEDBACK_ENDPOINT", "\"${providers.gradleProperty("feedback.endpoint").orNull ?: ""}\"")
               buildConfigField("String", "FEEDBACK_TOKEN", "\"${providers.gradleProperty("feedback.token").orNull ?: ""}\"") // LAN に出すときだけ
           }
       }
   }
   ```

   値の渡し方: `./gradlew :app:installDebug -Pfeedback.endpoint=http://10.0.2.2:7373/__feedback`、または開発機の `~/.gradle/gradle.properties`（全プロジェクトに効くので、ほかのアプリと受け口のポートが被らないようにする）。どちらもコミットしない。
4. main から呼ぶのは 2 つだけ: `setContent { FeedbackHost(appName = "…", getScreen = { … }) { App() } }` と、部品の `Modifier.fbTarget("画面/部品")`。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 右下の「FB」。既存の debug drawer・debug 画面があれば、`val fb = remember { FeedbackController() }` を作って `FeedbackHost(…, showEntryButton = false, controller = fb)` に渡し、その項目から `fb.enter()` を呼ぶ（release でも `FeedbackController` は no-op として在るので、同じコードがコンパイルできる） |
| `name` | `Modifier.fbTarget("ホーム/ボタン列/保存")`。道筋はそのまま書く（入れ子から組み立てない）。一覧の行は内容で名付ける: `items(fruits) { f -> ListItem(…, Modifier.fbTarget("果物一覧/行/${f.name}")) }`。`testTag` は付けない（既存の UI テストを壊さないため） |
| `pick` | タップ位置を含む `fbTarget` のうち最も小さい矩形（`boundsInWindow`）。「親を選ぶ」で次に大きいものへ。どれにも入らなければ「画面全体」（`kind: page`）。Material3 の `Button` などは最小のタッチ領域（48dp）を含むので、赤枠が見た目より少し大きい |
| `block` | `FeedbackRoot` の中で、アプリの上に全面の `pointerInput` を重ね、全部のポインタを consume する。下のアプリにも、祖先の drag（`BottomSheetScaffold` など同じウィンドウの中の drag）にも届かない。戻るは `BackHandler` でモードを出るだけ |
| `collect` | `FeedbackHost` の `getScreen`・`getView`（モデルから）。Navigation Compose なら `NavController` を `FeedbackHost` の外で作り、`getScreen = { nav.currentDestination?.route ?: "" }`。`env`（OS・端末・版）・`viewport`（dp と density）・`rect`（dp）は自動 |
| `logs` | `getRecentLogs = { … }`（Timber の Tree で溜めたバッファなど。`List<FeedbackLog>`、古い順）。作るのは別タスク（`../setup.md` §4） |
| `capture` | アプリの中身を `GraphicsLayer` に記録しておき、送るときに `toImageBitmap()`。モードの UI は記録の外なので写らない。software の bitmap に写してから赤枠と切り抜きを描く（`prepareShot`）。倍率は `min(density, 2)`。失敗は `shotError` |
| `transport` | `httpTransport(BuildConfig.FEEDBACK_ENDPOINT, BuildConfig.FEEDBACK_TOKEN)`（`HttpURLConnection` で 1 件ごとに POST。鍵は空なら送らない） |
| `pointer` | `FeedbackHost.kt` の `pointer` |

## 本番から外す

- フィードバックのコードは全部 `src/debug/`。main は `FeedbackHost`・`fbTarget`（と、debug menu から入るなら `FeedbackController`）だけを呼び、release では `src/release/` の no-op（`fbTarget` は `inline` で `this` を返す）が使われる。
- INTERNET（main に無い場合）・`networkSecurityConfig`（cleartext は `10.0.2.2`・`localhost`・`127.0.0.1` だけ）は `src/debug/AndroidManifest.xml` だけ。main に `networkSecurityConfig` があるなら、debug 版を `src/debug/res/xml/` に同じ名前で置いて上書きする。
- 送信先と鍵は debug の `buildConfigField` だけ。
- 確かめる: `./gradlew :app:assembleRelease :app:assembleDebug && bash "${CLAUDE_SKILL_DIR}/scripts/check-android-release.sh" app/build/outputs/apk/release/app-release.apk app/build/outputs/apk/debug/app-debug.apk`。dex・resources・manifest を grep し、release の manifest に cleartext の許可が無いこと、debug だけの権限が release に無いことを見る。検証では R8 ありの release で、部品の名前の文字列（`"ホーム/ボタン列"`）も消えていた。debug の APK に掛けると見つかる（検査が効いていることの確認）。

## この環境の注意

- `GraphicsLayer.toImageBitmap()` の画像は hardware bitmap で、そのまま `Canvas(ImageBitmap)` に描くと「Software rendering doesn't support hardware bitmaps」で失敗した。`prepareShot` で `Bitmap.copy(ARGB_8888)` してから描く。
- 下のシートは `WindowInsets.navigationBars.union(WindowInsets.ime)` の分だけ上げる（edge-to-edge のジェスチャナビの帯と、`windowSoftInputMode="adjustResize"` のキーボードに重ねない）。
- 上の帯（「部品をタップ」）は画面の上端の部品に重なる。重なった部品も帯の外側をタップすれば選べる。「FB」はアプリの右下の FAB などに重なる。重なるなら debug menu から入る形にする。
- 別ウィンドウ（`Dialog`・`Popup`・`DropdownMenu`・`ModalBottomSheet`）の中は選べない（上の「選べないもの」）。閉じてから、その画面の部品か画面全体に書く。
- 回転・ダーク / ライトの切り替えでは、モード（`FeedbackController` の Saver）と書きかけのコメントが残る。選んでいた部品は外れるので、もう一度タップする。
- 受け口を止めるときは `lsof -ti tcp:<ポート> -sTCP:LISTEN` で受け口だけの PID を取る（`-sTCP:LISTEN` を付けないと、エミュレータの `netsimd` の接続も出る）。
- 自動操作: `uiautomator dump` で Compose の文字と bounds が取れる。日本語の `adb shell input text` は通らないので、確認のコメントは英数字にした。
- 検証では既存の AVD の空き容量が足りず「not enough space」でインストールに失敗した。既存の AVD は消さず、`disk.dataPartition.size` を大きくした使い捨ての AVD を作った。
