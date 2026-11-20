# web / astro（Astro の Dev Toolbar に載せる）

検証状況: Astro 7 + Starlight の実サイトで運用。最小の Astro 7 プロジェクトで「dev で toolbar から要素を選んで保存 → 置き場に schema 1 の記録・リンクは動かない・Esc で出る・幅 390px のタッチでシート（16px・44px）」「build の dist で検査が通り、統合を本番にも登録すると検査が落ちる」を確認。Astro 7 より前の toolbar API は未検証。
ひな形: `assets/templates/adapters/web/astro/`（`feedback-integration.ts` 統合 = 受け口・`feedback-app.ts` toolbar アプリ = 画面側・`protocol.ts` 両側で共有する名前・`check-no-dev-tools-in-dist.sh` dist の検査）。コアは `copy-core.sh` で同じフォルダの `core/` に置く（統合が `./core/` を import する）。

Dev Toolbar が「既存のデバッグ基盤」（`../setup.md` §3）になるので、入口・本番除外・画面とサーバの通り道を新しく作らない。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | 統合の `astro:config:setup` で `addDevToolbarApp({ id, name, icon, entrypoint })`。FAB・ホットキーは要らない |
| `pick` / `block` | `feedback-app.ts` に入っている（マウスはクリック、タッチは pointerup でタップを判定） |
| `name` | 無くてもよい（場所は XPath・セレクタ・ソースの行で指す。`locate.md`）。部品の名前が欲しければ `data-fb` |
| `collect` | `feedback-app.ts` の payload（`screen` に URL・viewport・dpr）。ページのソースファイルが分かれば統合で足す |
| `logs` | `injectScript('head-inline', …)` で入れるリングバッファ（下）。feedback 専用の暫定で、ログ基盤ではない |
| `capture` | ひな形には無い。足すなら `modern-screenshot` を toolbar アプリの中で動的 import（`interaction.md` §5） |
| `transport` | toolbar のメッセージ（HMR の WebSocket）。ブラウザ側 `server.send('feedback-mode:save')` → 統合 `toolbar.on(…)` → Sink → `toolbar.send('feedback-mode:result')`。`requestId` で対応づけ、約 10 秒で時間切れ。`server.on` の購読は外せないので `init` で 1 回だけ |
| `pointer` | 統合の options |

- 統合は受け口でもある: `submitFeedback` に `meta` を渡して schema 1 で保存する。依頼の形・コメントの字数・全体の大きさ・添付の数と大きさを `protocol.ts` の `LIMITS` で確かめる。
- 置き場は `.local/feedback/` のような git に入らない場所にし、`FEEDBACK_DIR` で差し替えられるようにする。検証では一時ディレクトリに向ける。

## 本番から外す

- 統合の `astro:config:setup` で `command !== 'dev'` なら何もしない。build・preview・sync には toolbar アプリも、ログのバッファも、Markdown のプラグインも出ない。
- CI で build の後に dist を検査する。dist に html が 1 つも無いときも落ちる（ビルドの失敗を「何も見つからない」で通さない）。パターン（toolbar アプリの id・ログの global 名・dev だけの属性名・依存名）は `protocol.ts` の定数と合わせる。

```yaml
- run: pnpm run build
- run: sh <dev 専用の場所>/check-no-dev-tools-in-dist.sh dist
```

## ログのバッファ（head-inline）

ページのどの script より前に入れ、toolbar アプリの `init` より前のエラー・hydration の失敗も拾う。中身は console の error / warn / log・未捕捉の例外・unhandledrejection・画像や script の読み込み失敗（capture の `error`）・Vite の `vite:error`（`/@vite/client` を動的 import して `createHotContext(...).on('vite:error')`）を、件数と字数の上限つきで溜めるだけ。バンドルを通らないので ES5 で書く。

## この環境の注意

- `astro dev --host` で LAN に出すと、同じネットワークの人が保存できる。起動時に警告を 1 行出す。
- 依存を動的 import する（スクショのライブラリなど）なら、`optimizeDeps.include` に先に入れる。入れないと初めて読んだ瞬間に Vite がページを再読み込みする。
- Markdown のソースの行（`data-fb-src`）を付けるなら `locate.md` §4（Astro 7 の Markdown は rehype ではなく Sätteri）。
- dev はソースの変更で全リロードされるので、E2E はリポジトリの写しで dev を立てる（`interaction.md` §13）。
- Esc の `keyup` で toolbar が閉じるので、こちらで扱った Esc の keyup は止める（`interaction.md` §9）。
