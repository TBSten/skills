/**
 * feedback-core.mjs — フィードバック機能の「置き場に依存しないコア」（依存なし・ESM・Node でもブラウザでも動く）。
 *
 * 中身（すべて純粋な関数。ファイルを読み書きしない）:
 *   - Sink（フィードバック置き場）の契約（下の JSDoc）と submitFeedback（スクショの添付 → 文の組み立て → 置き場へ保存の一連）【追加】
 *   - 採番の規則 nextSeqFromIds（置き場が何であっても同じ。archived の番号も数える）【実績】
 *   - redact（秘密情報の伏せ字）【実績】/ cropRect（スクショの切り抜き範囲）【実績】/ buildFeedbackMarkdown（文の組み立て）【追加】
 * 置き場の実装は別ファイル:
 *   file-sink.mjs   ローカルのファイル（参照実装の feedbackFile.ts・feedbackShots.ts の写し）【実績】
 *   memory-sink.mjs メモリ（テスト・契約の確認用）【追加】
 *   http-sink.mjs   HTTP で別マシンの受け口（feedback-dev-server.mjs）に送る・読む・閉じる【追加】
 *   （Issue トラッカー・クラウドストレージは references/build/architecture.md §2.2 に概要のみ【未検証】）
 *
 * 参照実装との関係: 参照実装は main の buildFeedback（feedbackText.ts + feedbackFile.ts + feedbackShots.ts）が、置き場（ファイル）を直接呼んでいる。
 * ここではその 3 つを「文の組み立て（このファイル）」と「置き場（Sink）」に分け、置き場を差し替えられるようにした。
 *
 * テスト: node --test assets/templates/core/*.test.mjs
 */

/**
 * @typedef {Object} Attachment            添付（スクリーンショットなど）
 * @property {string} name                 'window.png' / 'element.png'
 * @property {Uint8Array} data
 * @property {string} [mime]               'image/png'
 *
 * @typedef {Object} PutAttachmentsResult
 * @property {Record<string,string>} refs  name → 参照（ファイルなら絶対パス、HTTP なら URL やパス、Issue なら添付 URL）。文に書かれ、Agent が Read/取得する
 * @property {string|null} [dir]           まとめ先（ファイルならディレクトリ）。無ければ null
 * @property {string|null} error           失敗の理由（投げずに返す。文に「撮れませんでした（理由）」と出る）
 *
 * @typedef {Object} EntryInfo             1 件の記録
 * @property {string} id                   置き場の中で一意。参照実装のファイルでは 'yymmdd-xx'（261028-01）。名前にユーザの入力を入れない
 * @property {string} ref                  人・Agent が開ける参照（相対パス・URL・Issue 番号など）
 * @property {'open'|'archived'} state     参照実装の「場所」（feedback/ か feedback/archived/）に相当
 * @property {number} [bytes]
 * @property {string} [createdAt]
 *
 * @typedef {Object} Sink                  フィードバック置き場
 * @property {string} kind                 'file' | 'memory' | 'http' | …
 * @property {{put:boolean,list:boolean,read:boolean,archive:boolean}} capabilities  できること。できない操作は呼ぶと投げる（例: 一方向のフォームへの送信は put だけ）
 * @property {(files: Attachment[], ctx?: {now?: Date}) => Promise<PutAttachmentsResult>} putAttachments  添付を保存し、文に書く参照を返す。putEntry より先に呼ぶ
 * @property {(entry: {text: string, now?: Date}) => Promise<EntryInfo & {attempts?: number}>} putEntry     採番して保存する。上書きしない・競合したら採番し直す・名前にユーザ入力を入れない（参照実装の規則）
 * @property {(q?: {state?: 'open'|'archived'|'all'}) => Promise<EntryInfo[]>} list     一覧（id の昇順）
 * @property {(id: string) => Promise<{text: string} & EntryInfo>} read                 1 件の全文
 * @property {(id: string, opts?: {note?: string}) => Promise<EntryInfo>} archive       対応済みにする（参照実装では mv。note があれば末尾に 1 行追記してよい）
 */

