/**
 * log-buffer.example.js — 暫定のログ置き場（console・error・click を溜めるだけ）。【追加】【未検証】
 *
 * **これは feedback mode の一部ではない**。アプリにログ基盤が無く、とりあえず feedback に添付するログが要るときの暫定。
 * 本来のログ（収集・形式・保存・ローテーション・圧縮・redact・本番での扱い）は別タスク・別 plan で作る（references/build/build.md 手順 1c）。
 * 使うなら feedback-mode.js とは別に、開発ビルドの入口で:
 *   const logs = installLogBuffer();
 *   installFeedbackMode({ …, getRecentLogs: logs.entries, logEvent: logs.push });
 * 参照実装のログ（main の log.ts に 1 行 1 件・key=value・actionId・バッファ 3000 件）とは別物で、形式・redact・圧縮は持たない（redact は文の組み立てが最後に掛ける）。
 */
export function installLogBuffer({ max = 3000 } = {}) {
  const entries = [];
  const push = (level, text) => {
    entries.push({ ts: new Date().toISOString().slice(11, 23), level, text: String(text).slice(0, 2000) });
    if (entries.length > max) entries.shift();
  };
  for (const level of ['debug', 'info', 'log', 'warn', 'error']) {
    const orig = console[level].bind(console);
    console[level] = (...args) => {
      push(level === 'log' ? 'info' : level, args.map((a) => (typeof a === 'string' ? a : safeJson(a))).join(' '));
      orig(...args);
    };
  }
  window.addEventListener('error', (e) => push('error', `window.onerror ${e.message} at ${e.filename}:${e.lineno}`));
  window.addEventListener('unhandledrejection', (e) => push('error', `unhandledrejection ${e.reason?.message ?? e.reason}`));
  // 画面の操作（参照実装の ui.click）。入力の中身は書かない
  window.addEventListener('click', (e) => { if (e.target instanceof Element) push('info', `ui.click target=${e.target.tagName.toLowerCase()}${e.target.id ? `#${e.target.id}` : ''}`); }, { capture: true });
  window.addEventListener('hashchange', () => push('info', `ui.screen hash=${location.hash}`));
  return { entries: () => entries.slice(), push };
}

function safeJson(v) {
  try {
    return JSON.stringify(v);
  } catch {
    return String(v);
  }
}

