# web / dom（素の DOM・Vite・webpack・WebView の画面側）

検証状況: ヘッドレスのブラウザで「ホバー → クリック → 吹き出し → 送信 → 置き場に記録ができる・下の要素にクリックが届かない・Esc で出る・`enabled` を渡さないと何もしない」を確認。html2canvas でのスクショ・Vite の middleware への載せ替えは未検証。
ひな形: `assets/templates/adapters/web/dom/`（`feedback-mode.js` 本体・`feedback-entry.dev.js` 開発ビルドの入口・`log-buffer.example.js` 暫定のログ）。受け口は `core/feedback-dev-server.mjs`。
React・Electron・Tauri の画面側もこれを使う（`react.md`・`../desktop/`）。

## 差し込み口

`installFeedbackMode(opts)` の引数がそのまま差し込み口。

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | **既存の debug menu・dev panel があれば、その項目から返り値の `enter()` を呼ぶだけにし、`hotkey: () => false` で既定のホットキーを切る**（入口を増やさない。`../setup.md` §3）。基盤が無いときだけ既定のホットキー Alt+Shift+F（`e.code` で判定）か FAB を使う |
| `pick` / `block` | 組み込み（全面オーバーレイ + `elementsFromPoint` + window の capture）。iframe の中は選べない |
| `name` | `data-fb="画面/部品"` を付ける |
| `collect` | `getScreen`（既定: `location.pathname`）・`getView`（store の状態から）・`stateOf(el)`・`getEnv` |
| `logs` | `getRecentLogs`・`logEvent` |
| `capture` | 既定は `window.html2canvas` があれば使う（無ければ撮らない）。差し替えは `opts.capture`（例: `modern-screenshot`。作り方は `interaction.md` §5）。CSS の一部・クロスオリジン画像・canvas・video は崩れる |
| `transport` | `httpTransport(import.meta.env.VITE_FEEDBACK_ENDPOINT, { headers: { 'x-feedback-token': … } })`。受け口は別プロセスの `node core/feedback-dev-server.mjs`（`FEEDBACK_REPO_ROOT` を導入先に）か、dev サーバの middleware に `createHandler({ sink })` |
| `pointer` | `opts.pointer` |

`opts.enabled === true` でなければ何もしない（保険）。`transport` は必須で、既定の URL は持たない。

## 本番から外す

- 入口は `feedback-entry.dev.js` の形で、開発ビルドだけで評価される動的 import の中から読む。
  - Vite / Rollup: `if (import.meta.env.DEV) import('./feedback-entry.dev.js').then((m) => m.start())`
  - webpack / esbuild: `if (process.env.NODE_ENV !== 'production') …`
- **静的 import にしない**（本番のバンドルに残る）。スクショのライブラリは devDependencies にして、動的 import の先でだけ読む。
- 送信先は `.env.development` だけ（`.env.production` に書かない）。CSP の `connect-src` に開発機を入れない。
- 確かめる: `npm run build && bash "${CLAUDE_SKILL_DIR}/scripts/check-no-feedback-in-prod.sh" dist`、`npm ls --omit=dev html2canvas` が空。

## 既存のデバッグ基盤の例

dev toolbar（フレームワーク付属・内製）・Storybook の addon・`?debug=1`・`window.__DEV_TOOLS__` のような内製の口。

## この環境の注意

- 受け口が別オリジン（`127.0.0.1:7373`）なので、Origin は localhost 系だけ許している。
- 認証が要る画面・個人情報が写る画面では、送る前のプレビューや要素の伏せ字を検討する。
- popover・スマホ幅・複数選択・下書きなどの作り方は `interaction.md`、場所の手がかりは `locate.md`。