export const ARCHIVE_DIR = 'archived';
/** 参照実装の ELEMENT_MARGIN */
export const ELEMENT_MARGIN = 24;

// ---------------------------------------------------------------------------
// 秘密情報の伏せ字（参照実装 util.ts の redact とパターンは同じ）

export function redact(text) {
  return String(text)
    .replace(/\b(Bearer)\s+[A-Za-z0-9._~+/=-]{8,}/gi, '$1 ***')
    .replace(/\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{4,}/g, '***')
    .replace(/\b(Cookie|Set-Cookie)\s*:\s*[^\n]+/gi, '$1: ***')
    .replace(
      /\b(_csrf|csrf[_-]?token|x-csrf-token|access_token|refresh_token|id_token|client_secret|authorization|token|password|passwd|secret|api[_-]?key)(["']?\s*[:=]\s*["']?)[^\s"'&,;}]{6,}/gi,
      '$1$2***',
    )
    .replace(/\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pous]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})/g, '***');
}

// ---------------------------------------------------------------------------
// 採番の規則（参照実装 feedbackFile.ts。置き場が何でも同じ規則にそろえる）

const p2 = (n) => String(n).padStart(2, '0');

/** ローカル時刻の yymmdd（2026-10-23 → 261023） */
export function feedbackDay(d) {
  return `${p2(d.getFullYear() % 100)}${p2(d.getMonth() + 1)}${p2(d.getDate())}`;
}

/** id = yymmdd-xx（xx は 2 桁以上の連番） */
export function entryId(day, seq) {
  return `${day}-${String(seq).padStart(2, '0')}`;
}

export const ID_PATTERN = /^(\d{6})-(\d{2,})$/;

/**
 * 次の連番。ids は「未対応と対応済みの両方」の id（参照実装: feedback/ と feedback/archived/ の同じ日付の最大 + 1。
 * 番号を再利用しない・日付が変われば 01 に戻る）。
 */
export function nextSeqFromIds(ids, day) {
  let max = 0;
  for (const id of ids) {
    const m = ID_PATTERN.exec(id);
    if (m && m[1] === day) max = Math.max(max, Number(m[2]));
  }
  return max + 1;
}

// ---------------------------------------------------------------------------
// スクリーンショットの切り抜き範囲（参照実装 feedbackShots.ts の cropRect）

/**
 * 要素の切り抜き範囲（画像の px）。target（CSS px / dp）+ margin に、画像と viewport の比（devicePixelRatio）を掛け、画像の中に収める。
 * 収まる部分が無ければ null。
 */
export function cropRect(target, viewport, image, margin = ELEMENT_MARGIN) {
  const sx = viewport.width > 0 ? image.width / viewport.width : 1;
  const sy = viewport.height > 0 ? image.height / viewport.height : 1;
  const left = Math.max(0, Math.floor((target.x - margin) * sx));
  const top = Math.max(0, Math.floor((target.y - margin) * sy));
  const right = Math.min(image.width, Math.ceil((target.x + target.width + margin) * sx));
  const bottom = Math.min(image.height, Math.ceil((target.y + target.height + margin) * sy));
  if (right - left < 1 || bottom - top < 1) return null;
  return { x: left, y: top, width: right - left, height: bottom - top };
}

// ---------------------------------------------------------------------------
// 文（Markdown）の組み立て（参照実装 feedbackText.ts の feedbackMarkdown を縮めたもの）

/** 節（参照実装は 6 つ: target / log / shots / env / screen / view）。「フィードバックの文」は常に入れる */
export const SECTIONS = ['target', 'log', 'shots', 'env', 'screen', 'view'];
export const SECTION_HEADINGS = { target: '選んだ位置', log: 'ログ', shots: 'スクリーンショット', env: '環境', screen: '今の画面', view: '画面に出ている情報' };
/** 参照実装の初期値: 選択位置・ログ・スクリーンショットがオン、環境・今の画面・画面の状態がオフ */
export const DEFAULT_OPTIONS = { target: true, log: true, shots: true, env: false, screen: false, view: false };

export function normalizeOptions(v) {
  const o = typeof v === 'object' && v !== null ? v : {};
  const out = { ...DEFAULT_OPTIONS };
  for (const id of SECTIONS) if (typeof o[id] === 'boolean') out[id] = o[id];
  return out;
}

