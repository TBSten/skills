# プラットフォーム別の実装指針

凡例・用語は `references/concepts.md`（【実績】【追加】【未検証】。置き場・Sink・Transport・受け口の意味もそこ）。コードは `assets/templates/`。
**検証状況**: コードを実際に動かして確かめたのは、`assets/templates/core/`（node:test 17 件。Sink の契約テストをメモリ・ファイル・HTTP の 3 実装に掛けた）と、`assets/templates/web/feedback-mode.js` + `feedback-dev-server.mjs` + `file-sink.mjs`（ヘッドレス Electron で「ホバー → クリック → 吹き出し → 送信 → 置き場に記録ができる・下の要素にクリックが届かない・Esc で出る・`enabled` を渡さないと何もしない」を確認）と、`scripts/check-no-feedback-in-prod.sh`（偽の成果物で検出・非検出を確認）だけ。ほかのテンプレート（Electron の配線・React Native・Flutter・下のネイティブ断片）は**書いただけで未検証**。
追記: `assets/templates/core/` は node:test 32 件（schema 1・writeDoc・採番の競合を足した）、`core-ts/` は vitest 28 件、`web/astro/` は Astro 7 の最小プロジェクトで「dev で toolbar から要素を選んで保存 → 置き場に schema 1 の記録・リンクは動かない・Esc で出る・幅 390px のタッチでシート（16px・44px）」「build の dist で検査が通り、統合を本番にも登録すると検査が落ちる」を確認した（§2.1）。

## 0. 共通方針

| 論点 | 方針 |
|---|---|
| 入る手段 | Desktop: メニュー + ホットキー【元: ⌥⌘F】/ Web: ホットキー（Alt+Shift+F）+ 小さな FAB【追加】/ モバイル: 画面の隅の FAB・シェイク・3 本指長押しなど【追加】【未検証】 |
| 部品を選ぶ | DOM があれば `elementsFromPoint`【実績】。無ければ「名前を付けた部品を registry に登録し、タップ位置を含む最小の矩形を選ぶ」【追加】 |
| 部品の名前 | `data-fb`【実績】/ `testID`・`accessibilityIdentifier`・`Semantics(label)`・`Key` など。**主な部品から付け始める**（参照実装も段階的に増やした）【追加】 |
| 操作を止める | 全面オーバーレイでタップを受ける【実績】。ジェスチャ・戻るボタン・キーボードショートカットも止めるかを確認する【追加】 |
| スクリーンショット | モードの UI（吹き出し・帯・強調）を**隠してから**、選んだ部品を枠で囲んで撮る【実績】。撮れなくても送る（理由を書く）【実績】 |
| 送信先（Transport → 受け口 → Sink） | 画面は Transport（IPC / HTTP / 共有シート…）で受け口に送り、受け口が Sink（ファイル・Issue・クラウド…）に書く【追加】。Desktop は main が受け口、それ以外は dev サーバ。モバイル実機は LAN 越しになるので、`adb reverse`・USB・同一 LAN の制限と `FEEDBACK_TOKEN` を使う【未検証】。Sink の差し替えは受け口の側だけで、画面側は変えない |
| 秘密情報 | 受け口で組み立てた文の全体に `redact` を 1 回【実績】。パスワード欄の値は集めない【追加】。ログに入れる前にも伏せる【実績】 |
| 本番に出すとき | 参照実装は個人用ツールで無効化の概念が無い（パッケージ版にも入っている）【実績】。汎用版は **debug ビルドだけに配線し、本番に入れない**を前提にする【追加】。具体策は §0.1。ユーザ向けに意図して出すなら、同意・個人情報・保存先を別途設計する（`references/build/build.md` の未確定事項）【追加】【未検証】 |
| 下書き・オプション | 部品ごとの下書き、節のオン/オフとその記憶【実績】。最小実装ではオプションを `localStorage` に覚えるだけ【追加】 |

### 0.1 本番から外す具体策【追加】【未検証: 各ビルド設定は実アプリで未検証。必ず成果物を検査する】

全プラットフォーム共通: ビルド時に消す（主）→ 実行時の `enabled` / debug 判定（保険）→ **成果物を `scripts/check-no-feedback-in-prod.sh` で grep して CI で落とす**（確認）。外すもの = コード・UI・送信先（URL・鍵）・依存・権限/設定・受け口。HTTP Sink の**開発用サーバの URL・`FEEDBACK_TOKEN` は、開発用のビルド変数・`.env.development` にだけ置き**（コミットしない）、本番の設定・CI の本番ジョブに渡さない。テンプレートは URL の既定値を持たない。

