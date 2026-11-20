# mobile / ios（Swift・SwiftUI）

検証状況: macOS 26.2・Xcode 26.2（Swift 6.2.3、`SWIFT_VERSION = 6`、最低 iOS 17）・iOS 26.2 シミュレータ（iPhone 17 Pro）で、画面 1 枚の最小 SwiftUI アプリを Maestro 2.3.0 で操作して確かめた。確かめたこと: 入口ボタンで入る → 部品をタップ → 「親を選ぶ」で広がる → コメント → 送る → 受け口（`core/feedback-dev-server.mjs`）の Inbox に記録ができ、`window.png`（赤枠）と `element.png`（周り 24pt）が付く → モードが終わる。モード中に下のボタンをタップしてもカウンタが増えない。受け口を止めると理由が出てモードが続き、「やめる」で出られる。環境変数 `FEEDBACK_ENDPOINT` が Info.plist の値より優先される。Release の `.app` に痕跡が無い（`scripts/check-ios-release.sh`）。
未検証: 実機（LAN 越し・ATS）、シェイクの入口、UIKit だけの画面・`sheet`・`NavigationStack` の中、スクロールする一覧、R8 のフォーカスを戻す（外すだけ）、送信先が空のときの文言、`.ipa`（archive）での検査。

ひな形: `assets/templates/adapters/mobile/ios/`（導入先の app target にコピーする）
- `FbTarget.swift`: `.fbTarget("名前")`・`.feedbackEntryButton()`。Debug と Release の両方でビルドし、Release は `Self` を返すだけ
- `FeedbackMode.swift`（本体・送信）・`FeedbackOverlay.swift`（オーバーレイ・registry）・`FeedbackShot.swift`（スクショ・OSLog の読み出し）: ファイル全体が `#if DEBUG`
- `Feedback.Debug.xcconfig`・`Info.plist.feedback-fragment.xml`: 送信先を Debug にだけ入れる設定の断片

先に `common.md` を読む。受け口は開発機で `FEEDBACK_REPO_ROOT=<導入先> node core/feedback-dev-server.mjs`（`scripts/copy-core.sh` でコピー）。

## 差し込み口

起動時（`App.init` など）に `#if DEBUG` の中で `FeedbackMode.shared.config.xxx = …` を埋める。

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 既存の debug 画面があれば、その項目から `FeedbackMode.shared.enter()` を呼ぶ。無ければルートの View に `.feedbackEntryButton()`（右下の「FB」）。シェイク（`UIWindow.motionEnded`）も入っているが未検証。不要なら消す |
| `name` | `.fbTarget("ホーム")` を外側に、`.fbTarget("保存ボタン")` を内側に付けると道筋が `ホーム/保存ボタン` になる。GeometryReader で window 座標の矩形を registry に登録する（`accessibilityIdentifier` は付けない。既存の UI テストの id を壊さないため） |
| `pick` | タップ位置を含む `.fbTarget` のうち最も小さい矩形。「親を選ぶ」で 1 つ外側へ。名前の無い部品は選べない（名前の付いた外側が選ばれる）。要素の種類・ラベルは取らず、`element` は名前の最後の段 |
| `block` | アプリの window の上に別の `UIWindow`（`windowLevel = .alert + 1`）を出し、全面で `SpatialTapGesture` を受ける。タップ・ジェスチャはアプリの window に届かない |
| `collect` | `getScreen`・`getView`（モデルから。`[String: Any]` で JSON にできる値だけ）・`stateOf(path)`。`env`（OS・端末・版・シミュレータか）と `viewport`（pt と scale）は自動。viewport は記録の「今の画面」に出る |
| `logs` | `config.getRecentLogs = { FeedbackMode.osLogEntries() }` で、このプロセスの `os.Logger` のログ（subsystem = bundle id、直近 5 分）を読む。`.warning` は error として出る |
| `capture` | アプリの window だけを `drawHierarchy` で描く（オーバーレイは別の window なので写らない）。赤枠を描き、周り 24pt を切り抜く。倍率は `min(scale, 2)`。描けなければ `shotError` |
| `transport` | `URLSession` で `POST`（JSON、PNG は base64）。送信先は 環境変数 `FEEDBACK_ENDPOINT` → Info.plist の `FeedbackEndpoint`。既定の URL は持たない。鍵は `FEEDBACK_TOKEN` / `FeedbackToken`（未検証） |
| `pointer` | `config.pointer` |

