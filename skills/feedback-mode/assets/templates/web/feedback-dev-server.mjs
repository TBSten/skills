/**
 * feedback-dev-server.mjs — フィードバックの受け口（開発用）。【追加】
 *
 * 参照実装では「画面 → IPC → main がファイルを書く」だったところを、Electron の main が無い環境（Web・モバイル・Tauri の dev）向けに
 * 「画面 → HTTP POST → このサーバが Sink に書く」へ置き換えたもの。文の組み立て・redact・採番は参照実装と同じ規則（core/）。
 * 置き場（Sink）は差し替えられる: 既定は file-sink（参照実装と同じ。FEEDBACK_REPO_ROOT / FEEDBACK_DIR / FEEDBACK_ATTACH_DIR）。
 * createHandler({ sink }) に別の Sink（Issue トラッカー等。references/build/architecture.md §2.2）を渡せば、画面側は変えずに置き場だけ変わる。
 *
 *   FEEDBACK_REPO_ROOT=/path/to/repo FEEDBACK_DIR=feedback FEEDBACK_ATTACH_DIR=.local/tmp/feedback node feedback-dev-server.mjs
 *
 * ルート（すべて JSON）:
 *   POST /__feedback                        画面からの submission { payload, options, windowPng, elementPng, shotError }（base64）→ submitFeedback → { ok, id, ref, bytes, shots }
 *   POST /__feedback/attachments            HTTP Sink 用 { files:[{name,mime,dataBase64}], now? } → { ok, refs, dir, error }
 *   POST /__feedback/entries                HTTP Sink 用 { text, now? }（受け口でも redact を掛ける）→ { ok, entry }
 *   GET  /__feedback/entries?state=open     一覧 / GET /__feedback/entries/:id 全文 / POST /__feedback/entries/:id/archive { note }
 * 守ること: 既定は 127.0.0.1 にだけ bind する・ブラウザの Origin は localhost 系だけ許す・本文の大きさに上限・本番では起動しない。
 * FEEDBACK_TOKEN を設定すると、ヘッダ x-feedback-token が一致しない要求を 401 にする（LAN に出すときは必須）。
 * Vite / webpack-dev-server を使うなら、同じ handler を middleware に載せ替えてよい（未検証）。
 */
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { redact, submitFeedback } from '../core/feedback-core.mjs';
import { createFileSink } from '../core/file-sink.mjs';

