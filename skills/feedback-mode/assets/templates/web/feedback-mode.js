/**
 * feedback-mode.js — Web / WebView（Electron・Tauri・Capacitor の画面側を含む）用の「フィードバックモード」最小実装。依存なし・ESM。
 *
 * 参照実装（renderer の feedback.ts・elementInfo.ts）の骨格を、アプリ固有の部分（モデルから作る画面の状態・
 * 初心者/中級者モード・ダイアログ内への配置・data-fb の大量付与）を除いて写したもの。
 *   - 全面の透明オーバーレイでポインタを受ける → document.elementsFromPoint で下の要素を探す（下の要素にクリックは届かない）
 *   - window の capture で click・キー・フォーカスの移動も止める（吹き出し・帯の中だけ通す）
 *   - 選ぶ単位: 操作できる要素はそのもの、ほかは data-fb のある最も近い祖先（無ければその要素）
 *   - 吹き出し: テキストエリア・送信・親を選ぶ・選び直す（文は次の要素に持っていく）・閉じる（モードを終える。文は下書きに残る）
 *   - 送るときは吹き出し・帯・強調を隠し、選んだ要素を枠で囲んでからスクリーンショットを撮る
 *   - 成功したらモードを終える。失敗したら吹き出しに理由を出して続ける
 * 【汎用化で追加】: 送信先を差し替え可能にした点（Transport。参照実装は IPC → main がファイルを書く）。ログは作らず、差し込み口（getRecentLogs）から読んで添付するだけ。
 *
 * 【汎用化で追加】debug ビルド限定の前提（参照実装には無い仕組み）:
 *   このファイルは本番バンドルに入れない。呼び出しは「開発ビルドだけで評価される動的 import」の中に書く（feedback-entry.dev.js の例）。
 *   二重の保険として、installFeedbackMode は opts.enabled === true でなければ何もしない（既定は無効）。
 *   送信先（endpoint・token）は既定値を持たず、開発用のビルド変数から渡す（本番に URL・鍵を混ぜない）。
 *
 * 送信の差し替え（保存先の抽象化）:
 *   画面側の差し替え点は transport（画面 → 受け口）。受け口（feedback-dev-server.mjs / Electron main）の裏で Sink（ファイル・Issue 等）を差し替える。
 *   httpTransport(endpoint, {headers})  既定。LAN の開発機の受け口へ HTTP POST（スマホ実機・別マシン）
 *   ipcTransport(fn)                    Electron / Tauri: IPC で main（Rust）に渡す（参照実装と同じ経路）
 *   自作: async (body) => ({ ok, id, ref, error })  例: 共有シートに Markdown を渡す・Issue トラッカーの API を直接呼ぶ（要トークンの扱い注意）
 *
 * 使い方（開発ビルドの入口だけで）:
 *   const { installFeedbackMode, httpTransport } = await import('./feedback-mode.js');
 *   installFeedbackMode({ enabled: true, appName: 'MyApp', transport: httpTransport(import.meta.env.VITE_FEEDBACK_ENDPOINT, { headers: { 'x-feedback-token': import.meta.env.VITE_FEEDBACK_TOKEN } }),
 *                         getRecentLogs: () => appLogger.recent(), // アプリのログ基盤から読む（無ければ省略 = ログ節は空）
 *                         getScreen: () => ({ id: location.pathname }), getView: () => store.getState() });
 *   // ホットキー Alt+Shift+F で入る / fb.enter()
 */

/** 画面 → 受け口（HTTP）。body は { payload, options, windowPng, elementPng, shotError }（assets/templates/feedback.schema.json）。結果は { ok, id, ref, error } */
export function httpTransport(endpoint, { headers = {} } = {}) {
  return async (body) => {
    const res = await fetch(endpoint, { method: 'POST', headers: { 'content-type': 'application/json', ...headers }, body: JSON.stringify(body) });
    const j = await res.json().catch(() => ({}));
    return res.ok ? j : { ok: false, error: j.error ?? `HTTP ${res.status}` };
  };
}

/** 画面 → 受け口（IPC。Electron の preload が公開した関数など）。そのまま渡せばよい（形を明示するための薄い別名） */
export const ipcTransport = (fn) => fn;

