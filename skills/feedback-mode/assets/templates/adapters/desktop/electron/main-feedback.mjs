/**
 * Electron main 側の配線（参照実装 src/main/main.ts の buildFeedback / saveFeedback / IPC に相当。ESM の最小版）。
 *
 * 参照実装との違い:
 *   - 置き場は Sink として差し替えられる（既定は file-sink。参照実装は feedbackFile.ts を直接呼んでいた）
 *   - 画面の状態・環境・ログは renderer が payload に入れて送る（参照実装は main が env・ログを持つ）。main のログも載せたいなら payload.logs に足す
 *   - **debug ビルド限定**（参照実装には無い）。registerFeedback は isDev が true のときだけ IPC を登録する。
 *     isDev には `!app.isPackaged` を渡す。さらに、本番のビルド（electron-builder の files）からこのファイルと core/ を除く、
 *     preload は本番と開発で別ファイルにする（preload-feedback.cjs）、までやる（references/setup/desktop/electron.md）。
 *
 * 使い方（main.js から。本番ビルドで消えるよう、条件つきの動的 import にする）:
 *   if (!app.isPackaged) {
 *     const { registerFeedback } = await import('./dev/feedback/main-feedback.mjs');
 *     const { createFileSink } = await import('./dev/feedback/core/file-sink.mjs');
 *     registerFeedback({ ipcMain, getWindow: () => mainWindow, isDev: true, sink: createFileSink({ repoRoot: path.resolve(__dirname, '..'), dir: 'feedback' }) });
 *   }
 */
import { cropRect, submitFeedback } from './core/feedback-core.mjs';

export function registerFeedback({ ipcMain, getWindow, sink, isDev }) {
  if (isDev !== true) return; // 本番ビルドでは登録しない
  ipcMain.handle('app:feedback-save', async (_event, body) => {
    try {
      if (!body || typeof body.payload !== 'object' || typeof body.payload.comment !== 'string' || body.payload.comment.length > 20_000) throw new Error('不正な指定です');
      if (JSON.stringify(body).length > 500_000) throw new Error('フィードバックの情報が大きすぎます'); // 参照実装の上限
      let windowPng = null;
      let elementPng = null;
      let shotError = null;
      const win = getWindow();
      if (body.options?.shots !== false && win && !win.isDestroyed()) {
        // 参照実装: webContents.capturePage()。吹き出し・帯は renderer が隠して枠を描いてから呼ばれる
        const image = await win.webContents.capturePage();
        if (image.isEmpty()) shotError = 'スクリーンショットが空でした';
        else {
          windowPng = new Uint8Array(image.toPNG());
          const rect = cropRect(body.payload.target.rect, body.payload.viewport ?? { width: 0, height: 0 }, image.getSize());
          elementPng = rect ? new Uint8Array(image.crop(rect).toPNG()) : null;
        }
      }
      const { text: _omit, ...res } = await submitFeedback({ sink, payload: body.payload, options: body.options, windowPng, elementPng, shotError });
      return res;
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : String(err) };
    }
  });
}

// preload は preload-feedback.cjs、renderer の入口は renderer-feedback.js、メニューは electron-dev-menu.mjs。