const MAX_BODY = 30 * 1024 * 1024; // 参照実装の IPC は 500,000 字まで（画像は別経路）。HTTP は画像込みで 30MB まで
const asDate = (v) => { const d = typeof v === 'string' ? new Date(v) : null; return d && !Number.isNaN(d.getTime()) ? d : undefined; }; // クライアントの時刻（採番の日付・添付の名前。無ければ受け口の時刻）
const ORIGIN_OK = /^https?:\/\/(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$/;

export function createHandler({ sink = createFileSink({ repoRoot: path.resolve(process.env.FEEDBACK_REPO_ROOT ?? process.cwd()), dir: process.env.FEEDBACK_DIR ?? 'feedback', attachDir: process.env.FEEDBACK_ATTACH_DIR ?? '.local/tmp/feedback' }), token = process.env.FEEDBACK_TOKEN ?? null } = {}) {
  return async function handle(req, res) {
    const origin = req.headers.origin;
    const cors = origin && ORIGIN_OK.test(origin) ? { 'access-control-allow-origin': origin, 'access-control-allow-headers': 'content-type, x-feedback-token', 'access-control-allow-methods': 'GET, POST, OPTIONS', vary: 'origin' } : {};
    const send = (code, body) => { res.writeHead(code, { 'content-type': 'application/json', ...cors }); res.end(JSON.stringify(body)); };
    if (req.method === 'OPTIONS') return send(204, {});
    if (origin && !ORIGIN_OK.test(origin)) return send(403, { ok: false, error: 'origin not allowed' });
    if (token && req.headers['x-feedback-token'] !== token) return send(401, { ok: false, error: 'token required' });
    const url = new URL(req.url, 'http://localhost');
    const p = url.pathname;
    if (!p.startsWith('/__feedback')) return send(404, { ok: false, error: 'not found' });
    try {
      let body = null;
      if (req.method === 'POST') {
        const chunks = [];
        let size = 0;
        for await (const c of req) {
          size += c.length;
          if (size > MAX_BODY) return send(413, { ok: false, error: 'フィードバックの情報が大きすぎます' });
          chunks.push(c);
        }
        body = chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {};
      }
      if (req.method === 'POST' && p === '/__feedback') {
        if (!body || typeof body.payload !== 'object' || typeof body.payload.comment !== 'string' || body.payload.comment.length > 20_000) return send(400, { ok: false, error: '不正な指定です' });
        const dec = (b64) => (b64 ? new Uint8Array(Buffer.from(b64, 'base64')) : null);
        const r = await submitFeedback({ sink, payload: body.payload, options: body.options, windowPng: dec(body.windowPng), elementPng: dec(body.elementPng), shotError: body.shotError ?? null });
        const { text: _omit, ...rest } = r;
        console.log(`[feedback] ${r.ref} (${r.bytes} bytes)`);
        return send(200, { ...rest, rel: r.ref /* 旧クライアント互換 */ });
      }
      if (req.method === 'POST' && p === '/__feedback/attachments') {
        const files = (body?.files ?? []).map((f) => ({ name: String(f.name), mime: f.mime, data: new Uint8Array(Buffer.from(f.dataBase64 ?? '', 'base64')) }));
        return send(200, { ok: true, ...(await sink.putAttachments(files, { now: asDate(body?.now) })) });
      }
      if (req.method === 'POST' && p === '/__feedback/entries') {
        if (typeof body?.text !== 'string') return send(400, { ok: false, error: 'text が必要です' });
        return send(200, { ok: true, entry: await sink.putEntry({ text: redact(body.text), now: asDate(body?.now) }) });
      }
      if (req.method === 'GET' && p === '/__feedback/entries') return send(200, { ok: true, entries: await sink.list({ state: url.searchParams.get('state') ?? 'all' }) });
      const m = /^\/__feedback\/entries\/([^/]+)(\/archive)?$/.exec(p);
      if (m && req.method === 'GET' && !m[2]) return send(200, { ok: true, entry: await sink.read(decodeURIComponent(m[1])) });
      if (m && req.method === 'POST' && m[2]) return send(200, { ok: true, entry: await sink.archive(decodeURIComponent(m[1]), { note: body?.note }) });
      return send(404, { ok: false, error: 'not found' });
    } catch (err) {
      console.warn('[feedback] failed', err);
      return send(500, { ok: false, error: err instanceof Error ? err.message : String(err) });
    }
  };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const port = Number(process.env.FEEDBACK_PORT ?? 7373);
  // 実機のスマホから LAN 越しに送るときだけ FEEDBACK_HOST=0.0.0.0 にする（同じネットワークの誰でも書き込めるので、FEEDBACK_TOKEN を必ず設定し、
  // 作業中だけ・信頼できる LAN だけ。可能なら adb reverse tcp:7373 tcp:7373 や iOS シミュレータの localhost で済ませ、既定の 127.0.0.1 のままにする）
  const host = process.env.FEEDBACK_HOST ?? '127.0.0.1';
  if (host !== '127.0.0.1' && host !== 'localhost' && !process.env.FEEDBACK_TOKEN) {
    console.error('FEEDBACK_HOST を localhost 以外にするときは FEEDBACK_TOKEN が必要です');
    process.exit(1);
  }
  http.createServer(createHandler()).listen(port, host, () => console.log(`feedback receiver: http://${host}:${port}/__feedback`));
}
