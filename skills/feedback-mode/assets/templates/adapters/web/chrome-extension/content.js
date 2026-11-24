// content.js — ページに入れて、フィードバックモードを入れる / 出す。ボタンを押すたびに呼ばれる（2 回目からは入る・出るの切り替え）。
// ここ（CONFIG）を自由に書き換えてよい。vendor/ の中は scripts/chrome-extension.mjs sync で skill の版に上書きされるので書き換えない。
(async () => {
  // ===== ここから書き換えてよい =====
  const CONFIG = {
    /** 記録の見出しに出る名前 */
    appName: () => document.title || location.host,
    /** 読む Agent への案内（ソースの場所・正本 docs）。記録の冒頭に入る */
    pointer: '',
    /** 画面の名前（既定は URL） */
    getScreen: () => ({ id: location.href, title: document.title }),
    /** 節の初期値（target・log・shots・env・screen・view）。拡張はスクショを撮らない・ページのログを読めないので切っておく */
    defaultOptions: { log: false, shots: false, screen: true },
  };
  // ===== ここまで =====

  const self = globalThis.__feedbackExtension;
  if (self?.mode) return self.mode.active() ? self.mode.exit() : self.mode.enter();
  if (self?.loading) return; // 読み込み中にもう一度押された
  globalThis.__feedbackExtension = { loading: true };
  try {
    const [{ installFeedbackMode }, core] = await Promise.all([
      import(chrome.runtime.getURL('vendor/feedback-mode.js')),
      import(chrome.runtime.getURL('vendor/feedback-core.mjs')),
    ]);
    const mode = installFeedbackMode({
      enabled: true,
      appName: CONFIG.appName(),
      pointer: CONFIG.pointer,
      getScreen: CONFIG.getScreen,
      defaultOptions: CONFIG.defaultOptions,
      hotkey: () => false, // 入口は拡張のボタンと Alt+Shift+F（拡張のコマンド）だけ
      capture: async () => ({ error: 'Chrome 拡張ではスクリーンショットを撮っていません' }),
      transport: copyTransport(core),
    });
    globalThis.__feedbackExtension = { mode };
    mode.enter();
  } catch (e) {
    delete globalThis.__feedbackExtension; // 次に押したときにやり直せるように
    console.error('feedback-mode: 入れられませんでした', e);
  }

  /** 記録の文を組み立ててクリップボードに書く（置き場・受け口は無い。人がチャットに貼る） */
  function copyTransport({ buildFeedbackMarkdown, redact, normalizeOptions }) {
    return async ({ payload, options, shotError }) => {
      const shots = { dir: null, window: null, element: null, error: shotError ?? 'スクリーンショットは撮っていません' };
      const text = redact(buildFeedbackMarkdown({ ...payload, shots }, normalizeOptions(options), { meta: { created: new Date() } }));
      try {
        await navigator.clipboard.writeText(text);
      } catch {
        // フォーカスが無い・ページの権限ポリシーで止められたときの代わり
        const ta = Object.assign(document.createElement('textarea'), { value: text });
        ta.style.cssText = 'position:fixed;left:-9999px;top:0;';
        document.body.append(ta);
        ta.select();
        const ok = document.execCommand('copy');
        ta.remove();
        if (!ok) return { ok: false, error: 'クリップボードに書けませんでした' };
      }
      return { ok: true, message: 'クリップボードにコピーしました。チャットに貼ってください' };
    };
  }
})();
