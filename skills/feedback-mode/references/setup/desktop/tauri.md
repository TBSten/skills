# desktop / tauri（Rust + WebView）

検証状況: 未検証（手順だけ）。
ひな形: 画面側は dom の `feedback-mode.js`。Rust 側のコードは無い。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | `tauri::menu` のイベントで `emit` し、renderer が `fb.enter('menu')`。またはホットキー（dom の既定） |
| `pick` / `block` / `name` / `collect` | dom と同じ |
| `logs` | `tauri-plugin-log` などのファイルの末尾、または Rust 側のバッファを `invoke` で読む |
| `capture` | 標準のウィンドウ画像の API が無い。(a) html2canvas（WebView の描画と差が出る）(b) Rust の `xcap` などでウィンドウを撮る (c) macOS の `screencapture` を呼ぶ。dpr と `cropRect` の扱いは同じ |
| `transport` | (a) `invoke('feedback_save', body)` を `ipcTransport` に渡し、Rust のコマンドで保存する。`references/spec/data.md` §3.1 の規則（`yymmdd-xx`・`OpenOptions::new().create_new(true)`・`archived/` も数える・ルート外とシンボリックリンクの拒否・redact）を移植する。(b) 移植が面倒なら、開発中だけ `core/feedback-dev-server.mjs` を sidecar で起動し、`httpTransport` で送る |
| `pointer` | dom と同じ |

## 本番から外す

- Rust 側は `#[cfg(debug_assertions)]` か Cargo feature（`--features feedback` を dev だけ）で `invoke_handler` に登録する。
- 開発機への接続（`connect-src` の `127.0.0.1:7373`）は開発用の設定（`tauri.dev.conf.json` など）にだけ足す。
- フロントは dom と同じ。
- 確かめる: release のバンドルを `check-no-feedback-in-prod.sh`。

## この環境の注意

- (a) の Rust 移植をしたら、`core/sink-contract.test.mjs` と同じ内容のテストを Rust で書く。
