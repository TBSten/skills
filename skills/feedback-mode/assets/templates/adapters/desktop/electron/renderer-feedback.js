/**
 * renderer-feedback.js — Electron の renderer で、dom の feedback-mode.js を IPC につなぐ入口（feedback-entry.dev.js の Electron 版）。
 * renderer はバンドラ（Vite・webpack・electron-vite）で組み、開発ビルドだけで評価される動的 import から読む:
 *   if (import.meta.env.DEV) import('./dev/renderer-feedback.js').then((m) => m.start());
 * バンドルしない renderer（<script> で素のファイルを読む）だと、この import が本番の成果物に残る。
 */
export async function start() {
  if (window.__feedback || !window.feedbackApi) return; // preload-feedback.cjs が読まれていない（本番の preload）なら何もしない
  window.__feedback = { pending: true };
  try {
    const { installFeedbackMode, ipcTransport } = await import('./feedback-mode.js');
    window.__feedback = installFeedbackMode({
      enabled: true,
      appName: document.title,
      hotkey: () => false, // 入口は main のメニュー（electron-dev-menu.mjs）。renderer のホットキーは切る
      transport: ipcTransport(window.feedbackApi.save),
      capture: async () => ({}), // スクショは main が webContents.capturePage() で撮る（main-feedback.mjs）
      // getRecentLogs: () => …,  // main のログ基盤から読むなら IPC で渡す（ログ基盤の変更は別タスク）
    });
    window.feedbackApi.onMenu(() => window.__feedback.enter('menu'));
  } catch (e) {
    delete window.__feedback;
    throw e;
  }
}