| 環境 | 本番から外す方法 |
|---|---|
| **Electron** | `app.isPackaged` が false のときだけ `await import()` で配線（`electron/main-feedback.mjs` の `isDev`）。preload の `contextBridge.exposeInMainWorld('feedbackApi', …)` も開発時だけ。メニュー項目・アクセラレータも開発時だけ足す。electron-builder の `files` から `dev/feedback/**` を除外（別ディレクトリに置くとやりやすい）。renderer は下の Web と同じ（バンドラの DCE + 動的 import） |
| **Tauri** | Rust 側は `#[cfg(debug_assertions)]` か Cargo feature（`--features feedback` を dev だけ）で `invoke_handler` に登録。`tauri.conf.json` は開発用の設定（`tauri.dev.conf.json` 等）にだけ `connect-src` の開発機を足す。フロントは Web と同じ |
| **Web** | Vite/Rollup: `if (import.meta.env.DEV) { import('./feedback-entry.dev.js') }`（本番ビルドで `false` に置換され、動的 import の先ごと消える）。webpack/esbuild: `process.env.NODE_ENV !== 'production'`。**静的 import にしない**。`html2canvas` は `devDependencies`。送信先は `import.meta.env.VITE_FEEDBACK_*`（`.env.development` のみ。`.env.production` に書かない）。CSP の `connect-src` に開発機を入れない。確認: `npm run build` → `check-no-feedback-in-prod.sh dist`、`npm ls --omit=dev html2canvas` が空 |
| **iOS** | ネイティブの feedback コードは `#if DEBUG … #endif`、または Debug configuration だけが含む別ターゲット/ファイル（Target Membership）。Info.plist の ATS 例外（開発機への http）・`NSLocalNetworkUsageDescription`・`NSBonjourServices` は Debug 用の plist / xcconfig にだけ。確認: Release の `.app` / `.ipa` を `check-no-feedback-in-prod.sh` |
| **Android** | `src/debug/` の source set（debug だけにコンパイルされる）に置き、`src/main/` からは参照しない（release 用には空の `src/release/` のスタブ、または `debugImplementation` の依存 + リフレクション/ServiceLoader）。`BuildConfig.DEBUG` は保険。`usesCleartextTraffic`・`network_security_config`（開発機のドメイン）・追加の権限は `src/debug/AndroidManifest.xml` だけ。依存は `debugImplementation`。確認: `assembleRelease` の apk を `check-no-feedback-in-prod.sh`、`aapt2 dump permissions` |
| **React Native** | `__DEV__ ? require('./dev/react-native-feedback') : stub`（Metro が `__DEV__` を定数にして dead branch を消すか**【未検証】要確認**。必ず release バンドルを grep）。依存は `devDependencies`（`react-native-view-shot`）。release の autolinking から外す設定は `react-native.config.js`【未検証】。送信先は開発用の設定（react-native-config の `.env.development`）だけ。Android/iOS の権限・ATS・cleartext は上の行 |
| **Flutter** | **別 entrypoint**（`lib/main_dev.dart` だけが `flutter_feedback.dart` を import。`lib/main.dart` は import しない。`flutter run -t lib/main_dev.dart`）。`kDebugMode` は保険。送信先は `--dart-define-from-file=env.development.json`（本番の define に入れない）。確認: release の apk/ipa（`libapp.so` の文字列）を grep（AOT 後は識別子が潰れるので、`__feedback`・`x-feedback-token` などの文字列で見る） |

本番ユーザー向けに**意図して**入れたい場合は、上の「外す」とは逆の判断になる。同意（オプトイン）・個人情報の扱い（スクショ・ログ・画面の状態に何が写るか）・送信先と保存期間・鍵の持ち方を別途設計する（`references/build/build.md` の未確定事項）。この skill のテンプレートはその用途を想定していない。

### 0.2 既存のデバッグ基盤を探す・乗せる【追加】【未検証: 各ツールの API は一般知識で未検証】

**原則: アプリ内に既存のデバッグ用ツールがあれば、そこに載せる**（新しい入口・新しい本番除外のガードを作らない）。見つからなければ §0.1 のとおり新規に作る。