const one = (s) => String(s).replace(/\s+/g, ' ').trim();
const byteLength = (s) => new TextEncoder().encode(s).length;

/** インラインコード（中にバッククォートがあれば長い組で囲む） */
export function code(s) {
  const longest = Math.max(0, ...(String(s).match(/`+/g) ?? []).map((m) => m.length));
  const fence = '`'.repeat(longest + 1);
  return longest > 0 ? `${fence} ${s} ${fence}` : `${fence}${s}${fence}`;
}

/** コードブロック（中身の ``` と紛れないようフェンスを長くする） */
export function block(lines, lang = 'text') {
  const body = lines.join('\n');
  const longest = Math.max(2, ...(body.match(/`{3,}/g) ?? []).map((m) => m.length));
  const fence = '`'.repeat(longest + 1);
  return `${fence}${lang}\n${body}\n${fence}`;
}

const kv = (obj) => Object.entries(obj ?? {}).map(([k, v]) => `- ${k}: ${v === null || v === undefined ? 'null' : one(typeof v === 'object' ? JSON.stringify(v) : v)}`);

/**
 * payload の形は assets/templates/feedback.schema.json。
 *   appName, pointer（Agent への案内: ソースの場所・ログの形）, comment,
 *   target { path, element, selector, rect, click, screen, state }, shots { window, element, error }（参照 = Sink が返した refs）,
 *   env {…}, screen {…}, view {…}, logs [{ ts, level, text }]
 */
export function buildFeedbackMarkdown(payload, options = DEFAULT_OPTIONS, { logLimitBytes = 200 * 1024 } = {}) {
  const o = normalizeOptions(options);
  const off = SECTIONS.filter((id) => !o[id]);
  const lines = [`# ${payload.appName ?? 'app'}: 画面へのフィードバック`, ''];
  if (payload.pointer) lines.push(payload.pointer, '');
  if (off.length > 0) lines.push(`（オプションでオフ: ${off.map((id) => SECTION_HEADINGS[id]).join('・')}）`, '');
  const comment = (payload.comment ?? '').trim();
  lines.push('## フィードバックの文', '', ...(comment === '' ? ['（書いていません）'] : comment.split(/\r?\n/).map((l) => `> ${l}`)));
  const t = payload.target;
  if (o.target && t) {
    lines.push('', '## 選んだ位置', '');
    lines.push(`- 画面: ${code(t.screen ?? '')}`, `- 部品の道筋（data-fb / testID など）: ${code(t.path ?? '')}`, `- 要素: ${one(t.element ?? '')}`);
    if (t.selector) lines.push(`- セレクタ: ${code(t.selector)}`);
    if (t.rect) lines.push(`- 矩形: x ${Math.round(t.rect.x)}・y ${Math.round(t.rect.y)}・${Math.round(t.rect.width)} × ${Math.round(t.rect.height)}（CSS px / dp。ウィンドウの左上から）`);
    if (t.click) lines.push(`- クリックした座標: (${Math.round(t.click.x)}, ${Math.round(t.click.y)})`);
    lines.push('- 要素の状態:', ...(Object.keys(t.state ?? {}).length ? kv(t.state).map((l) => `  ${l}`) : ['  - なし']));
  }
  if (o.shots) {
    const s = payload.shots;
    lines.push('', '## スクリーンショット', '');
    if (s && (s.window || s.element)) {
      lines.push(`- ウィンドウ全体（選んだ要素を枠で囲む）: ${s.window ? code(s.window) : 'なし'}`, `- 要素の周り: ${s.element ? code(s.element) : 'なし'}`, '- 参照（パス・URL）を開けば画像を見られる（Claude Code ならパスを Read）');
    } else lines.push(`- 撮れませんでした${s?.error ? `（${one(s.error)}）` : ''}`);
  }
  const debug = [];
  if (o.env && payload.env) debug.push(['## 環境', '', ...kv(payload.env)]);
  if (o.screen && payload.screen) debug.push(['## 今の画面', '', ...kv(payload.screen)]);
  if (o.view && payload.view) debug.push(['## 画面に出ている情報', '', ...kv(payload.view)]);
  if (o.log) {
    const logs = payload.logs ?? [];
    const rows = logs.map((e) => `${e.ts ?? ''} ${(e.level ?? 'info').padEnd(5)} ${e.text ?? ''}`.trim());
    let size = 0;
    let from = rows.length;
    while (from > 0) {
      const b = byteLength(rows[from - 1]) + 1;
      if (size + b > logLimitBytes) break;
      size += b;
      from--;
    }
    const kept = rows.slice(from);
    const problems = logs.filter((e) => e.level === 'error' || e.level === 'warn').slice(-40).map((e) => `${e.ts ?? ''} ${e.level} ${e.text ?? ''}`);
    debug.push([
      '## ログ',
      '',
      from > 0 ? `- 新しい方から ${kept.length} 件（${Math.round(logLimitBytes / 1024)}KB まで）。古い ${from} 件は省いた` : `- ${kept.length} 件をすべて入れた`,
      '',
      `### error / warn の要約（${problems.length} 件）`,
      '',
      ...(problems.length ? [block(problems)] : ['- なし']),
      '',
      '### 一覧（古い順。新しいものが下）',
      '',
      block(kept),
    ]);
  }
  if (debug.length > 0) {
    lines.push('', '---', '', '以下はデバッグ情報。');
    for (const d of debug) lines.push('', ...d);
  }
  // 入れた節すべてに、文の全体に 1 回だけ redact を掛ける（参照実装と同じ。コピーと保存で文を変えないため、組み立ての 1 か所で）
  return redact(`${lines.join('\n')}\n`);
}

// ---------------------------------------------------------------------------
// 送信の一連（参照実装 main.ts の buildFeedback + saveFeedback に相当）【追加】

const SINK_METHODS = ['putAttachments', 'putEntry', 'list', 'read', 'archive'];

/** Sink の形を確かめる（足りないメソッドがあれば投げる）。capabilities にないものは呼んだとき投げてよい */
export function assertSink(sink) {
  if (!sink || typeof sink.kind !== 'string') throw new Error('Sink に kind がありません');
  for (const m of SINK_METHODS) if (typeof sink[m] !== 'function') throw new Error(`Sink（${sink.kind}）に ${m} がありません`);
  return sink;
}

/**
 * スクショを添付として保存 → 参照を文に入れて組み立て → 置き場へ保存。
 * @param {{ sink: Sink, payload: object, options?: object, windowPng?: Uint8Array|null, elementPng?: Uint8Array|null, shotError?: string|null, now?: Date }} req
 * @returns {Promise<{ ok: true } & EntryInfo & { text: string, shots: object }>}
 * 参照実装と同じく、スクショ失敗は投げず文に理由を書く。保存（putEntry）の失敗は投げる（呼ぶ側が吹き出しに理由を出してモードを続ける）。
 */
export async function submitFeedback({ sink, payload, options, windowPng = null, elementPng = null, shotError = null, now = new Date() }) {
  assertSink(sink);
  const opt = normalizeOptions(options);
  let shots = { dir: null, window: null, element: null, error: null };
  if (opt.shots) {
    if (windowPng && windowPng.length > 0) {
      const files = [{ name: 'window.png', data: windowPng, mime: 'image/png' }];
      if (elementPng && elementPng.length > 0) files.push({ name: 'element.png', data: elementPng, mime: 'image/png' });
      try {
        const r = await sink.putAttachments(files, { now });
        shots = { dir: r.dir ?? null, window: r.refs?.['window.png'] ?? null, element: r.refs?.['element.png'] ?? null, error: r.error ?? (r.refs?.['element.png'] ? null : '要素の切り抜きは作っていません') };
      } catch (err) {
        shots = { dir: null, window: null, element: null, error: err instanceof Error ? err.message : String(err) };
      }
    } else {
      shots.error = shotError ?? 'スクリーンショットが空でした';
    }
  }
  const text = buildFeedbackMarkdown({ ...payload, shots }, opt);
  const info = await sink.putEntry({ text, now });
  return { ok: true, ...info, text, shots };
}
