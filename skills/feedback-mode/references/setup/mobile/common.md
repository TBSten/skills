# mobile 共通の前提

検証状況: 「端末から開発機へ届かせる」のうち、エミュレータ・シミュレータ・`adb reverse`（エミュレータで）・LAN + トークン（エミュレータから開発機の LAN の IP へ）は確かめた。USB の Android 実機・iOS 実機は未検証。モバイルの adapter（`android.md`・`kmp.md`・`ios.md`）は、このファイルと合わせて読む。

## Web との違い

| 論点 | モバイルでの扱い |
|---|---|
| 部品の選択（`pick`） | DOM が無い。名前を付けた部品の矩形を registry に登録し、タップ位置を含む最小の矩形を選ぶ（Compose・SwiftUI のひな形）。「親を選ぶ」は次に大きい矩形 |
| 名前（`name`） | `Modifier.fbTarget("画面/部品")`（Compose）・`.fbTarget("部品")`（SwiftUI）。既存の `testTag`・`accessibilityIdentifier` は書き換えない |
| ホバー | 無い。「モードに入る → 部品をタップ → 下のシート」。シートは画面下に固定し、キーボードに隠れないようにする |
| 入口（`entry`） | 画面の隅の小さなボタン（ひな形の「FB」）・既存の debug 画面の項目。シェイクは iOS のひな形にあるが未検証 |
| 送り先（`transport`） | 端末からリポジトリには書けない。開発機の HTTP の受け口（`core/feedback-dev-server.mjs`）に submission を送る |
| スクショの倍率 | 論理座標（dp / pt）と画像の px の比 = 画面密度。記録の `rect`・`viewport` は論理座標、画像は `min(密度, 2)` 倍 |

## 端末から開発機へ届かせる

送り方は 1 件ごとの HTTP POST（`POST /__feedback`）。WebSocket は使わない。受け口は開発機の `core/feedback-dev-server.mjs`（既定は `127.0.0.1:7373` にだけ bind）。

| つなぎ方 | 受け口の URL（端末から） | 受け口の起動 | 確かめたこと |
|---|---|---|---|
| Android エミュレータ | `http://10.0.2.2:7373/__feedback` | そのまま | Compose・KMP のひな形で送れた |
| iOS シミュレータ | `http://127.0.0.1:7373/__feedback`（`localhost` も可） | そのまま | SwiftUI・KMP のひな形で送れた。ATS の例外なしで届いた |
| Desktop・Web（同じ Mac） | `http://127.0.0.1:7373/__feedback` | そのまま | KMP の Desktop・wasmJs で送れた。Web は Origin が localhost 系なら CORS に答える |
| Android 実機（USB） | `adb reverse tcp:7373 tcp:7373` のあと `http://localhost:7373/__feedback` | そのまま（127.0.0.1 のまま） | エミュレータに `adb reverse` を掛けて `localhost` で送れた。USB の実機は未検証 |
| 実機（同じ LAN） | `http://<開発機の LAN の IP>:7373/__feedback` | 作業中だけ LAN に出す（下） | エミュレータから開発機の LAN の IP へ、トークン付きで送れた。トークン無しは 401。iOS 実機は未検証 |
| 共有シート・Issue・Slack | 無い | — | 未検証。Sink を差し替える（`references/spec/transport.md` §2） |

### 作業中だけ受け口を LAN に出す（実機）

同じネットワークの誰でも書き込めるようになるので、作業の間だけにする。adapter の本体には入れず、開発機の設定と debug の設定だけで行う。

1. 受け口をトークン付きで起動する: `FEEDBACK_HOST=0.0.0.0 FEEDBACK_TOKEN=<長い乱数> FEEDBACK_REPO_ROOT=<導入先> node core/feedback-dev-server.mjs`。トークンが無いと起動しない。起動時に「LAN から書き込めます」の警告が出る。
2. 端末の送信先を `http://<開発機の LAN の IP>:7373/__feedback` に、トークンをヘッダ `x-feedback-token` にする（開発機の設定だけ。コミットしない）。
   - Android: `~/.gradle/gradle.properties` に `feedback.endpoint=…`・`feedback.token=…`（debug の `buildConfigField`）。`src/debug/res/xml/feedback_network_security_config.xml` に開発機の IP の `<domain>` を足す（cleartext は IP ごとに許す）。
   - iOS 実機（未検証）: Debug の xcconfig の `FEEDBACK_ENDPOINT` を LAN の IP にする。LAN の IP への http に ATS の例外（`NSAllowsLocalNetworking` など）とローカルネットワークの許可（`NSLocalNetworkUsageDescription`）が要るかは確かめていない。要るなら Debug の Info.plist の断片（`ios.md`）にだけ置く。
3. 終わったら受け口を止め、debug の設定から IP を消す。

## 本番から外す（共通）

- ビルド時に消す（debug source set・ビルドのスイッチ・`#if DEBUG`）が主。`BuildConfig.DEBUG` などの実行時の判定は保険。
- 開発機への http を許す設定（Android の `networkSecurityConfig`・`usesCleartextTraffic`、iOS の ATS 例外・`NSLocalNetworkUsageDescription`・`NSBonjourServices`）は debug 用の設定にだけ置く。
- 送信先の URL・鍵は開発用の設定だけ。
- release の成果物を検査する: Android は `scripts/check-android-release.sh`（APK の grep・manifest の cleartext・debug だけの権限）、iOS は `scripts/check-ios-release.sh`（`.app` / `.ipa` の grep・Info.plist のキー・実行ファイルの型名）。どちらも `check-no-feedback-in-prod.sh` を中で呼ぶ。Kotlin/Native の文字列は UTF-16LE なので、`check-no-feedback-in-prod.sh` は UTF-16LE でも探す。
- ストア審査と個人情報（画面・ログに個人データが写る）に注意する。

## 既存のデバッグ基盤の例

| 環境 | 探すもの |
|---|---|
| iOS | シェイクで出る debug 画面・FLEX などの debug ライブラリ・`#if DEBUG` の設定画面 |
| Android | debug drawer・Hyperion・通知から開く debug 画面・`src/debug/` の Activity |

## 既存のログ基盤の例

| 環境 | 探すもの | 読み出し |
|---|---|---|
| iOS | `os_log` / `OSLog`・CocoaLumberjack | `OSLogStore`（iOS 15+）・ファイルの末尾 |
| Android | Timber・`Log.*` | Timber の Tree でバッファ |
