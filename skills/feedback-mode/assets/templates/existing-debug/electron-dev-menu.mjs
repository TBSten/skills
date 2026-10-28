/**
 * 既存の Electron アプリメニューの「開発」項目に「フィードバックモードへ」を足すだけの最小例。【追加】【未検証: 実アプリで未検証】
 * 参照実装（main のメニュー定義）は、メニューの一番右に「フィードバック」を作り、⌥⌘F を割り当てている【実績】。
 * 既に「開発」「Debug」メニューがあるアプリでは、新しいトップレベルのメニューを作らず、その中に 1 項目足す。本番除外は基盤の条件に乗る
 * （例: 既存の開発メニューが `!app.isPackaged` のときだけ組み立てられているなら、その配列に push するだけ）。
 *
 * 使い方（既存のメニュー組み立てコードの中で。`devSubmenu` は既存の開発メニューの submenu 配列）:
 *   if (!app.isPackaged) devSubmenu.push(feedbackMenuItem(() => mainWindow.webContents.send('app:menu-feedback', { action: 'mode', on: true })));
 * 受け側（renderer）は feedback-entry.dev.js で `fb.enter('menu')` を呼ぶ（preload で 'app:menu-feedback' を公開する。main-feedback.mjs の注記を参照）。
 */
export function feedbackMenuItem(send) {
  // accelerator は既存のショートカットと衝突しないものに（参照実装は Alt+Command+F）。既存の規約（文言・並び）に合わせる
  return { label: 'フィードバックモードへ', accelerator: 'Alt+Command+F', click: send };
}
