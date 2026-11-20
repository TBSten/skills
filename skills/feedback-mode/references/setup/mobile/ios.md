# mobile / ios（Swift・SwiftUI）

検証状況: macOS 26.2・Xcode 26.2（Swift 6.2.3、`SWIFT_VERSION = 6`、最低 iOS 17）・iOS 26.2 シミュレータ（iPhone 17 Pro）・Maestro 2.3.0。2 回確かめた: (1) 画面 1 枚の最小アプリ、(2) この docs とひな形だけを読んだ別の担当が、新しいアプリ（TabView・NavigationStack・40 行の List・TextField・`.sheet`・`.alert`・`.confirmationDialog`）に導入した。
- 通った: 入口で入る → 部品をタップ → 「親を選ぶ」→ コメント（空でも可）→ 送る → 受け口の Inbox に schema 1 の記録と `window.png`（赤枠）・`element.png`（周り 24pt）。モード中はタップ・スクロール・端からのスワイプで戻る・タブの切り替えがアプリに届かない。スクロールした後の行・画面遷移の後（前の画面の部品は残らない）・ダークモード・横向き・入力中に入って出る（フォーカスが戻る）・受け口が止まっているとき（理由が出てモードが続く）・送信先が空のとき（理由が出る）。Release の `.app` に痕跡が無い（`scripts/check-ios-release.sh`。Debug では見つかる）。
- 例外: `.alert`・`.confirmationDialog`（UIKit のシステムの部品）は名前を付けられず、選べない。出ている間は入口も押せない。
- 未検証: 実機（LAN 越し・ATS）、シェイクの入口、UIKit だけの画面、IME の未確定の文字、`.ipa`（archive）での検査。

ひな形: `assets/templates/adapters/mobile/ios/`
- **app target に入れるのは `.swift` の 4 枚だけ**:
  - `FbTarget.swift`: `.fbTarget("名前")`・`.feedbackEntryButton()`。Debug と Release の両方でビルドし、Release は `Self` を返すだけ
  - `FeedbackMode.swift`（本体・送信）・`FeedbackOverlay.swift`（オーバーレイ・registry）・`FeedbackShot.swift`（スクショ・OSLog の読み出し）: ファイル全体が `#if DEBUG`
- `config/`（**target に入れない**。プロジェクトの `Config/` などに置く）: `Feedback.Debug.xcconfig`・`Info.plist.feedback-fragment.xml`（送信先を Debug にだけ入れる設定の断片）。Xcode 16 以降の同期フォルダ（`PBXFileSystemSynchronizedRootGroup`）にひな形のフォルダを丸ごと入れると、断片の xml がリソースとして Release の `.app` に入った

先に `common.md` を読む。受け口は開発機で `FEEDBACK_REPO_ROOT=<導入先> node core/feedback-dev-server.mjs`（`scripts/copy-core.sh` でコピー）。記録は `<FEEDBACK_REPO_ROOT>/feedback/`、画像は `<FEEDBACK_REPO_ROOT>/.local/tmp/feedback/<時刻>/`。

## 差し込み口

起動時（`App.init` など）に `#if DEBUG` の中で `FeedbackMode.shared.config.xxx = …` を埋める。

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 既存の debug 画面があれば、その項目から `FeedbackMode.shared.enter()` を呼ぶ。無ければルートの View に `.feedbackEntryButton()`（右下の「FB」）。**`.sheet`・`.fullScreenCover` の中身にも `.feedbackEntryButton()` を付ける**（ルートのボタンはシートに隠れる。付ければシートの中も選べ、スクショにも写る）。シェイク（`UIWindow.motionEnded`）も入っているが未検証。不要なら消す |
| `name` | `.fbTarget("ホーム")` を外側に、`.fbTarget("保存ボタン")` を内側に付けると道筋が `ホーム/保存ボタン` になる。GeometryReader で window 座標の矩形を registry に登録する（`accessibilityIdentifier` は付けない。既存の UI テストの id を壊さないため）。**一覧の行は内容で名前を付け、ラベルを行いっぱいに広げてから付ける**: `ForEach(items) { item in NavigationLink(value: item) { Text(item.name).frame(maxWidth: .infinity, minHeight: 44, alignment: .leading) }.fbTarget(item.name) }`（広げないと矩形が文字の高さしかなく、行の余白のタップで親が選ばれる） |
| `pick` | タップ位置を含む `.fbTarget` のうち最も小さい矩形。「親を選ぶ」で 1 つ外側へ。名前の無い部品は選べない（名前の付いた外側が選ばれる）。要素の種類・ラベルは取らず、`element` は名前の最後の段 |
| `block` | アプリの window の上に別の `UIWindow`（`windowLevel = .alert + 1`）を出し、全面で `SpatialTapGesture` を受ける。タップ・ジェスチャはアプリの window に届かない。吹き出しは、選んだ部品が画面の下半分なら上に、上半分なら下に出す（部品を隠さない） |
| `collect` | `getScreen`・`getView`（モデルから。`[String: Any]` で JSON にできる値だけ）・`stateOf(path)`。`env`（OS・端末・版・シミュレータか）と `viewport`（pt と scale）は自動。viewport は記録の「今の画面」に出る |
| `logs` | `config.getRecentLogs = { FeedbackMode.osLogEntries() }` で、このプロセスの `os.Logger` のログ（subsystem = bundle id、直近 5 分）を読む。`.warning` は error として出る |
| `capture` | アプリの window だけを `drawHierarchy` で描く（オーバーレイは別の window なので写らない）。赤枠を描き、周り 24pt を切り抜く。倍率は `min(scale, 2)`。描けなければ `shotError` |
| `transport` | `URLSession` で `POST`（JSON、PNG は base64）。送信先は 環境変数 `FEEDBACK_ENDPOINT`（空なら無視）→ Info.plist の `FeedbackEndpoint`。既定の URL は持たない。鍵は `FEEDBACK_TOKEN` / `FeedbackToken`（未検証） |
| `pointer` | `config.pointer` |