// ---------------------------------------------------------------------------
// 要素の名前・説明（参照実装 elementInfo.ts）

const CONTROL = 'button, a[href], input, select, textarea, summary, [role="radio"], [role="button"], [role="link"], img';
const clip = (s, max) => {
  const t = String(s).replace(/\s+/g, ' ').trim();
  return t.length > max ? `${t.slice(0, max)}…` : t;
};

export function labelOf(el, max = 40) {
  const aria = el.getAttribute('aria-label');
  if (aria) return clip(aria, max);
  if (el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement) return clip(el.value || el.placeholder || el.title.split('\n')[0] || '', max);
  if (el instanceof HTMLSelectElement) return clip(el.selectedOptions[0]?.textContent ?? el.value, max);
  if (el instanceof HTMLImageElement) return clip(el.alt || el.title, max);
  const text = el instanceof HTMLElement ? el.innerText : el.textContent ?? '';
  if (text && text.trim() !== '') return clip(text, max);
  const title = el.getAttribute('title');
  return title ? clip(title.split('\n')[0] ?? '', max) : '';
}

const classesOf = (el, n) => (typeof el.className === 'string' ? el.className.trim().split(/\s+/).filter((c) => c !== '' && !c.startsWith('fb-')).slice(0, n) : []);

export function kindOf(el) {
  return `${el.tagName.toLowerCase()}${el.id ? `#${el.id}` : ''}${classesOf(el, 3).map((c) => `.${c}`).join('')}`;
}

export function describeElement(el) {
  const label = labelOf(el);
  return `${kindOf(el)}${label ? `「${label}」` : ''}`;
}

/** data-fb の道筋（外側から）。data-fb の無い要素は「祖先の道筋 › 要素の説明」 */
export function fbPath(el) {
  const names = [];
  for (let cur = el; cur && cur !== document.body; cur = cur.parentElement) {
    const n = cur.getAttribute('data-fb');
    if (n) names.unshift(n);
  }
  const base = names.join('/');
  if (el.hasAttribute('data-fb')) return base;
  return base ? `${base} › ${describeElement(el)}` : describeElement(el);
}

export function cssPath(el) {
  const parts = [];
  for (let cur = el; cur && cur !== document.body && cur !== document.documentElement; cur = cur.parentElement) {
    if (cur.id) {
      parts.unshift(`${cur.tagName.toLowerCase()}#${cur.id}`);
      break;
    }
    let seg = `${cur.tagName.toLowerCase()}${classesOf(cur, 2).map((c) => `.${CSS.escape(c)}`).join('')}`;
    const parent = cur.parentElement;
    if (parent) {
      const same = [...parent.children].filter((c) => c.tagName === cur.tagName);
      if (same.length > 1) seg += `:nth-of-type(${same.indexOf(cur) + 1})`;
    }
    parts.unshift(seg);
  }
  return parts.join(' > ');
}

export function meaningfulTarget(el) {
  return el.closest(CONTROL) ?? el.closest('[data-fb]') ?? el;
}

export function parentTarget(el) {
  const p = el.parentElement?.closest('[data-fb]') ?? null;
  return p && p !== document.body ? p : null;
}

/** どの要素にもある状態（参照実装 #generalState） */
export function generalState(el) {
  const s = {};
  if (el instanceof HTMLButtonElement || el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement) s.disabled = el.disabled;
  if (el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement) {
    // 【汎用化で追加】パスワード欄の値は入れない
    s.value = el instanceof HTMLInputElement && el.type === 'password' ? '***' : clip(el.value, 200);
  }
  if (el instanceof HTMLAnchorElement) s.href = el.getAttribute('href');
  for (const a of ['aria-checked', 'aria-expanded', 'aria-invalid', 'role']) {
    const v = el.getAttribute(a);
    if (v !== null) s[a] = v;
  }
  const label = labelOf(el, 80);
  if (label) s.label = label;
  return s;
}