記録に入る節は `config.options`（既定は受け口と同じで env・screen・view がオフ）。

## 送信先を Debug にだけ入れる

1. target の Build Settings で `INFOPLIST_PREPROCESS = YES`（Debug・Release の両方）。
2. Debug の xcconfig から `#include "Feedback.Debug.xcconfig"` し、その後に `FEEDBACK_ENDPOINT = http:/$()/127.0.0.1:7373/__feedback`（xcconfig は `//` がコメントになる）。
3. Info.plist の `<dict>` に `Info.plist.feedback-fragment.xml` の `#ifdef FEEDBACK_MODE … #endif` を足す。`GENERATE_INFOPLIST_FILE = YES` と併用でき、生成分と合わさった（DOCTYPE の URL も壊れなかった）。Info.plist が無い target は、空の Info.plist を作って `INFOPLIST_FILE` に指定する。
4. シミュレータで別の送信先を試すときは `SIMCTL_CHILD_FEEDBACK_ENDPOINT=http://localhost:7374/__feedback xcrun simctl launch <udid> <bundle id>`（Info.plist より優先）。

## 本番から外す

- `FeedbackMode.swift`・`FeedbackOverlay.swift`・`FeedbackShot.swift` は全体が `#if DEBUG`。呼ぶ側（`App.init` の設定）も `#if DEBUG` で囲む。
- `.fbTarget` は Release で `-> Self` の no-op。`-> some View` で `self` を返す形では、Release のシンボル表に `fbTarget` の名前が残った。今の形では名前の文字列（`"ホーム/保存ボタン"`）も Release の実行ファイルから消えた。
- 確かめる: `xcodebuild -scheme <S> -configuration Release -sdk iphonesimulator -derivedDataPath build CODE_SIGNING_ALLOWED=NO build` のあと `bash "${CLAUDE_SKILL_DIR}/scripts/check-ios-release.sh" build/Build/Products/Release-iphonesimulator/<App>.app "<部品の名前>"`。`check-no-feedback-in-prod.sh` に加えて、Info.plist のキー（`FeedbackEndpoint`・ATS の例外・`NSLocalNetworkUsageDescription`・`NSBonjourServices`）と、実行ファイルの型名・`fbTarget`（大文字小文字を区別しない）を見る。Debug の `.app` に掛けると見つかる（検査が効いていることの確認）。

## この環境の注意

- ATS: シミュレータから `http://127.0.0.1:7374` と `http://localhost:7374` へは、ATS の例外なしで送れた。実機から開発機の LAN の IP に送るときに例外（`NSAllowsLocalNetworking` など）が要るかは未検証。要るなら Info.plist の `#ifdef FEEDBACK_MODE` の中に置く。
- 入力中にキーボードが出ると、アプリ側の SwiftUI も「キーボードを避ける」配置に動く。送る前にキーボードを閉じて 0.5 秒待ち、選んだ部品の矩形を registry から引き直してから撮る（引き直さないと赤枠がずれた）。
- Debug ビルドの実体は `<App>.debug.dylib`（Xcode 26 の既定）。Debug の `.app` を grep するときは dylib も見る。
- 自動操作: Maestro の `tapOn: { point: "x,y" }` でオーバーレイの上から座標をタップし、`assertVisible` で道筋・カウンタを確かめた。日本語の `inputText` も通った。
