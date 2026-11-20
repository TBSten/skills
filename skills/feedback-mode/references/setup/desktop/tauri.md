# desktop / tauri

検証状況: Tauri 2 + Vite 7 の最小アプリ（`create-tauri-app` の vanilla を Vite に載せ替え。Linux・WebKitGTK 4.1・xvfb）で、dom のひな形（`feedback-mode.js`・`feedback-entry.dev.js`）をそのまま使い、tauri-driver（WebDriver）の実入力で「ホットキーで入る → 部品をクリック（下のボタンにクリックが届かない）→ 書く → HTTP で受け口へ → 置き場に記録」を確認。`tauri build` の `dist` で検査が通ることも確認。Rust の IPC（`invoke`）で受ける形・スクショ・macOS / Windows は未確認なので載せていない。
ひな形: dom のもの（`assets/templates/adapters/web/dom/`）をそのまま使う。Tauri 専用のコードは無い。

画面は WebView の DOM なので、dom と同じものを入れる。受け口は Rust に移植せず、開発時だけ別プロセスで `core/feedback-dev-server.mjs` を動かし、WebView から HTTP で送る。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | フロントエンドの入口で、開発ビルドだけ動的 import（dom と同じ）。既存の開発メニュー・dev panel があればそこから `enter()` |
| `transport` | `httpTransport(import.meta.env.VITE_FEEDBACK_ENDPOINT)`。開発時の WebView は `devUrl`（`http://localhost:1420` など）から読まれるので、受け口の Origin の確認（localhost 系だけ許す）を通る |
| `capture` | 未確認（html2canvas は WebKitGTK では試していない）。撮らないなら記録に「撮っていません」と出る |
| ほか | dom と同じ |

```json
// src-tauri/tauri.conf.json（フロントエンドを Vite で組む）
"build": { "devUrl": "http://localhost:1420", "frontendDist": "../dist", "beforeBuildCommand": "npx vite build" }
```

## 本番から外す

- フロントエンドは dom と同じ（`import.meta.env.DEV` + 動的 import。静的 import にしない）。`tauri build` は `vite build` の結果を埋め込むので、`dist` に残らなければバイナリにも残らない。
- Rust 側には何も足さない（コマンド・権限・capabilities を増やさない）。
- `app.security.csp` を設定しているアプリは、開発時だけ `connect-src` に `http://127.0.0.1:7373` を足す（本番の設定に書かない）。
- 確かめる: `npx tauri build --no-bundle` のあと `bash "${CLAUDE_SKILL_DIR}/scripts/check-no-feedback-in-prod.sh" dist`。release のバイナリはフロントエンドを圧縮して埋め込むので、バイナリへの grep では見つけられない。`dist` を検査する。

## この環境の注意

- ヘッドレスの確認は tauri-driver（`cargo install tauri-driver --locked`）と WebKitWebDriver（Linux は `webkit2gtk-driver`）を `xvfb-run` の中で動かし、WebDriver の actions で実際にキーとマウスを送る。Playwright は Tauri の WebView を操作できない。
- Linux でビルドするには `libwebkit2gtk-4.1-dev` が要る。
