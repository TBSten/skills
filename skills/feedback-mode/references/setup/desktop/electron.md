# desktop / electron

検証状況: Electron 38 + Vite 7 の最小アプリ（Linux・xvfb・Playwright の `_electron`）で、ひな形をそのままコピーして「開発メニューの項目 → モードに入る → 部品をクリック（下のボタンにクリックが届かない）→ 書く → IPC → main が `capturePage` で撮って保存 → 置き場に記録と枠付きのスクショ」を確認。本番の renderer バンドル・本来の preload・main を asar にまとめ、展開して `check-no-feedback-in-prod.sh` が通ることも確認。electron-builder でのパッケージ・macOS は未確認。形そのもの（renderer のモード → IPC → main が保存）は、元にした社内アプリで数週間運用した。
ひな形: `assets/templates/adapters/desktop/electron/`。画面側は dom の `feedback-mode.js`。

| ファイル | 置き場所 | 役目 |
|---|---|---|
| `main-feedback.mjs` | `<dev>/feedback/`（隣に `core/`） | main の IPC の受け口。撮影と保存 |
| `electron-dev-menu.mjs` | 同上 | 既存の開発メニューに 1 項目足す |
| `preload-feedback.cjs` | 同上 | 開発時だけの preload。本来の preload を読んでから `feedbackApi` を足す |
| `renderer-feedback.js` | renderer の dev ディレクトリ（隣に `feedback-mode.js`） | renderer の入口 |

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 既存の「開発」「Debug」メニューに `feedbackMenuItem()` を 1 項目足す（無ければメニューを作る）。main → `app:menu-feedback` → preload の `onMenu` → renderer の `enter('menu')`。renderer のホットキーは切る |
| `pick` / `block` / `name` | dom と同じ。モーダルの `<dialog>` を開いているときは、モードの DOM をそのダイアログの中に置く（対応済み） |
| `collect` | renderer の `getScreen` / `getView`（モデルから）/ `stateOf`。main からは版・パス・ウィンドウを足せる |
| `logs` | main に集約したログのバッファを IPC で renderer に渡して `getRecentLogs` で返す（ログ基盤の変更は別タスク） |
| `capture` | renderer は `capture: async () => ({})`。main が `webContents.capturePage()` で撮る。撮る間は吹き出し・帯を隠し、選んだ要素を `.fb-frame` で囲む |
| `transport` | `ipcTransport(window.feedbackApi.save)` → main の `registerFeedback({ ipcMain, getWindow, sink, isDev })`（チャネル `app:feedback-save`） |
| `pointer` | renderer の `opts.pointer` |

```js
// main（ESM）
const isDev = !app.isPackaged;
// ⭐️ preload を本番と開発で別ファイルにする
mainWindow = new BrowserWindow({ webPreferences: { preload: path.join(__dirname, isDev ? 'dev/feedback/preload-feedback.cjs' : 'preload.cjs'), contextIsolation: true, sandbox: false } });
if (isDev) {
  const { registerFeedback } = await import('./dev/feedback/main-feedback.mjs');
  const { createFileSink } = await import('./dev/feedback/core/file-sink.mjs');
  registerFeedback({ ipcMain, getWindow: () => mainWindow, isDev: true, sink: createFileSink({ repoRoot: <リポジトリのルート>, dir: 'feedback' }) });
  const { feedbackMenuItem } = await import('./dev/feedback/electron-dev-menu.mjs');
  devSubmenu.push(feedbackMenuItem(() => mainWindow.webContents.send('app:menu-feedback', { action: 'mode', on: true })));
}
```

```js
// renderer の入口（バンドラで組む）
if (import.meta.env.DEV) import('./dev/renderer-feedback.js').then((m) => m.start());
```

## 本番から外す

- main: `app.isPackaged` が false のときだけ `await import()` で読む。`registerFeedback` も `isDev !== true` なら何もしない（保険）。
- preload: **本来の preload に `if (isDev) exposeInMainWorld('feedbackApi', …)` と書かない**（本番の成果物に文字列が残り、検査に落ちる。確かめた）。開発時だけ `preload-feedback.cjs` を使う。
- renderer: バンドラの DCE + 動的 import（dom と同じ）。バンドルしない renderer では import の行が残るので、この形は使えない。
- パッケージ: `<dev>/feedback/**`（`core/` を含む）をパッケージの `files` から外す。
- 確かめる: `npx @electron/asar extract <app.asar> <dir> && bash "${CLAUDE_SKILL_DIR}/scripts/check-no-feedback-in-prod.sh" <dir>`。

## この環境の注意

- **ヘッドレスで動かせるようにする**（`show: false` を環境変数で切り替える）。Linux のコンテナでは `xvfb-run` と `--no-sandbox`（root のとき）。スモークは Playwright の `_electron.launch` で起動し、メニューは `app.evaluate(({ Menu }) => …click())`、部品は実際のマウス操作で選ぶ。
- `preload-feedback.cjs` は `require` で本来の preload を読むので、そのウィンドウは `sandbox: false` にする。
- 開発起動では **main が古いまま renderer だけ新しくなる**ことがある。新しい IPC や設定キーを main が知らないエラーになり、再起動で直る。調べてほしいという記録の多くがこれ（`references/loop/lessons.md`）。
