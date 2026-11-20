/**
 * preload-feedback.cjs — 開発時だけ使う preload。アプリ本来の preload を読んでから、フィードバックの IPC を足す。
 * main で `preload: isDev ? '<dev>/preload-feedback.cjs' : '<アプリの preload>'` と切り替える（本番の preload にはフィードバックの文字列が 1 つも入らない）。
 * 本来の preload の中で `if (isDev) exposeInMainWorld('feedbackApi', …)` と書くと、本番の成果物に文字列が残り検査に落ちる（実際に確かめた）。
 * sandbox: true の preload では require できるモジュールが限られるので、このファイルを使う開発時のウィンドウだけ sandbox: false にするか（sandbox: !isDev）、本来の preload に中身を写す。
 */
require('../../preload.cjs'); // ← アプリ本来の preload へのパスに変える
const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('feedbackApi', {
  save: (body) => ipcRenderer.invoke('app:feedback-save', body),          // main-feedback.mjs の registerFeedback が受ける
  onMenu: (fn) => ipcRenderer.on('app:menu-feedback', (_e, msg) => fn(msg)), // electron-dev-menu.mjs の項目から
});
