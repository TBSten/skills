/**
 * Sink の契約テスト: どの置き場でも、同じ操作で同じ結果になること（採番・保存・添付・一覧・読み・アーカイブ）。
 * 新しい Sink（Issue トラッカー等）を足したら、ここに makeSink を 1 つ足すだけで同じ契約を確かめられる。
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { createMemorySink } from './memory-sink.mjs';
import { createFileSink } from './file-sink.mjs';
import { createHttpSink } from './http-sink.mjs';
import { createHandler } from '../web/feedback-dev-server.mjs';
import { submitFeedback } from './feedback-core.mjs';

const tmp = () => fs.mkdtempSync(path.join(os.tmpdir(), 'fbsink-'));

async function withHttp(token) {
  const sink = createFileSink({ repoRoot: tmp() });
  const srv = http.createServer(createHandler({ sink, token }));
  await new Promise((r) => srv.listen(0, '127.0.0.1', r));
  const baseUrl = `http://127.0.0.1:${srv.address().port}`;
  return { sink, baseUrl, close: () => new Promise((r) => srv.close(r)) };
}

const kinds = {
  memory: async () => ({ sink: createMemorySink(), close: async () => {} }),
  file: async () => ({ sink: createFileSink({ repoRoot: tmp() }), close: async () => {} }),
  http: async () => {
    const h = await withHttp('secret-token');
    return { sink: createHttpSink({ baseUrl: h.baseUrl, token: 'secret-token' }), close: h.close };
  },
};

for (const [kind, make] of Object.entries(kinds)) {
  test(`Sink 契約 [${kind}]: 採番・添付・一覧・読み・アーカイブ・番号の再利用なし`, async () => {
    const { sink, close } = await make();
    try {
      const d1 = new Date(2026, 9, 23, 10);
      const a = await sink.putEntry({ text: 'one', now: d1 });
      const b = await sink.putEntry({ text: 'two', now: d1 });
      assert.deepEqual([a.id, b.id], ['261023-01', '261023-02']);
      assert.equal(a.state, 'open');
      assert.deepEqual((await sink.list({ state: 'open' })).map((e) => e.id), ['261023-01', '261023-02']);
      assert.equal((await sink.read('261023-02')).text, 'two');
      const arch = await sink.archive('261023-02', { note: '対応した' });
      assert.equal(arch.state, 'archived');
      assert.deepEqual((await sink.list({ state: 'open' })).map((e) => e.id), ['261023-01']);
      assert.deepEqual((await sink.list({ state: 'archived' })).map((e) => e.id), ['261023-02']);
      assert.match((await sink.read('261023-02')).text, /対応メモ: 対応した/);
      // archived の番号も数える（再利用しない）。日付が変われば 01
      assert.equal((await sink.putEntry({ text: 'three', now: d1 })).id, '261023-03');
      assert.equal((await sink.putEntry({ text: 'next day', now: new Date(2026, 9, 24, 9) })).id, '261024-01');
      const at = await sink.putAttachments([{ name: 'window.png', data: new Uint8Array([1, 2, 3]), mime: 'image/png' }, { name: 'element.png', data: new Uint8Array([4]) }], { now: d1 });
      assert.ok(at.refs['window.png'] && at.refs['element.png'] && !at.error);
      await assert.rejects(() => sink.read('261099-99'), /見つかりません|HTTP|not/);
      // 通し: submitFeedback（スクショの参照が文に入る）
      const r = await submitFeedback({ sink, payload: { appName: 'T', comment: 'hi token=abcdef123456', target: { path: 'p' } }, windowPng: new Uint8Array([9]), now: d1 });
      assert.match(r.text, /window\.png/);
      assert.ok(!(await sink.read(r.id)).text.includes('abcdef123456'));
    } finally {
      await close();
    }
  });
}

test('HTTP Sink: トークンが無い・違うと拒否される。受け口の裏のファイルにも保存される', async () => {
  const h = await withHttp('t0k3n-value');
  try {
    await assert.rejects(() => createHttpSink({ baseUrl: h.baseUrl }).list(), /token required/);
    await assert.rejects(() => createHttpSink({ baseUrl: h.baseUrl, token: 'wrong' }).list(), /token required/);
    const remote = createHttpSink({ baseUrl: h.baseUrl, token: 't0k3n-value' });
    const e = await remote.putEntry({ text: 'x', now: new Date(2026, 9, 23) });
    assert.equal((await h.sink.read(e.id)).text, 'x'); // 裏のファイルを直接読める（Agent は開発機でそのまま読んでよい）
  } finally {
    await h.close();
  }
});
