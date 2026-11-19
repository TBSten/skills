# warehouse-web

倉庫スタッフ向けの入出庫管理 Web アプリ (Vite + TypeScript、フレームワークなし)。

- 開発: `npm run dev` / 本番ビルド: `npm run build` (成果物は `dist/`)
- テスト: `npm test`
- 開発中だけ右下に DevPanel (API モック切り替え・feature flag) が出る。本番ビルドには入らない
- ログ: `src/lib/logger.ts` (loglevel + 直近 500 件のリングバッファ)
