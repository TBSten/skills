# mobile 共通の前提

検証状況: 未検証（手順だけ）。モバイルの adapter（`react-native.md`・`flutter.md`・`ios.md`・`android.md`）は、このファイルと合わせて読む。

## Web との違い

| 論点 | モバイルでの扱い |
|---|---|
| 部品の選択（`pick`） | DOM が無い。「名前を付けた部品を registry に登録し、タップ位置を含む最小の矩形を選ぶ」（RN・Flutter のひな形）か、ネイティブのビュー階層をたどってヒットテスト |
| 名前（`name`） | `testID`・`accessibilityIdentifier`・`resource-id` / `contentDescription`・`Semantics.label`・`Key`・`testTag` |
| ホバー | 無い。「モードに入る → 部品をタップ → 吹き出し」。吹き出しは画面下に固定し、キーボードに隠れないようにする |
| 入口（`entry`） | 画面の隅の FAB・シェイク（`DevSettings` / `CoreMotion` / `SensorManager`）・3 本指の長押し・既存の debug 画面の項目 |
| 送り先（`transport`） | 端末からリポジトリには書けない。開発機の HTTP の受け口（`core/feedback-dev-server.mjs`）に submission を送る |
| スクショの倍率 | 論理座標（dp / pt）と画像の px の比 = 画面密度。`cropRect` の `viewport` に論理サイズ、`image` に実画像のサイズを渡す |

## 開発機への届け方

| つなぎ方 | 受け口の URL | 注意 |
|---|---|---|
| Android エミュレータ | `http://10.0.2.2:7373/__feedback` | |
| iOS シミュレータ | `http://localhost:7373/__feedback` | |
| Android 実機（USB） | `adb reverse tcp:7373 tcp:7373` のあと `localhost:7373` | 受け口は 127.0.0.1 のままでよい |
| 実機（同じ LAN） | 開発機の LAN の IP | `FEEDBACK_HOST=0.0.0.0` と `FEEDBACK_TOKEN` が必須。同じネットワークの誰でも書き込めるので作業中だけ |
| 共有シート・Issue・Slack | 無い | Markdown を共有シートで渡す、または Sink を差し替えて Agent が取り込む（`references/spec/transport.md` §2） |

## 本番から外す（共通）

- ビルド時に消す（別 entrypoint・debug source set・`#if DEBUG`）が主。`__DEV__` / `kDebugMode` / `BuildConfig.DEBUG` は保険。
- 開発機への http を許す設定（Android の `usesCleartextTraffic`・`network_security_config`、iOS の ATS 例外・`NSLocalNetworkUsageDescription`・`NSBonjourServices`）は debug 用の設定にだけ置く。
- 送信先の URL・鍵は開発用の設定だけ。
- release の apk / aab / ipa を `check-no-feedback-in-prod.sh` で検査する（zip を展開して grep する）。権限は `aapt2 dump permissions`（Android）、Release の Info.plist（iOS）を目視。
- ストア審査と個人情報（画面・ログに個人データが写る）に注意する。

## 既存のデバッグ基盤の例

| 環境 | 探すもの |
|---|---|
| React Native | Dev Menu（`DevSettings.addMenuItem`）・内製の debug screen |
| iOS | シェイクで出る debug 画面・FLEX などの debug ライブラリ・`#if DEBUG` の設定画面 |
| Android | debug drawer・Hyperion・通知から開く debug 画面・`src/debug/` の Activity |
| Flutter | 内製の debug overlay・debug page・`kDebugMode` で出す FAB |

## 既存のログ基盤の例

| 環境 | 探すもの | 読み出し |
|---|---|---|
| React Native | 内製 logger・`react-native-logs`・Sentry | メモリのバッファ・ファイルの末尾 |
| iOS | `os_log` / `OSLog`・CocoaLumberjack | `OSLogStore`（iOS 15+）・ファイルの末尾 |
| Android | Timber・`Log.*` | Timber の Tree でバッファ |
| Flutter | `logger`・`logging` | `Logger.root.onRecord` で溜めたバッファ |
