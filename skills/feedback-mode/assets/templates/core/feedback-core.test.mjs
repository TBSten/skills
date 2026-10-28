import test from 'node:test';
import assert from 'node:assert/strict';
import { buildFeedbackMarkdown, cropRect, feedbackDay, nextSeqFromIds, normalizeOptions, redact, submitFeedback, assertSink, kindOf, parseRecordFrontmatter, safeDocPath } from './feedback-core.mjs';
import { createMemorySink } from './memory-sink.mjs';

test('feedbackDay: ローカル日付の yymmdd', () => assert.equal(feedbackDay(new Date(2026, 9, 23)), '261023'));

test('採番の規則: 未対応と対応済みの id を合わせた同じ日付の最大 + 1。ほかの日付・別の形は数えない', () => {
  assert.equal(nextSeqFromIds(['261023-01', '261023-05', '261022-09', 'memo', '261023-x'], '261023'), 6);
  assert.equal(nextSeqFromIds([], '261023'), 1);
  assert.equal(nextSeqFromIds(['261023-99'], '261024'), 1);
});

test('redact: Bearer・Cookie・token=・sk-', () => {
  const s = redact('Authorization: Bearer abcdefghij1234\nCookie: a=b; c=d\ntoken=abcdef123456 key sk-abcdefghijklmnopqrstuv');
  assert.ok(!/abcdefghij1234|a=b|abcdef123456|sk-abcdef/.test(s));
});

test('cropRect: dpr を掛けて画像の中に収める。外なら null', () => {
  assert.deepEqual(cropRect({ x: 100, y: 100, width: 50, height: 20 }, { width: 500, height: 400 }, { width: 1000, height: 800 }), { x: 152, y: 152, width: 196, height: 136 });
  assert.equal(cropRect({ x: 900, y: 900, width: 10, height: 10 }, { width: 500, height: 400 }, { width: 1000, height: 800 }), null);
});

const payload = {
  appName: 'Demo', pointer: 'ソースは src/', comment: '',
  target: { screen: 'home', path: 'ヘッダ/保存', element: 'button「保存」', rect: { x: 1, y: 2, width: 3, height: 4 }, state: { disabled: true } },
  shots: { window: '/tmp/w.png', element: null }, env: { os: 'x' },
  logs: [{ ts: '10:00:00', level: 'error', text: 'boom token=abcdef123456' }],
};