**探すもの（共通）**: debug menu・開発者設定画面・隠しジェスチャ（シェイク・長押し・n 回タップ・バージョン表記の連打）・feature flag・内製の dev panel / debug page・環境切り替え（API の向き先を変える画面）。

| 環境 | 探すものの例 |
|---|---|
| Web | dev toolbar（フレームワーク付属・内製）・Storybook（addon / toolbar）・URL パラメータ（`?debug=1`）・`window.__DEV_TOOLS__` のような内製の口 |
| React Native | Dev Menu（`DevSettings.addMenuItem` で項目を足せる）・Flipper（プラグイン。現在は非推奨方向なので存在確認のみ）・内製の debug screen |
| iOS | シェイクで出る debug 画面・FLEX / Netfox などの debug ライブラリ・`#if DEBUG` の設定画面・Settings.bundle の debug 項目 |
| Android | debug drawer（DebugDrawer / 内製）・Hyperion・通知から開く debug 画面・`src/debug/` の Activity・Developer options 風の設定 |
| Flutter | debug 用 overlay（`debugPaint` 系は対象外。内製の overlay）・独自の debug page・`kDebugMode` で出す FAB・`flutter_driver` 以外の dev menu パッケージ |
| Electron / Tauri | アプリメニューの「開発」「Debug」項目・`app.isPackaged` / `cfg(debug_assertions)` で足しているメニュー・開発用ショートカット |

**探し方（grep のキーワード例）**: `debug menu` / `debugMenu` / `DebugMenu` / `DevMenu` / `devtools` / `dev panel` / `DevSettings` / `isDebug` / `isDev` / `__DEV__` / `kDebugMode` / `BuildConfig.DEBUG` / `#if DEBUG` / `process.env.NODE_ENV` / `import.meta.env.DEV` / `feature flag`（アプリの flag 名・`FeatureFlag` / `Remote Config`）/ `shake` / `onLongPress` / `tapCount`。`bash scripts/detect.sh` が同じキーワードで候補ファイルを出す（情報であり、判定は変えない）。

**乗せるときの注意**
1. **その基盤が本当に本番から外れているか確認する**: 基盤のコードが release ビルドに入っていないか（§0.1 と同じ検査: 成果物の grep、その基盤固有の文字列。例: `DevSettings` / `DebugDrawer` のクラス名・メニュー文言）。**外れていなければ、乗せずにユーザへ指摘して確認する**（feature flag だけで隠している基盤は、フラグ次第で本番に出る）。
2. **新しい入口・ガードを作らない**: 入口は基盤のもの、本番除外はその基盤の仕組み（`__DEV__`・debug source set・`app.isPackaged`・feature flag のビルド変数）。足したコードが基盤の外に静的 import で残らないようにする（フィードバック用の依存・送信先は §0.1 のとおり）。
3. **既存の UI 規約・権限に合わせる**: メニューの文言・並び・アイコン・アクセスの条件（誰が開けるか。社内ビルドだけ・QA だけ等）に従う。基盤に権限（認証・社内アカウント）があるなら、フィードバックモードもその内側に置く。
4. **重複する機能は再利用する**: 基盤が既にスクショ撮影・ログのバッファ・環境情報・バグ報告（共有シート・Slack への送信）を持っていれば、それを `capture` / `logs` / `getEnv` に渡し、新しく作らない。既存のバグ報告機能の送信先が Sink として使えるなら、Transport / Sink の差し替えで繋ぐ（`architecture.md` §2.2）。
5. 基盤が持つ**操作の制約**（モード中に押せない・画面遷移が起きる）と、フィードバックモードの「アプリの操作を止める」（全面オーバーレイ）が衝突しないか、基盤のメニューを閉じてからモードに入るかを確認する。

最小例（既存メニューに 1 項目足すだけ）: `assets/templates/existing-debug/`（React Native の Dev Menu、Electron の開発用メニュー）。

### 0.3 既存のログ基盤を探す【追加】【未検証: 各ライブラリの API は一般知識で未検証】

**ログは feedback と別タスク**（`architecture.md` §2.5。`build.md` 手順 1c）。既存のログ基盤があれば、`getRecentLogs` でそれを読む。探し方（grep: `logger` / `Logger` / `log.` / `winston` / `pino` / `electron-log` / `Sentry` / `breadcrumb` / `Timber` / `os_log` / `OSLog` / `CocoaLumberjack` / `NSLog` / `Logcat` / `debugPrint` / `logging` / `console.` の集約ラッパー / `LogBuffer` / `ring buffer`）:

