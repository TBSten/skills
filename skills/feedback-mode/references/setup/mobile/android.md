# mobile / android（Kotlin・Jetpack Compose）

検証状況: 未検証。ひな形のファイルは無く、下の断片だけ。
先に `common.md` を読む。受け口は開発機の `core/feedback-dev-server.mjs` に submission（`POST /__feedback`、画像は base64）を送る。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | debug drawer・通知・`src/debug/` の Activity・シェイク（`SensorManager`） |
| `pick` / `name` | View はルートを再帰し、タップ座標を含む最も深い View。名前は `resources.getResourceEntryName(id)` か `contentDescription`。Compose は `Modifier.testTag("…")` / `semantics` を付け、`SemanticsNode` の境界から選ぶ |
| `block` | 全面のオーバーレイ View（`dispatchTouchEvent` で受ける） |
| `capture` | `PixelCopy`（API 26+。それ以前は `View.draw`） |
| `transport` | OkHttp などで JSON を POST |

```kotlin
// スクリーンショット（オーバーレイを隠したあと）
val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
// ⭐️ PixelCopy は SurfaceView を含めて正確に撮れる
PixelCopy.request(window, bmp, { /* 完了後に PNG へ */ }, Handler(Looper.getMainLooper()))
```

## 本番から外す

- `src/debug/` の source set に置き、`src/main/` からは参照しない（release は空のスタブを `src/release/` に置く、または `debugImplementation` + ServiceLoader）。`BuildConfig.DEBUG` は保険。
- `usesCleartextTraffic`・`network_security_config`（開発機のドメイン）・追加の権限は `src/debug/AndroidManifest.xml` だけ。依存は `debugImplementation`。
- 確かめる: `assembleRelease` の apk を `check-no-feedback-in-prod.sh`、`aapt2 dump permissions`。