// ---------------------------------------------------------------------------
// ログ: このファイルはログを「作らない」。アプリのログ基盤から読んで添付するだけ（getRecentLogs）。
//   ログの収集・形式・保存・ローテーション・圧縮・redact・本番での扱いは別タスク（references/build/build.md 手順 1c、architecture.md §2.5）。
//   暫定の実装が要るときだけ assets/templates/web/log-buffer.example.js（console のフック。feedback とは別物）を使う。

// ---------------------------------------------------------------------------
// スクリーンショット（既定: window.html2canvas があれば使う。無ければ撮らない。差し替え可: opts.capture）

async function defaultCapture(targetRect, rootEl) {
  const h2c = window.html2canvas;
  if (typeof h2c !== 'function') return { error: 'html2canvas が無いので撮っていません（opts.capture で差し替え可）' };
  const scale = window.devicePixelRatio || 1;
  const canvas = await h2c(document.documentElement, { x: window.scrollX, y: window.scrollY, width: window.innerWidth, height: window.innerHeight, scale, ignoreElements: (el) => el === rootEl });
  const m = 24; // 参照実装の ELEMENT_MARGIN
  const sx = canvas.width / window.innerWidth;
  const sy = canvas.height / window.innerHeight;
  const left = Math.max(0, Math.floor((targetRect.x - m) * sx));
  const top = Math.max(0, Math.floor((targetRect.y - m) * sy));
  const right = Math.min(canvas.width, Math.ceil((targetRect.x + targetRect.width + m) * sx));
  const bottom = Math.min(canvas.height, Math.ceil((targetRect.y + targetRect.height + m) * sy));
  const png = (c) => c.toDataURL('image/png').split(',')[1];
  let element = null;
  if (right - left >= 1 && bottom - top >= 1) {
    const c = document.createElement('canvas');
    c.width = right - left;
    c.height = bottom - top;
    c.getContext('2d').drawImage(canvas, left, top, c.width, c.height, 0, 0, c.width, c.height);
    element = png(c);
  }
  return { windowPng: png(canvas), elementPng: element };
}

// ---------------------------------------------------------------------------
// フィードバックモード本体

const STYLE = `
.fb-root{position:fixed;inset:0;z-index:2147483000;pointer-events:none;font:13px/1.5 system-ui,sans-serif;color:#1b1f24}
.fb-overlay{position:fixed;inset:0;pointer-events:auto;cursor:crosshair;background:transparent}
.fb-highlight{position:fixed;left:var(--x);top:var(--y);width:var(--w);height:var(--h);outline:2px solid #2f6feb;background:rgba(47,111,235,.12);pointer-events:none}
.fb-hl-label{position:absolute;left:0;top:-22px;background:#2f6feb;color:#fff;padding:1px 6px;border-radius:3px;font-size:11px;white-space:nowrap;max-width:60vw;overflow:hidden;text-overflow:ellipsis}
.fb-highlight.label-below .fb-hl-label{top:100%}
.fb-frame{position:fixed;left:var(--x);top:var(--y);width:var(--w);height:var(--h);outline:3px solid #e5484d;pointer-events:none}
.fb-banner{position:fixed;top:8px;right:8px;pointer-events:auto;background:#2f6feb;color:#fff;border-radius:6px;padding:6px 10px;display:flex;gap:10px;align-items:center}
.fb-banner button{font:inherit;cursor:pointer}
.fb-balloon{position:fixed;left:var(--x);top:var(--y);width:340px;box-sizing:border-box;pointer-events:auto;background:#fff;border:1px solid #c9d1d9;border-radius:8px;box-shadow:0 6px 24px rgba(0,0,0,.25);padding:10px}
.fb-balloon textarea{width:100%;box-sizing:border-box;font:inherit;min-height:84px}
.fb-b-name{font-weight:600;word-break:break-all}.fb-b-el{color:#57606a;font-size:12px;margin-bottom:6px;word-break:break-all}
.fb-actions{display:flex;gap:6px;margin-top:6px;flex-wrap:wrap}.fb-actions .grow{flex:1}
.fb-status{display:block;margin-top:6px;min-height:1em}.fb-status.err{color:#c62828}
.fb-opts label{margin-right:10px;white-space:nowrap}
.fb-root.capturing .fb-balloon,.fb-root.capturing .fb-banner,.fb-root.capturing .fb-highlight{visibility:hidden}
.fb-toast{position:fixed;left:50%;bottom:24px;transform:translateX(-50%);background:#1b1f24;color:#fff;padding:8px 14px;border-radius:6px;z-index:2147483001}
`;