記録に入る節は `config.options`（既定は受け口と同じで env・screen・view がオフ）。入る前にフォーカスがあった入力欄は、出たときにフォーカスを戻す（R8）。

## 送信先を Debug にだけ入れる

1. `config/` の 2 枚を、target の外（例: `Config/`）に置く。
2. Debug の xcconfig を用意する。新しいプロジェクトには無いので、`Config/Debug.xcconfig` を作って `#include "Feedback.Debug.xcconfig"` と `FEEDBACK_ENDPOINT = http:/$()/127.0.0.1:7373/__feedback` の 2 行を書き（xcconfig は `//` がコメントになる）、project か target の **Debug の構成ファイル**（Build Settings の Based on Configuration File。pbxproj では `baseConfigurationReference`）に指定する。Release には何も指定しない。
3. target の Build Settings で `INFOPLIST_PREPROCESS = YES`（Debug・Release の両方。Release で `#ifdef` の行を消すため）。
4. Info.plist の `<dict>` に断片の `#ifdef FEEDBACK_MODE … #endif` を足す。`GENERATE_INFOPLIST_FILE = YES` と併用でき、生成分と合わさる。Info.plist が無い target は、空の Info.plist を **target の外**（`Config/Info.plist`）に作って `INFOPLIST_FILE` に指定する（同期フォルダの中に置くと「Multiple commands produce」になる）。断片のコメントは ASCII のままにする（全角の文字があるとプリプロセッサが警告を出す）。
5. シミュレータで別の送信先を試すときは `SIMCTL_CHILD_FEEDBACK_ENDPOINT=http://localhost:7374/__feedback xcrun simctl launch <udid> <bundle id>`（Info.plist より優先）。

## 本番から外す

- `FeedbackMode.swift`・`FeedbackOverlay.swift`・`FeedbackShot.swift` は全体が `#if DEBUG`。呼ぶ側（`App.init` の設定）も `#if DEBUG` で囲む。
- `.fbTarget` は Release で `-> Self` の no-op。`-> some View` で `self` を返す形では、Release のシンボル表に `fbTarget` の名前が残った。今の形では名前の文字列（`"ホーム/保存ボタン"`）も Release の実行ファイルから消えた。
- 確かめる: `xcodebuild -scheme <S> -configuration Release -sdk iphonesimulator -derivedDataPath build CODE_SIGNING_ALLOWED=NO build` のあと `bash "${CLAUDE_SKILL_DIR}/scripts/check-ios-release.sh" build/Build/Products/Release-iphonesimulator/<App>.app "<部品の名前>"`。`check-no-feedback-in-prod.sh` に加えて、Info.plist のキー（`FeedbackEndpoint`・ATS の例外・`NSLocalNetworkUsageDescription`・`NSBonjourServices`）と、実行ファイルの型名・`fbTarget`（大文字小文字を区別しない）を見る。Debug の `.app` に掛けると見つかる（検査が効いていることの確認）。
- 検査に渡す部品の名前は、画面に表示される文字と違うものを選ぶ（例: 画面に「タップ回数」と出ているなら `"ホーム/タップ回数"` のように道筋ごと）。表示の文字は Release にも当然あるので、そのまま渡すと見つかってしまう。

## この環境の注意

- `.alert`・`.confirmationDialog` は選べない（features.md R3 の「ダイアログの中も選べる」の例外）。タブバー・ナビゲーションバーも名前を付けられないので、そこをタップすると下にある部品が選ばれることがある。
- ATS: シミュレータから `http://127.0.0.1:7374` と `http://localhost:7374` へは、ATS の例外なしで送れた。実機から開発機の LAN の IP に送るときに例外（`NSAllowsLocalNetworking` など）が要るかは未検証。要るなら Info.plist の `#ifdef FEEDBACK_MODE` の中に置く。
- 入力中にキーボードが出ると、アプリ側の SwiftUI も「キーボードを避ける」配置に動く。送る前にキーボードを閉じて 0.5 秒待ち、選んだ部品の矩形を registry から引き直してから撮る（引き直さないと赤枠がずれた）。
- Debug ビルドの実体は `<App>.debug.dylib`（Xcode 26 の既定）。Debug の `.app` を grep するときは dylib も見る。
- 自動操作: Maestro の `tapOn: { point: "x,y" }` か `tapOn: "<表示の文字>"`（モード中もアプリの window の部品が見える。画面の外の要素は見つからない）でタップし、`assertVisible` で道筋・カウンタを確かめた。日本語の `inputText` も通った。