| 環境 | 既存ログ基盤の例（探すもの） | 読み出しの口の例 |
|---|---|---|
| Web | 内製の logger・pino/loglevel・Sentry breadcrumb・`console` のラッパー | バッファ/`getBreadcrumbs`・IndexedDB のログ |
| Electron / Tauri | `electron-log`（ファイルとメモリ）・main の集約 logger（参照実装は `log.ts`）・Tauri の `tauri-plugin-log` | main が持つバッファ（参照実装は `log.entries()`）・ログファイルの末尾 |
| React Native | 内製 logger・`react-native-logs`・Sentry・Flipper のログ | メモリのバッファ・ファイルの末尾（`react-native-fs`） |
| iOS | `os_log`/`OSLog`・CocoaLumberjack・`NSLog`・内製 | `OSLogStore`（iOS 15+）・Lumberjack のファイルの末尾 |
| Android | Timber・`Log.*`・Logcat・内製の file logger | 自プロセスのログは Timber の Tree でバッファ、`logcat -d`（自アプリのみ）【未検証】 |
| Flutter | `logger`・`logging` パッケージ・`debugPrint`・Crashlytics のログ | `Logger.root.onRecord` で溜めたバッファ |

どれも無ければ、差し込み口だけ置いてログ節は空にする。暫定は `assets/templates/web/log-buffer.example.js`（Web 用の例）。

## 1. Desktop

### 1.1 Electron（参照実装と同じ構成）【実績】

- 構成: renderer（`feedback.ts` = モード UI）→ preload（`copyFeedback` / `saveFeedback` / `feedbackOptions` …を `contextBridge` で公開）→ main（`buildFeedback`: `webContents.capturePage()` でスクリーンショット → 文を組み立て → クリップボード or `feedbackFile.ts`）。メニューは main の `Menu`（`appInfo.ts`）で、クリックを IPC `app:menu-feedback` で renderer に伝える。モードの入り・出は renderer → main（`app:feedback-mode`）に知らせてメニューの表記（「〜へ」/「〜を終える」）を切り替える。
- 最小導入【追加】: `assets/templates/web/feedback-mode.js`（renderer）+ `assets/templates/electron/main-feedback.mjs`（main の IPC `app:feedback-save`。`feedback-core.mjs` の `submitFeedback` と Sink を使う）+ preload に数行。`transport: ipcTransport(window.feedbackApi.save)`、`capture: async () => ({})`（main が撮る）にする。**開発時（`!app.isPackaged`）だけ配線する（§0.1）**。
- 注意【実績】:
  - **ヘッドレス**で動かせるようにする（`HEADLESS` 等の環境変数・クリップボードに書かない・`show:false`）。スモークは実入力（デバッガ経由）で。
  - 開発起動（`npm run dev`）では **main が古いまま renderer だけ新しくなる**ことがある → 新しい IPC・設定キーを知らないエラー。再起動で直る。質問型フィードバックの多くがこれ。
  - macOS では `capturePage` は dpr 倍の画像（`cropRect` が dpr を掛ける）。
  - モーダル `<dialog>` を開いている間は、モードの DOM をそのダイアログの中に置く（`feedback-mode.js` の `enter` が対応済み）。
- 本番（パッケージ版）に含めるか: 参照実装は含めている【実績】。汎用版の既定は含めない（`registerFeedback` の `isDev`、§0.1）【追加】。

### 1.2 Tauri（Rust + WebView）【未検証】

- renderer は Web と同じ `feedback-mode.js`。`transport` に `@tauri-apps/api` の `invoke('feedback_save', body)` を渡す。
- Rust 側のコマンドで、body を受けて**リポジトリルートに書く**（`feedback-core.mjs` の規則を Rust に移植: `yymmdd-xx.md`・`wx` 相当の `OpenOptions::new().create_new(true)`・`archived/` も数える・ルート外/シンボリックリンクの拒否・redact）。移植が面倒なら、Node の sidecar（`feedback-dev-server.mjs`）を dev 中だけ起動して `fetch` で送ってもよい（`tauri.conf.json` の CSP/allowlist で `127.0.0.1:7373` への接続を許可）。
- スクリーンショット: Tauri にはウィンドウ画像の標準 API が無い【未検証】。選択肢: (a) html2canvas（Web と同じ。WebView の描画と差が出る）、(b) Rust の `xcap` などのクレートでウィンドウを撮る、(c) OS の `screencapture`（macOS）を呼ぶ。dpr と切り抜き（`cropRect`）の扱いは同じ。
- 「Tauri のアプリのメニュー」から入るなら `tauri::menu` のイベントで `emit` して renderer が `fb.enter('menu')`。

