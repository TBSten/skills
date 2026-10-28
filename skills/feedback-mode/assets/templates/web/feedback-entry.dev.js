/**
 * feedback-entry.dev.js — 開発ビルドの入口に置く配線の例。【追加】
 *
 * 呼び出し側（アプリの main.ts / index.js）では、次の形で「開発ビルドのときだけ」読み込む。
 * バンドラが条件を静的に false と判断できる書き方にすること（本番ビルドで feedback-mode.js が丸ごと消える）:
 *
 *   // Vite / Rollup: import.meta.env.DEV は本番ビルドで false に置換される
 *   if (import.meta.env.DEV) { import('./feedback-entry.dev.js').then((m) => m.start()); }
 *   // webpack / esbuild: process.env.NODE_ENV !== 'production'（DefinePlugin / define で置換される）
 *   if (process.env.NODE_ENV !== 'production') { import('./feedback-entry.dev.js').then((m) => m.start()); }
 *
 * 守ること:
 *   - このファイルと feedback-mode.js は、上の条件の中の動的 import 以外から import しない（静的 import だと本番にも残る）
 *   - 送信先の URL・鍵は .env.development（コミットしない・VITE_ などのプレフィックスは本番ビルドの env に置かない）だけに書く
 *   - html2canvas などの依存は devDependencies にして、ここ（動的 import の先）でだけ読む
 *   - 本番ビルド後に scripts/check-no-feedback-in-prod.sh でバンドルを確認する
 */
export async function start() {
  const { installFeedbackMode, httpTransport } = await import('./feedback-mode.js');
  // const { default: html2canvas } = await import('html2canvas'); window.html2canvas = html2canvas; // スクショを撮るなら（devDependency）
  const endpoint = import.meta.env.VITE_FEEDBACK_ENDPOINT; // 例: http://127.0.0.1:7373/__feedback
  if (!endpoint) return console.warn('feedback: VITE_FEEDBACK_ENDPOINT が無いので無効');
  // ログはアプリのログ基盤から読む（別タスク。references/build/build.md 手順 1c）。基盤が無い間は省略（ログ節は空）か、暫定の log-buffer.example.js
  window.__feedback = installFeedbackMode({
    enabled: true,
    appName: document.title,
    pointer: 'ソースは <リポジトリ>/src/、ログの形は README の「ログ」', // Agent への案内（アプリごとに書き換える）
    getRecentLogs: () => [], // 例: () => appLogger.recent()。時刻・レベル・本文の配列（古い順）
    logEvent: (level, text) => {}, // 例: (l, t) => appLogger[l](t)（feedback 自身の動きをアプリのログに流す。省略可）
    transport: httpTransport(endpoint, import.meta.env.VITE_FEEDBACK_TOKEN ? { headers: { 'x-feedback-token': import.meta.env.VITE_FEEDBACK_TOKEN } } : {}),
    // getScreen / getView / stateOf: アプリの状態から作る（DOM の文字は拾わない。個人的な入力の本文は書かず字数だけ）
  });
}