const SECTION_LABELS = { target: '選択位置', log: '操作ログ', shots: 'スクリーンショット', env: '環境', screen: '今の画面', view: '画面の状態' };
const DEFAULT_OPTIONS = { target: true, log: true, shots: true, env: false, screen: false, view: false };
const OPTIONS_KEY = 'feedback-mode:options';

function safeLogs(fn) {
  try {
    return typeof fn === 'function' ? fn() ?? [] : [];
  } catch {
    return []; // ログが読めなくても送る（参照実装も、画面の状態が読めなければ理由を書いて続ける）
  }
}

const nextFrame = () => new Promise((r) => requestAnimationFrame(() => r()));

export function installFeedbackMode(opts = {}) {
  const o = {
    appName: document.title || 'app',
    enabled: false, // debug ビルドの入口が true を渡したときだけ動く（本番に紛れても何もしない保険）
    transport: null, // 必須（httpTransport / ipcTransport / 自作）。既定の URL は持たない
    hotkey: (e) => e.altKey && e.shiftKey && e.code === 'KeyF',
    getScreen: () => ({ id: location.pathname, hash: location.hash, title: document.title }),
    getView: () => ({}),
    getEnv: () => ({ userAgent: navigator.userAgent, language: navigator.language, time: new Date().toString() }),
    stateOf: () => ({}),
    pointer: '',
    /** ログの差し込み口: () => [{ ts, level: 'debug'|'info'|'warn'|'error', text }]。古い順。ログ基盤はアプリ側（別タスク）。省略可 */
    getRecentLogs: null,
    /** feedback 自身の動き（enter / select / exit / save-failed）をアプリのログ基盤に流す口: (level, text) => void。省略可 */
    logEvent: null,
    capture: defaultCapture,
    ...opts,
  };
  if (o.enabled !== true) return { enter() {}, exit() {}, active: () => false, options: () => ({ ...DEFAULT_OPTIONS }), disabled: true };
  if (typeof o.transport !== 'function') throw new Error('installFeedbackMode: transport が必要です（httpTransport(endpoint) など）');
  let active = false;
  let selected = null; // { el, path, key, click }
  let hover = null;
  let carry = null;
  let sending = false;
  let prevFocus = null;
  let textarea = null;
  let statusEl = null;
  const drafts = new Map(); // 要素ごとの下書き（モードを出ても残す。送ったら消す）
  const offs = [];
  let options = { ...DEFAULT_OPTIONS };
  try {
    options = { ...DEFAULT_OPTIONS, ...JSON.parse(localStorage.getItem(OPTIONS_KEY) ?? '{}') };
  } catch { /* 覚えていなければ初期値 */ }

  const el = (tag, attrs = {}, ...kids) => {
    const n = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (k.startsWith('on')) n.addEventListener(k.slice(2), v);
      else if (v !== false && v != null) n.setAttribute(k, v === true ? '' : v);
    }
    n.append(...kids.filter((x) => x != null));
    return n;
  };
  const root = el('div', { class: 'fb-root', 'data-fb': 'フィードバックモード' });
  const overlay = el('div', { class: 'fb-overlay', 'aria-hidden': 'true' });
  const highlight = el('div', { class: 'fb-highlight', hidden: true });
  const hlLabel = el('span', { class: 'fb-hl-label' });
  highlight.append(hlLabel);
  const frame = el('div', { class: 'fb-frame', hidden: true });
  const balloon = el('div', { class: 'fb-balloon', role: 'dialog', 'aria-label': 'フィードバック', hidden: true });
  const banner = el('div', { class: 'fb-banner', role: 'status' }, el('b', {}, 'フィードバックモード'), el('span', {}, '要素をクリックしてコメント。Esc で終了'), el('button', { type: 'button', onclick: () => exit('banner') }, '終える'));
  root.append(overlay, highlight, frame, balloon, banner);
  const style = el('style', {});
  style.textContent = STYLE;

  const inUi = (t) => t instanceof Node && (balloon.contains(t) || banner.contains(t));
  const under = (x, y) => {
    for (const e of document.elementsFromPoint(x, y)) if (!root.contains(e) && e !== document.documentElement && e !== document.body) return e;
    return null;
  };

  function draw(target) {
    if (!target || !target.isConnected) { highlight.hidden = true; return; }
    const r = target.getBoundingClientRect();
    highlight.hidden = false;
    for (const [k, v] of [['--x', r.x], ['--y', r.y], ['--w', r.width], ['--h', r.height]]) highlight.style.setProperty(k, `${v}px`);
    highlight.classList.toggle('label-below', r.y < 26);
    hlLabel.textContent = `${fbPath(target)}  ${Math.round(r.width)} × ${Math.round(r.height)}`;
  }

  function listen() {
    const on = (type, fn, extra = {}) => {
      const opt = { capture: true, ...extra };
      window.addEventListener(type, fn, opt);
      offs.push(() => window.removeEventListener(type, fn, opt));
    };
    const block = (e) => { e.preventDefault(); e.stopImmediatePropagation(); };
    for (const t of ['pointerdown', 'pointerup', 'mousedown', 'mouseup', 'dblclick', 'auxclick', 'contextmenu', 'dragstart', 'submit']) on(t, (e) => { if (!inUi(e.target)) block(e); });
    on('click', (e) => {
      if (inUi(e.target)) return;
      block(e);
      // オーバーレイへのクリックだけで選ぶ。プログラムからの el.click() は止めるだけ
      if (e.target === overlay && e.button === 0) select(e.clientX, e.clientY);
    });
    on('pointermove', (e) => {
      if (inUi(e.target)) return;
      e.stopImmediatePropagation();
      if (sending) return;
      const u = under(e.clientX, e.clientY);
      hover = u ? meaningfulTarget(u) : null;
      draw(hover ?? selected?.el ?? null);
    });
    on('wheel', (e) => {
      if (inUi(e.target)) return;
      block(e);
      // ホイールは下の要素のスクロールできる入れ物に渡す
      let n = under(e.clientX, e.clientY);
      while (n && n !== document.body && !(/(auto|scroll)/.test(getComputedStyle(n).overflowY) && n.scrollHeight > n.clientHeight + 1)) n = n.parentElement;
      (n && n !== document.body ? n : document.scrollingElement ?? document.documentElement).scrollBy({ left: e.deltaX, top: e.deltaY });
    }, { passive: false });
    for (const t of ['keydown', 'keyup', 'keypress']) {
      on(t, (e) => {
        if (e.key === 'Escape') { block(e); if (t === 'keydown') exit('esc'); return; }
        if (balloon.contains(e.target)) {
          if (t === 'keydown' && e.key === 'Enter' && (e.metaKey || e.ctrlKey)) { e.preventDefault(); void send(); }
          e.stopImmediatePropagation(); // アプリのショートカットに渡さない
          return;
        }
        if (banner.contains(e.target) && (e.key === 'Enter' || e.key === ' ' || e.key === 'Tab')) { e.stopImmediatePropagation(); return; }
        block(e);
      });
    }
    on('focusin', (e) => {
      if (inUi(e.target)) return;
      if (e.target instanceof HTMLElement) e.target.blur(); // Enter・Space でアプリの要素を押せないように
      if (selected && textarea) textarea.focus({ preventScroll: true });
    });
    on('resize', reposition);
    on('scroll', reposition);
  }

  function select(x, y, target) {
    const t = target ?? (() => { const u = under(x, y); return u ? meaningfulTarget(u) : null; })();
    if (!t) return;
    if (selected) saveDraft();
    const path = fbPath(t);
    selected = { el: t, path, key: `${path}\u0000${cssPath(t)}`, click: { x, y } };
    o.logEvent?.('info', `feedback.select target=${path}`);
    openBalloon();
  }

  function saveDraft() {
    if (!selected || !textarea) return;
    if (textarea.value.trim() === '') drafts.delete(selected.key);
    else drafts.set(selected.key, textarea.value);
  }

  function openBalloon() {
    const sel = selected;
    let initial = drafts.get(sel.key) ?? '';
    if (!drafts.has(sel.key) && carry !== null) { initial = carry; carry = null; }
    textarea = el('textarea', { class: 'fb-text', rows: 4, placeholder: 'この部品へのフィードバック', 'aria-label': 'フィードバック' });
    textarea.value = initial;
    statusEl = el('span', { class: 'fb-status', role: 'status' });
    const parent = parentTarget(sel.el);
    const btn = (label, title, fn, disabled = false) => el('button', { type: 'button', title, disabled, onclick: fn }, label);
    const opts2 = el('div', { class: 'fb-opts' }, ...Object.keys(SECTION_LABELS).map((id) => {
      const input = el('input', { type: 'checkbox', checked: options[id] });
      input.addEventListener('change', () => { options = { ...options, [id]: input.checked }; try { localStorage.setItem(OPTIONS_KEY, JSON.stringify(options)); } catch { /* 無くてもよい */ } });
      return el('label', {}, input, SECTION_LABELS[id]);
    }));
    balloon.replaceChildren(
      el('div', { class: 'fb-b-name', title: sel.path }, sel.path),
      el('div', { class: 'fb-b-el' }, describeElement(sel.el)),
      textarea,
      el('div', { class: 'fb-actions' },
        btn('親を選ぶ', parent ? `${fbPath(parent)} を選ぶ` : '親の部品はありません', () => reselect(parent), parent === null),
        btn('選び直す', '別の要素を選ぶ（文は次の要素に持っていく）', () => reselect(null)),
        btn('閉じる', 'モードを終える（文は下書きに残る。Esc）', () => { saveDraft(); exit('close'); }),
      ),
      opts2,
      el('div', { class: 'fb-actions' }, el('span', { class: 'grow' }), btn('送信（⌘/Ctrl+Enter）', 'フィードバック置き場に追加する', () => void send())),
      statusEl,
    );
    balloon.hidden = false;
    draw(sel.el);
    reposition();
    textarea.focus({ preventScroll: true });
    textarea.setSelectionRange(textarea.value.length, textarea.value.length);
  }

  function reselect(target) {
    if (!selected) return;
    const text = textarea?.value ?? '';
    drafts.delete(selected.key);
    carry = text.trim() === '' ? null : text;
    textarea = null;
    selected = null;
    balloon.hidden = true;
    balloon.replaceChildren();
    if (target) {
      const r = target.getBoundingClientRect();
      select(r.x + r.width / 2, r.y + r.height / 2, target);
    } else draw(hover);
  }

  function reposition() {
    if (!active || !selected) return;
    const t = selected.el.isConnected ? selected.el : null;
    if (t) draw(t);
    const r = t ? t.getBoundingClientRect() : { x: selected.click.x, y: selected.click.y, width: 1, height: 1, bottom: selected.click.y + 1, top: selected.click.y };
    const w = balloon.offsetWidth || 340;
    const hgt = balloon.offsetHeight || 220;
    const m = 8;
    const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));
    const x = clamp(r.x + r.width / 2 - w / 2, m, Math.max(m, innerWidth - m - w));
    // 下 → 上 → 画面内に重ねる（参照実装 placeBalloon の簡略版。右・左は省いた）
    let y = r.y + r.height + 10;
    if (y + hgt > innerHeight - m) y = r.y - 10 - hgt >= m ? r.y - 10 - hgt : clamp(innerHeight - hgt - m, m, innerHeight);
    balloon.style.setProperty('--x', `${x}px`);
    balloon.style.setProperty('--y', `${y}px`);
  }

  function collectTarget() {
    const t = selected.el;
    const r = t.isConnected ? t.getBoundingClientRect() : null;
    const rect = r ? { x: r.x, y: r.y, width: r.width, height: r.height } : { x: selected.click.x, y: selected.click.y, width: 1, height: 1 };
    const state = { ...generalState(t), ...o.stateOf(t) };
    if (!t.isConnected) state.note = '選んだあとに画面が描き直されたので、矩形は選んだときのもの';
    return { screen: o.getScreen().id ?? '', path: selected.path, element: describeElement(t), selector: cssPath(t), rect, click: selected.click, state };
  }

  async function send() {
    if (!selected || sending) return;
    sending = true;
    const sel = selected;
    const target = collectTarget();
    const comment = textarea?.value ?? '';
    const opt = { ...options };
    statusEl.className = 'fb-status';
    statusEl.textContent = '送信しています…';
    frame.style.setProperty('--x', `${target.rect.x}px`);
    frame.style.setProperty('--y', `${target.rect.y}px`);
    frame.style.setProperty('--w', `${target.rect.width}px`);
    frame.style.setProperty('--h', `${target.rect.height}px`);
    frame.hidden = false;
    root.classList.add('capturing'); // 写さない: 吹き出し・帯・強調。選んだ要素は枠で囲む
    let result;
    try {
      let shot = {};
      if (opt.shots) {
        await nextFrame();
        await nextFrame();
        try { shot = await o.capture(target.rect, root); } catch (err) { shot = { error: String(err?.message ?? err) }; }
      }
      const payload = {
        appName: o.appName, pointer: o.pointer, comment, target,
        env: o.getEnv(), screen: o.getScreen(), view: o.getView(), logs: safeLogs(o.getRecentLogs),
        viewport: { width: innerWidth, height: innerHeight, dpr: devicePixelRatio },
      };
      const body = { payload, options: opt, windowPng: shot.windowPng ?? null, elementPng: shot.elementPng ?? null, shotError: shot.error ?? null };
      // 画面側の送信（Transport）。置き場（Sink）の差し替えは受け口の裏側で行う
      result = await o.transport(body);
      if (!result?.ok) throw new Error(result?.error ?? '送信に失敗しました');
    } catch (err) {
      root.classList.remove('capturing');
      frame.hidden = true;
      sending = false;
      statusEl.className = 'fb-status err';
      statusEl.textContent = `追加できませんでした: ${err?.message ?? err}`; // 失敗したら吹き出しに理由を出してモードを続ける
      textarea?.focus({ preventScroll: true });
      o.logEvent?.('warn', `feedback.save-failed ${err?.message ?? err}`);
      return;
    }
    root.classList.remove('capturing');
    frame.hidden = true;
    sending = false;
    drafts.delete(sel.key); // 送ったので下書きは消す
    if (textarea) textarea.value = '';
    toast(`${result.ref ?? result.rel ?? result.id ?? 'フィードバック'} に追加しました`);
    exit('save');
  }

  function toast(text) {
    const t = el('div', { class: 'fb-toast', role: 'status' }, text);
    document.body.append(t);
    setTimeout(() => t.remove(), 3500);
  }

  function enter(via = 'api') {
    if (active) return;
    active = true;
    const a = document.activeElement;
    prevFocus = a instanceof HTMLElement && a !== document.body ? a : null;
    prevFocus?.blur();
    document.documentElement.classList.add('feedback-mode');
    document.head.append(style);
    (document.querySelector('dialog:modal') ?? document.body).append(root); // モーダルを開いていればその中（参照実装と同じ）
    listen();
    o.logEvent?.('info', `feedback.enter via=${via}`);
  }

  function exit(via = 'api') {
    if (!active) return;
    active = false;
    saveDraft();
    for (const off of offs.splice(0)) off();
    selected = null;
    textarea = null;
    hover = null;
    carry = null;
    balloon.hidden = true;
    balloon.replaceChildren();
    highlight.hidden = true;
    root.remove();
    style.remove();
    document.documentElement.classList.remove('feedback-mode');
    prevFocus?.isConnected && prevFocus.focus({ preventScroll: true });
    prevFocus = null;
    o.logEvent?.('info', `feedback.exit via=${via}`);
  }

  // ホットキーで入る・出る（モード中のキーは listen() が止めるので、Esc 以外の出入りは entered 状態で判断）
  window.addEventListener('keydown', (e) => { if (!active && o.hotkey(e)) { e.preventDefault(); enter('hotkey'); } });

  return { enter, exit, active: () => active, options: () => ({ ...options }) };
}