### 1.3 そのほか Desktop（.NET MAUI / Qt / Swift・AppKit 等）【未検証】

DOM が無いので §3 のネイティブ方式（部品の registry か、ビュー階層のヒットテスト）を使う。スクリーンショットは各 OS の API（macOS: `CGWindowListCreateImage`/`ScreenCaptureKit`、ウィンドウの `cacheDisplay(in:to:)`）。受け口は同じ HTTP（dev サーバ）でよい。

## 2. Web

- 導入【追加】: `feedback-entry.dev.js`（開発ビルドだけの入口）→ `feedback-mode.js`（ES module。`enabled: true`・`transport: httpTransport(url)` が必要）+ `feedback-dev-server.mjs`（`node` で別プロセス。裏の Sink は既定がファイル。または Vite の `configureServer` に `createHandler({sink})` を載せる【未検証】）。開発サーバ（Vite 等）から別オリジン（`127.0.0.1:7373`）に POST するので、受け口は localhost 系の Origin を許可している（それ以外は 403）。
- スクリーンショット【未検証】: `html2canvas`（`window.html2canvas` があれば `feedback-mode.js` の既定が使う。CDN/npm で読み込む）。制約: CSS の一部（`backdrop-filter`・一部の `mix-blend-mode`・クロスオリジン画像・`<canvas>`/`<video>` の中身・Shadow DOM）が崩れる。代替: `getDisplayMedia`（共有の許可ダイアログが出る。タブの映像を 1 フレーム取る）、`modern-screenshot`/`dom-to-image-more`、Playwright 等の E2E で撮る。`opts.capture` で差し替える。
- 「ホバーした部品の枠」・`elementsFromPoint`・`closest` は標準 API で動く【実績】。iframe の中は選べない（別 document）【未検証】。
- ホットキーは既存のショートカットとぶつからないものに（`Alt+Shift+F`）。Mac の Alt+文字は別の文字になることがあるので `e.code` で判定している【追加】。
- 本番: §0.1。
- 認証が要る画面のスクリーンショットや、ユーザの個人情報が写る画面では、送信前にプレビューを出す・要素を伏せる、などを検討する【追加】【未検証】。
- 場所を一意に指す手がかり（XPath・CSS セレクタは保存の前に引き直す・テキストフラグメント・Markdown 由来のページのソースの行）: `locate.md`。操作（popover・モードの外の右クリック・ページ全体への送信・スマホのタップとシート）: `interaction.md`。ページと箇所ごとの継続メモ: `memo.md`。

### 2.1 Astro の Dev Toolbar に載せる【実績: Astro 7 + Starlight の docs サイト】

フレームワークが dev 用の toolbar を持っていれば、それが「既存のデバッグ基盤」（§0.2）になる。Astro はその代表で、入口・本番からの除外・画面とサーバーの通り道がそろっている。ひな形: `assets/templates/web/astro/`（`feedback-integration.ts`・`feedback-app.ts`・`protocol.ts`・`check-no-dev-tools-in-dist.sh`。コアは `scripts/copy-core.sh` で同じフォルダの `core/` に置く）。

