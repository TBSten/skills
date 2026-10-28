/**
 * http-sink.mjs — HTTP で別マシンの受け口（assets/templates/web/feedback-dev-server.mjs）に送る・読む・閉じる Sink。【追加】【未検証: 実機・LAN 越しは未検証】
 *
 * 想定:
 *   - スマホ実機・別マシンのアプリ → 同じ LAN の開発機（リポジトリがあるマシン）の受け口。受け口は裏で file-sink を使う。
 *   - Agent が別マシンから受け口の置き場を list / read / archive する（受け口の裏がファイルなら、Agent は開発機で直接 ls / mv してもよい）。
 * 画面側（ブラウザ・RN・Flutter など）が JS で文を組み立てられない場合は、この Sink ではなく feedback-mode.js の transport（生の submission を
 * POST /__feedback に送る）を使い、受け口側で submitFeedback を呼ぶ（assets/templates/web/feedback-dev-server.mjs が両方に対応）。
 *
 * 認証: 受け口に FEEDBACK_TOKEN を設定したら、ここにも token を渡す（ヘッダ x-feedback-token）。LAN に出すときは必須にする。
 */
export function toBase64(bytes) {
  if (typeof Buffer !== 'undefined') return Buffer.from(bytes).toString('base64');
  let bin = '';
  for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(bin);
}

/** @returns {import('./feedback-core.mjs').Sink} */
export function createHttpSink({ baseUrl, token = null, fetchImpl = globalThis.fetch }) {
  const base = baseUrl.replace(/\/+$/, '');
  const call = async (method, p, body) => {
    const headers = { 'content-type': 'application/json', ...(token ? { 'x-feedback-token': token } : {}) };
    const res = await fetchImpl(`${base}${p}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const j = await res.json().catch(() => ({}));
    if (!res.ok || j.ok === false) throw new Error(j.error ?? `HTTP ${res.status}`);
    return j;
  };
  return {
    kind: 'http',
    capabilities: { put: true, list: true, read: true, archive: true, doc: false }, // doc（writeDoc / readDoc）は受け口に経路が無い
    async putAttachments(files, { now } = {}) {
      const j = await call('POST', '/__feedback/attachments', { now: now?.toISOString(), files: files.map((f) => ({ name: f.name, mime: f.mime ?? 'application/octet-stream', dataBase64: toBase64(f.data) })) });
      return { refs: j.refs ?? {}, dir: j.dir ?? null, error: j.error ?? null };
    },
    async putEntry({ text, now }) {
      const j = await call('POST', '/__feedback/entries', { text, now: now?.toISOString() });
      return j.entry;
    },
    async list({ state = 'all' } = {}) {
      return (await call('GET', `/__feedback/entries?state=${encodeURIComponent(state)}`)).entries;
    },
    async read(id) {
      return (await call('GET', `/__feedback/entries/${encodeURIComponent(id)}`)).entry;
    },
    async archive(id, { note } = {}) {
      return (await call('POST', `/__feedback/entries/${encodeURIComponent(id)}/archive`, { note })).entry;
    },
  };
}
