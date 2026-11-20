// node --test scripts/watch-inbox.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import http from 'node:http';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { parseArgs, createWatcher } from './watch-inbox.mjs';
import { createHandler } from '../assets/templates/core/feedback-dev-server.mjs';
import { createMemorySink } from '../assets/templates/core/memory-sink.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const tmp = () => fs.mkdtempSync(path.join(os.tmpdir(), 'fb-watch-'));
// 時計を進められる watcher
const make = (o) => { let t = 0; const lines = []; const w = createWatcher({ interval: 10, settle: 100, ...o }, { out: (s) => lines.push(s), err: () => {}, now: () => t }); return { w, lines, advance: (ms) => { t += ms; } }; };

test('引数: ディレクトリか --url のどちらか 1 つ', () => {
  assert.throws(() => parseArgs([]));
  assert.throws(() => parseArgs(['a', '--url', 'http://x']));
  assert.equal(parseArgs(['--url', 'http://x/__feedback/']).url, 'http://x/__feedback');
  assert.throws(() => parseArgs(['a', '--nope']));
});

test('起動時にある記録は出さず、あとから増えた記録だけを大きさが落ち着いてから 1 回出す', async () => {
  const dir = tmp();
  fs.writeFileSync(path.join(dir, '261120-01.md'), 'old');
  const { w, lines, advance } = make({ dir });
  await w.tick({ initial: true });
  fs.writeFileSync(path.join(dir, '261120-02.md'), 'new');
  fs.writeFileSync(path.join(dir, 'HISTORY.md'), 'x');          // 記録ではない
  fs.mkdirSync(path.join(dir, 'archived'));
  fs.writeFileSync(path.join(dir, 'archived', '261120-03.md'), 'x'); // archived は数えない
  assert.deepEqual(await w.tick(), []);                           // 初めて見た回は待つ
  advance(150);
  assert.deepEqual((await w.tick()).map((r) => r.id), ['261120-02']);
  advance(150);
  assert.deepEqual(await w.tick(), []);                           // 二度は出さない
  assert.equal(lines.length, 1);
  assert.match(lines[0], /^NEW 261120-02 /);
});

test('書きかけ（0 バイト・大きさが変わっている）は出さない', async () => {
  const dir = tmp();
  const { w, advance } = make({ dir });
  await w.tick({ initial: true });
  const f = path.join(dir, '261120-01.md');
  fs.writeFileSync(f, '');
  await w.tick(); advance(150);
  assert.deepEqual(await w.tick(), []);   // 0 バイトのまま
  fs.writeFileSync(f, 'abc');
  await w.tick(); advance(50);
  fs.writeFileSync(f, 'abcdef');
  assert.deepEqual(await w.tick(), []);   // 大きさが変わった
  advance(150);
  assert.equal((await w.tick()).length, 1);
});

test('--include-existing は起動時の記録も出す。--json', async () => {
  const dir = tmp();
  fs.writeFileSync(path.join(dir, '261120-01.md'), 'old');
  const { w, lines, advance } = make({ dir, includeExisting: true, json: true });
  await w.tick({ initial: true }); await w.tick(); advance(150); await w.tick();
  assert.equal(JSON.parse(lines[0]).id, '261120-01');
  assert.equal(JSON.parse(lines[0]).event, 'new');
});

test('--exec は 1 件ずつ順番に、FEEDBACK_ID / FEEDBACK_FILE を渡して実行する', async () => {
  const dir = tmp();
  const log = path.join(tmp(), 'log.txt');
  const { w, advance } = make({ dir, exec: `echo "$FEEDBACK_ID $(basename "$FEEDBACK_FILE")" >> "${log}"` });
  await w.tick({ initial: true });
  fs.writeFileSync(path.join(dir, '261120-02.md'), 'b');
  fs.writeFileSync(path.join(dir, '261120-01.md'), 'a');
  await w.tick(); advance(150); await w.tick();
  assert.equal(fs.readFileSync(log, 'utf8'), '261120-01 261120-01.md\n261120-02 261120-02.md\n');
  assert.equal(w.failed(), false);
});

test('--url: HTTP の受け口の open の一覧を見る', async () => {
  const sink = createMemorySink();
  const server = http.createServer(createHandler({ sink })).listen(0, '127.0.0.1');
  await new Promise((r) => server.once('listening', r));
  try {
    const url = `http://127.0.0.1:${server.address().port}/__feedback`;
    const { w, lines, advance } = make({ url });
    await w.tick({ initial: true });
    const e = await sink.putEntry({ text: '# x', now: new Date() });
    await w.tick(); advance(150); await w.tick();
    assert.deepEqual(lines, [`NEW ${e.id} ${e.ref}`]);
  } finally { server.close(); }
});

test('cli: --once で 1 件見つけたら終わる', async () => {
  const dir = tmp();
  const p = spawn(process.execPath, [path.join(here, 'watch-inbox.mjs'), dir, '--once', '--interval', '50', '--settle', '50']);
  let out = '';
  p.stdout.on('data', (d) => { out += d; });
  await new Promise((r) => setTimeout(r, 300));
  fs.writeFileSync(path.join(dir, '261120-01.md'), 'x');
  const code = await new Promise((r) => p.on('exit', r));
  assert.equal(code, 0);
  assert.match(out, /^NEW 261120-01 /);
});

test('cli: --timeout まで何も来なければ TIMEOUT を出して 3 で終わる', async () => {
  const dir = tmp();
  const p = spawn(process.execPath, [path.join(here, 'watch-inbox.mjs'), dir, '--timeout', '0.3', '--interval', '50']);
  let out = '';
  p.stdout.on('data', (d) => { out += d; });
  const code = await new Promise((r) => p.on('exit', r));
  assert.equal(code, 3);
  assert.equal(out, 'TIMEOUT\n');
});