- **`command === 'dev'` のときだけ登録する**: 統合の `astro:config:setup` で `command !== 'dev'` なら何もしない。`addDevToolbarApp({ id, name, icon, entrypoint })` で toolbar のアプリにする。build・preview・sync には toolbar アプリも、下のログのバッファも、Markdown のプラグインも出ない。FAB・ホットキー・`enabled` の保険は要らない（統合そのものが登録されない）。
- **保存は toolbar のメッセージで受ける**: `astro:server:setup` の `toolbar.on('<id>:save', …)` で受け、Sink に書いて `toolbar.send('<id>:result', …)` で返す。ブラウザ側は toolbar アプリの `server.send` / `server.on`。Vite の HMR の WebSocket に乗るので、HTTP の受け口・ポート・CORS・トークンが要らない（Electron の IPC と同じ位置づけ）。結果は `requestId` で依頼と対応づけ、時間切れ（約 10 秒）で失敗にする。`server.on` の購読は外せないので、アプリの `init` で 1 回だけ登録する。
- **受け口で形と大きさを確かめる**: `astro dev --host` で LAN に出すと、同じネットワークからページを開いた人が保存できる。依頼の形・コメントの字数・全体の大きさ・添付の数と大きさを受け口で検査し、`--host` のときは起動時に「LAN から書き込める」と警告を 1 行出す。
- **ログのバッファは head-inline**: `injectScript('head-inline', …)` で、ページのどの script より前に入れる（toolbar アプリの `init` より前のエラー・hydration の失敗も拾う）。中身は console の error / warn / log・未捕捉の例外・unhandledrejection・画像や script の読み込み失敗（capture の `error`）・Vite の `vite:error`（`/@vite/client` を動的 import して `createHotContext(...).on('vite:error')`）を、件数と字数の上限つきのリングバッファに溜めるだけ。バンドルを通らないので ES5 の文字列で書く。これは feedback 専用の暫定のログで、ログ基盤ではない（`architecture.md` §2.5）。
- **Markdown のソースの行**（任意）: `astro:config:done` で Markdown の processor にプラグインを差す（`locate.md` §4。Astro 7 は Sätteri）。
- **本番の dist を grep する検査を CI に入れる**: `check-no-dev-tools-in-dist.sh <dist>` を build の後に 1 ステップ足す。toolbar アプリの id・ログのバッファの global 名・dev だけの属性名・dev だけの依存名を grep し、1 つでもあれば落とす。**dist に html が 1 つも無いときも落とす**（ビルドの失敗を「何も見つからない」で通さない）。パターンは統合の定数と突き合わせるテストを置くとずれない。

  ```yaml
  # 例: GitHub Actions の docs のジョブ
  - run: pnpm run build
  - run: sh <dev 専用の場所>/check-no-dev-tools-in-dist.sh dist
  ```
- 置き場は `.local/feedback/` のような git に入らない場所にし、環境変数（ひな形は `FEEDBACK_DIR`）で差し替えられるようにする。検証では本物の置き場を汚さないよう、一時ディレクトリに向ける。
- 依存を動的 import する（スクショのライブラリなど）なら、`optimizeDeps.include` に先に入れる。入れないと、初めて読んだ瞬間に Vite がページを再読み込みする【未検証: 作りかけ。`interaction.md` §5】。
- Astro 7 より前の版の toolbar API（`app` / `server` の helpers）は【未検証】。

## 3. モバイル

共通の前提【追加】【未検証】:
- **DOM が無い**ので、「名前を付けた部品を registry に登録 → タップ位置を含む最小の矩形を選ぶ」方式にする（`assets/templates/mobile/*` はこの方式）。またはネイティブのビュー階層をたどってヒットテスト（iOS `UIView.hitTest`、Android `ViewGroup` を再帰）。名前は `accessibilityIdentifier` / `resource-id`・`contentDescription` / `testID` / `Semantics.label` / `Key`。
- **ホバーが無い**: 「モードに入る → 部品をタップ → 吹き出し」。吹き出しは画面下に固定して、キーボードに隠れないようにする。
- **入る手段**: 画面の隅の FAB、シェイク（`RCTDeviceEventEmitter`/`CoreMotion`/`SensorManager`）、3 本指の長押し。
- **送る先**: 端末からリポジトリには書けない。(a) 開発機の受け口（`feedback-dev-server.mjs`。エミュレータ: Android `10.0.2.2:7373` / iOS シミュレータ `localhost:7373`。実機: `adb reverse tcp:7373 tcp:7373`、または `FEEDBACK_HOST=0.0.0.0` で LAN 越し（同じネットワークの誰でも書き込めるので作業中だけ））、(b) 共有シート（Markdown をファイル/クリップボード経由で開発機へ）、(c) イシュートラッカーや Slack に投げ、Agent が取り込む（Sink の差し替え。`references/loop/loop.md` の Sink ごとの読み方）。
- スクリーンショットのスケール: モバイルは論理座標（dp/pt）と画像の px の比 = 画面密度。`cropRect` の `viewport` に論理サイズ、`image` に実画像のサイズを渡す。
- 本番: §0.1（`__DEV__` / `kDebugMode` / `BuildConfig.DEBUG` / `#if DEBUG` は保険で、主はビルド時に消すこと）。ストア審査・個人情報（画面・ログに個人データが写る）に注意。

