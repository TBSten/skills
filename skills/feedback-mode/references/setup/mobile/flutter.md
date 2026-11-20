# mobile / flutter

検証状況: 未検証（コンパイル・実機とも未確認）。
ひな形: `assets/templates/adapters/mobile/flutter/flutter_feedback.dart`（`FeedbackRoot` と `FbTarget`）。受け口は開発機の `core/feedback-dev-server.mjs`。先に `common.md` を読む。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | `kDebugMode` で出す FAB、または既存の debug page の項目 |
| `pick` / `name` | `FbTarget(name: '画面/部品', child: …)`（GlobalKey で RenderBox を registry に登録）。タップ位置を含む最小の矩形を選ぶ |
| `block` | モード中だけ Stack の最前面に置く `GestureDetector` |
| `collect` | ひな形は `appName`・`comment`・viewport だけ。画面の状態が要るなら引数を足す |
| `logs` | `getRecentLogs`（`Logger.root.onRecord` で溜めたバッファなど） |
| `capture` | アプリ全体を `RepaintBoundary(key: FeedbackRoot.boundaryKey)` で包み、`toImage` → PNG（オーバーレイを隠してから） |
| `transport` | `devTransport`（ひな形の例。`package:http`）。endpoint・token は `--dart-define-from-file=env.development.json` |
| `pointer` | ひな形に無い。payload に足す |

`Overlay` / `Navigator` の上に出すなら、`MaterialApp.builder` で `FeedbackRoot` を包む。

## 本番から外す

- **別 entrypoint**: `lib/main_dev.dart` だけが `flutter_feedback.dart` を import し、`lib/main.dart` は import しない（`flutter run -t lib/main_dev.dart`）。`kDebugMode` は保険。
- 送信先は開発用の define だけ（本番の define に入れない）。
- 確かめる: release の apk / ipa を `check-no-feedback-in-prod.sh`。AOT 後は識別子が潰れるので、`__feedback`・`x-feedback-token` などの文字列で見る。