test('Markdown: 既定の節・オフ表示・redact・文が空', () => {
  const md = buildFeedbackMarkdown(payload);
  assert.match(md, /（オプションでオフ: 環境・今の画面・画面に出ている情報）/);
  assert.match(md, /（書いていません）/);
  assert.ok(!md.includes('abcdef123456'));
  assert.ok(!md.includes('## 環境'));
  const all = buildFeedbackMarkdown(payload, { env: true });
  assert.match(all, /## 環境/);
  assert.equal(buildFeedbackMarkdown(payload, { env: true }), all); // コピーと保存で同じ文: 同じ入力・同じ options なら同一
  assert.deepEqual(normalizeOptions({ target: 'x', log: false }), { target: true, log: false, shots: true, env: false, screen: false, view: false });
});

test('submitFeedback: 添付 → 参照が文に入る → 置き場に保存。スクショなし・オフの扱い', async () => {
  const sink = createMemorySink();
  const now = new Date(2026, 9, 23, 10);
  const r = await submitFeedback({ sink, payload, options: {}, windowPng: new Uint8Array([1, 2]), elementPng: new Uint8Array([3]), now });
  assert.equal(r.id, '261023-01');
  assert.match(r.text, /memory:attach\/1\/window\.png/);
  assert.equal((await sink.read('261023-01')).text, r.text);
  const r2 = await submitFeedback({ sink, payload, now }); // 画像なし
  assert.equal(r2.id, '261023-02');
  assert.match(r2.text, /撮れませんでした（スクリーンショットが空でした）/);
  const r3 = await submitFeedback({ sink, payload, options: { shots: false }, windowPng: new Uint8Array([1]), now });
  assert.ok(!r3.text.includes('## スクリーンショット'));
  assert.equal(sink.attachments.size, 2); // 添付は 1 回目の 2 枚だけ（2 回目は画像なし、3 回目は shots オフで撮らない）
});

test('assertSink: 足りないメソッドは投げる', () => {
  assert.throws(() => assertSink({ kind: 'x' }), /putAttachments/);
  assert.doesNotThrow(() => assertSink(createMemorySink()));
});

test('schema 1: meta を渡したときだけ frontmatter。渡さなければ旧形と同じ文（後方互換）', () => {
  const old = buildFeedbackMarkdown(payload);
  assert.ok(old.startsWith('# Demo: 画面へのフィードバック'));
  const md = buildFeedbackMarkdown(payload, {}, { meta: { created: new Date(Date.UTC(2026, 9, 23)), dirty: true, stale: true } });
  const fm = parseRecordFrontmatter(md);
  assert.equal(fm.schema, 1);
  assert.equal(fm.kind, 'element');
  assert.equal(fm.screen, 'home');
  assert.equal(fm.target, 'ヘッダ/保存');
  assert.equal(fm.dirty, true);
  assert.equal(fm.stale, true);
  assert.deepEqual(fm.omitted, ['env', 'screen', 'view']); // 載せなかった節は frontmatter と本文の 1 行の両方
  assert.match(md, /（オプションでオフ: 環境・今の画面・画面に出ている情報）/);
  assert.match(md, /注意: 画面を開いた後にソースが変わっています/);
  // frontmatter を外せば、本文は旧形と同じ並び（注意の 1 行を除く）
  const body = md.replace(/^---\n[\s\S]*?\n---\n\n/, '').replace(/注意: [^\n]*\n\n/, '');
  assert.equal(body, old);
});

test('schema 1: dirty / stale は分からなければ書かない（false と書かない）', () => {
  const fm = parseRecordFrontmatter(buildFeedbackMarkdown(payload, {}, { meta: {} }));
  assert.ok(!('dirty' in fm) && !('stale' in fm));
});

test('parseRecordFrontmatter: frontmatter の無い旧形・手書きのメモは schema 0', () => {
  assert.deepEqual(parseRecordFrontmatter('# MyApp: 画面へのフィードバック\n\n本文'), { schema: 0 });
  assert.deepEqual(parseRecordFrontmatter('ボタンが押せない（手書き）'), { schema: 0 });
  assert.equal(parseRecordFrontmatter('---\npage: /x\n---\n').schema, 0); // schema の無い frontmatter も旧形
});

test('kindOf: kind が無ければ target の有無から推す', () => {
  assert.equal(kindOf({ target: {} }), 'element');
  assert.equal(kindOf({}), 'page');
  assert.equal(kindOf({ kind: 'text' }), 'text');
  assert.equal(kindOf({ kind: 'bogus', target: {} }), 'element');
});

test('submitFeedback: meta を渡すと created と添付の参照が frontmatter に入る', async () => {
  const sink = createMemorySink();
  const r = await submitFeedback({ sink, payload, windowPng: new Uint8Array([1]), now: new Date(2026, 9, 23, 10), meta: { stale: false } });
  const fm = parseRecordFrontmatter((await sink.read(r.id)).text);
  assert.equal(fm.schema, 1);
  assert.equal(fm.stale, false);
  assert.deepEqual(fm.attachments, ['memory:attach/1/window.png']);
});

test('safeDocPath: 正規形にし、外に出るものは投げる', () => {
  assert.equal(safeDocPath('./memo//ja/a.md'), 'memo/ja/a.md');
  for (const bad of ['', '.', '../x', 'a/../../x', '/etc/passwd', 'C:/x', 'a\\b', 'x\0y', 1]) assert.throws(() => safeDocPath(bad));
});

test('選んだ位置: XPath・テキストフラグメント・ソースの行があれば書く。ソースの行は frontmatter にも', () => {
  const p2 = { ...payload, target: { ...payload.target, xpath: '//*[@id="save"]', fragment: 'http://localhost/x#:~:text=%E4%BF%9D%E5%AD%98', source: 'src/pages/x.md:12-14' } };
  const md = buildFeedbackMarkdown(p2, {}, { meta: {} });
  assert.match(md, /- XPath: `\/\/\*\[@id="save"\]`/);
  assert.match(md, /- テキストフラグメント URL: http:\/\/localhost\/x#:~:text=/);
  assert.match(md, /- ソースの行: `src\/pages\/x\.md:12-14`/);
  assert.equal(parseRecordFrontmatter(md).source, 'src/pages/x.md:12-14');
  assert.ok(!buildFeedbackMarkdown(payload).includes('XPath')); // 無ければ書かない（旧形の文は変わらない）
});
