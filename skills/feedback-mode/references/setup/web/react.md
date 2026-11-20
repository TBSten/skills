# web / react（React + Vite）

検証状況: React 19 + Vite 7 の最小アプリ（StrictMode）に下の書き方で入れ、ヘッドレスの Chromium で「ホットキーで入る → 部品をクリック（下のボタンにクリックが届かない）→ 送信 → 置き場に記録・html2canvas のスクショ（枠付き）→ Esc で出る」と、`vite build` の `dist` で検査が通り `npm ls --omit=dev html2canvas` が空なことを確認。Next.js などほかの React の構成は未確認なので載せていない。
ひな形: dom のもの（`assets/templates/adapters/web/dom/`）をそのまま使う。React 専用のコードは無い。

DOM の上で動くので、React のコンポーネントにせず、**アプリの外側から一度だけ入れる**。差し込み口の意味は `dom.md` と同じで、React で変わるのは次だけ。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | `main.tsx` で、開発ビルドのときだけ動的 import する（下の例） |
| `name` | JSX に `data-fb="画面/部品"` を付ける（`<button data-fb="ヘッダ/保存">`）。共通コンポーネントは props で受けて最上位の要素に渡す |
| `collect` | `getView`: store（Redux・Zustand・TanStack Query のキャッシュ）の状態を返す。`getScreen`: ルーターの現在地（`location.pathname` で足りることが多い） |
| `logs` | 既存の logger があれば読む。無ければ省略 |
| `transport` | dom と同じ `httpTransport`（`import.meta.env.VITE_FEEDBACK_ENDPOINT`） |

```tsx
// main.tsx（Vite）
createRoot(document.getElementById('root')!).render(<App />)
// ⭐️ 開発ビルドだけで評価される動的 import にする
if (import.meta.env.DEV) import('./dev/feedback/feedback-entry.dev.js').then((m) => m.start())
```

## 本番から外す

dom と同じ（静的 import にしない・依存は devDependencies・送信先は開発用の env だけ）。確かめる: `vite build` の `dist` を `check-no-feedback-in-prod.sh` で検査する。

## この環境の注意

- StrictMode でも `start()` は 1 回だけ入れる（`window.__feedback` の印。確認済み）。
- html2canvas を足した直後の初回は、Vite が依存を最適化してページを読み直す。そのあいだの操作は消えるので、スモークは読み直しのあとに始める。
- React が DOM を作り直しても、モードは毎回 `elementsFromPoint` で選ぶので影響しない。下書きの鍵は「`data-fb` の道筋 + CSS セレクタ」なので、再描画で構造が変わると戻らないことがある。
