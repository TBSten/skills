# mobile / ios（Swift・SwiftUI）

検証状況: 未検証。ひな形のファイルは無く、下の断片だけ。
先に `common.md` を読む。受け口は開発機の `core/feedback-dev-server.mjs` に submission（`POST /__feedback`、画像は base64）を送る。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | シェイク（`motionEnded`）・既存の debug 画面の項目 |
| `pick` / `name` | 全面のオーバーレイの `touchesEnded` で `window.hitTest` → `accessibilityIdentifier` のある最も近い祖先。SwiftUI は `.accessibilityIdentifier(_:)` を付ける |
| `block` | 全面の `UIView` のオーバーレイ（自分自身は `hitTest` で除く） |
| `capture` | `drawHierarchy`（オーバーレイを隠してから） |
| `transport` | `URLSession` で JSON を POST |

```swift
// スクリーンショット（オーバーレイを隠したあと）
func snapshot(_ window: UIWindow) -> Data? {
  let r = UIGraphicsImageRenderer(bounds: window.bounds)
  return r.image { _ in window.drawHierarchy(in: window.bounds, afterScreenUpdates: true) }.pngData()
}
// 部品の選択
let hit = window.hitTest(point, with: nil)
// ⭐️ 名前のある最も近い祖先までさかのぼる
var v: UIView? = hit; while v != nil && (v!.accessibilityIdentifier ?? "").isEmpty { v = v?.superview }
let rect = v?.convert(v!.bounds, to: nil)   // window 座標の矩形
```

## 本番から外す

- フィードバックのコードは `#if DEBUG … #endif`、または Debug configuration だけが含むファイル（Target Membership）。
- ATS の例外・`NSLocalNetworkUsageDescription`・`NSBonjourServices` は Debug 用の plist / xcconfig にだけ。
- 確かめる: Release の `.app` / `.ipa` を `check-no-feedback-in-prod.sh`。