### 3.1 React Native【未検証】
`assets/templates/mobile/react-native-feedback.tsx`。`react-native-view-shot` の `captureScreen`。RN の Fabric/新アーキテクチャでの `measureInWindow` の挙動、`Modal` の上にオーバーレイを出す方法（Modal は別ウィンドウなので、Modal の中にも `FeedbackProvider` が要る）は未確認。ログは既存のログ基盤から `getRecentLogs` で読む（無ければ省略。ログ基盤は別タスク。§0.3）。

### 3.2 Flutter【未検証】
`assets/templates/mobile/flutter_feedback.dart`。`RepaintBoundary.toImage`。`Overlay`/`Navigator` の上に出すなら `MaterialApp.builder` で包む（`FeedbackRoot` を `builder` に）。`debugPrint` のフック、`WidgetInspectorService`（debug のウィジェットツリー）を使う案もある。

### 3.3 iOS ネイティブ（Swift）【未検証】
```swift
// スクリーンショット（オーバーレイを隠したあと）
func snapshot(_ window: UIWindow) -> Data? {
  let r = UIGraphicsImageRenderer(bounds: window.bounds)
  return r.image { _ in window.drawHierarchy(in: window.bounds, afterScreenUpdates: true) }.pngData()
}
// 部品の選択: 全面の UIView（オーバーレイ）の touchesEnded で
let hit = window.hitTest(point, with: nil)            // 本来の最前面ビュー
var v: UIView? = hit; while v != nil && (v!.accessibilityIdentifier ?? "").isEmpty { v = v?.superview } // 名前のある最も近い祖先
let rect = v?.convert(v!.bounds, to: nil)            // window 座標の矩形
```
オーバーレイ自体は `hitTest` で除く。SwiftUI は `accessibilityIdentifier(_:)` を付ける。

### 3.4 Android ネイティブ（Kotlin）【未検証】
```kotlin
// スクリーンショット（API 26+: PixelCopy が正確。それ以前は View.draw）
val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
PixelCopy.request(window, bmp, { /* 完了後に PNG へ */ }, Handler(Looper.getMainLooper()))
// 部品の選択: ルートビューを再帰し、タップ座標を含む最も深い View。名前は resources.getResourceEntryName(id) か contentDescription
```
Jetpack Compose は `Modifier.testTag("…")` / `semantics` を付け、`SemanticsNode` の境界から選ぶ。

## 4. 差分の早見表

Web で Astro の Dev Toolbar に載せる場合（§2.1）【実績】: 選択は `elementsFromPoint`、送信は toolbar のメッセージ、置き場は受け口（統合）の裏の file、ログは head-inline のバッファ、本番から外すのは `command === 'dev'` の登録と dist の grep。

| 項目 | Electron【実績】 | Web【追加】 | Tauri【未検証】 | RN / Flutter【未検証】 | ネイティブ【未検証】 |
|---|---|---|---|---|---|
| 部品の選択 | `elementsFromPoint` | 同左 | 同左（WebView） | registry の最小矩形 | ビュー階層のヒットテスト |
| 名前 | `data-fb` | `data-fb` | `data-fb` | `FbTarget` / `testID` | accessibility id / `testTag` |
| 操作を止める | 全面オーバーレイ + capture | 同左 | 同左 | 全面オーバーレイ | 全面オーバーレイ |
| スクショ | `capturePage` | html2canvas 等 | クレート/OS コマンド/html2canvas | view-shot / `toImage` | `drawHierarchy` / `PixelCopy` |
| 送信（Transport） | IPC → main | HTTP → dev サーバ | invoke → Rust または HTTP | HTTP（開発機） | HTTP / 共有シート |
| 置き場（Sink） | file | 受け口の裏（既定 file） | 同左 | 同左 | 同左 |
| ログ（読むだけ。基盤は別タスク） | main の `log.ts` を読む | 既存の logger を `getRecentLogs` で読む（暫定: `log-buffer.example.js`） | 同左 or Rust 側 | 既存の logger / バッファ | `os_log` / Logcat 等の既存基盤 |
| 本番から外す | `app.isPackaged` + 動的 import + files 除外 | DCE + 動的 import | `cfg(debug_assertions)` / feature | `__DEV__` + require / 別 entrypoint | `#if DEBUG` / debug source set |
