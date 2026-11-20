# web / react（React + Vite / Next.js など）

検証状況: 本体は dom と同じ。React での配線（下の `main.tsx` の書き方）は未検証。
ひな形: dom のもの（`assets/templates/adapters/web/dom/`）をそのまま使う。React 専用のコードは無い。

DOM の上で動くので、React のコンポーネントにせず、**アプリの外側から一度だけ入れる**。差し込み口の意味は `dom.md` と同じで、React で変わるのは次だけ。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | `main.tsx`（Next.js は client だけで動く最上位の `useEffect`）で、開発ビルドのときだけ動的 import する（下の例） |
| `name` | JSX に `data-fb="画面/部品"` を付ける（`<button data-fb="ヘッダ/保存">`）。共通コンポーネントは props で受けて最上位の要素に渡す |
| `collect` | `getView`: store（Redux・Zustand・TanStack Query のキャッシュ）の状態を返す。`getScreen`: ルーターの現在地（`location.pathname` で足りることが多い） |
| `logs` | 既存の logger があれば読む。無ければ省略 |
| `transport` | dom と同じ `httpTransport`（Vite は `import.meta.env.VITE_FEEDBACK_ENDPOINT`、Next.js は `process.env.NEXT_PUBLIC_FEEDBACK_ENDPOINT`） |

```tsx
// main.tsx（Vite）
createRoot(document.getElementById('root')!).render(<App />)
// ⭐️ 開発ビルドだけで評価される動的 import にする
if (import.meta.env.DEV) import('./dev/feedback/feedback-entry.dev.js').then((m) => m.start())
```

```tsx
// Next.js（app router）: client component を最上位の layout に 1 つ置く
'use client'
useEffect(() => {
  // ⭐️ NODE_ENV は本番ビルドで定数になり、分岐ごと消える
  if (process.env.NODE_ENV !== 'production') import('../dev/feedback/feedback-entry.dev.js').then((m) => m.start())
}, [])
```

Next.js では `import.meta.env.VITE_*` が無い。コピーした `feedback-entry.dev.js` の `start()` で、送信先と鍵の 2 か所を `process.env.NEXT_PUBLIC_FEEDBACK_ENDPOINT` と `process.env.NEXT_PUBLIC_FEEDBACK_TOKEN` に置き換える（どちらも `.env.development.local` にだけ書く）。

## 本番から外す

dom と同じ（静的 import にしない・依存は devDependencies・送信先は開発用の env だけ）。確かめる: Vite は `dist`、Next.js は `.next/static` を `check-no-feedback-in-prod.sh` で検査する。

## この環境の注意

- StrictMode の開発時は effect が 2 回走る。`feedback-entry.dev.js` の `start()` は、`window.__feedback` があれば何もしない。
- React が DOM を作り直しても、モードは毎回 `elementsFromPoint` で選ぶので影響しない。下書きの鍵は「`data-fb` の道筋 + CSS セレクタ」なので、再描画で構造が変わると戻らないことがある。
