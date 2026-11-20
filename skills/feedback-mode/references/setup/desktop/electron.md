# desktop / electron

検証状況: この形（renderer のモード → IPC → main が組み立てて保存）は、元にした社内向けの Electron アプリで数週間運用した。ひな形の `main-feedback.mjs` と `electron-dev-menu.mjs` の配線そのものは未検証。
ひな形: `assets/templates/adapters/desktop/electron/`（`main-feedback.mjs` main の IPC = 受け口・`electron-dev-menu.mjs` 既存の開発用メニューに 1 項目足す例）。画面側は dom の `feedback-mode.js`。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | アプリメニューの項目 + アクセラレータ（例: ⌥⌘F）。クリックを IPC で renderer に伝え、renderer が `fb.enter('menu')`。既存の「開発」「Debug」メニューがあればそこに足す |
| `pick` / `block` / `name` | dom と同じ（`feedback-mode.js`）。モーダルの `<dialog>` を開いているときは、モードの DOM をそのダイアログの中に置く（対応済み） |
| `collect` | renderer の `getScreen` / `getView`（モデルから）/ `stateOf`。main からは版・パス・ウィンドウを `getEnv` 相当で足せる |
| `logs` | main に集約したログ（`electron-log` など）のバッファを読む |
| `capture` | renderer は `capture: async () => ({})`（撮らない）。main が `webContents.capturePage()` で撮る。macOS では dpr 倍の画像になる（`cropRect` が dpr を掛ける） |
| `transport` | `ipcTransport(window.feedbackApi.save)`。preload の `contextBridge.exposeInMainWorld('feedbackApi', { save })` → main の `registerFeedback({ ipcMain, getWindow, sink, isDev })`（チャネル `app:feedback-save`） |
| `pointer` | renderer の `opts.pointer` |

## 本番から外す

- `app.isPackaged` が false のときだけ `await import()` で `registerFeedback` を読む（`isDev`）。
- preload の `exposeInMainWorld('feedbackApi', …)`・メニュー項目・アクセラレータも開発時だけ。
- electron-builder の `files` から `dev/feedback/**` を外す（dev 専用のディレクトリに置くと外しやすい）。
- renderer は dom と同じ（DCE + 動的 import）。
- 確かめる: パッケージの `app.asar` を `npx asar extract` で展開してから `check-no-feedback-in-prod.sh`。

## この環境の注意

- **ヘッドレスで動かせるようにする**（`HEADLESS` などの環境変数・クリップボードに書かない・`show: false`）。スモークは実入力（デバッガの `Input.dispatchMouseEvent`）で送り、ルートが一時ディレクトリのときだけ保存を押す。
- 開発起動では **main が古いまま renderer だけ新しくなる**ことがある。新しい IPC や設定キーを main が知らないエラーになり、再起動で直る。調べてほしいという記録の多くがこれ（`references/loop/lessons.md`）。
