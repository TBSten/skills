// node --test scripts/watch-inbox.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { listRecords, parseArgs, waitForNew } from './watch-inbox.mjs';

const SCRIPT = fileURLToPath(new URL('./watch-inbox.mjs', import.meta.url));
const tmp = () => fs.mkdtempSync(path.join(os.tmpdir(), 'fb-watch-'));
const run = (args, opts = {}) => new Promise((resolve) => {
  const p = spawn(process.execPath, [SCRIPT, ...args], opts);
  let out = '', err = '';
  p.stdout.on('data', (d) => (out += d)); p.stderr.on('data', (d) => (err += d));
  p.on('close', (code) => resolve({ code, out, err }));
  resolve.proc = p;
});

test('記録として数えるのは直下の yymmdd-xx.md だけ', () => {
  const d = tmp();
  for (const f of ['261120-01.md', '261120-100.md', 'HISTORY.md', '.gitkeep', '2611-01.md']) fs.writeFileSync(path.join(d, f), 'x');
  fs.mkdirSync(path.join(d, 'archived')); fs.writeFileSync(path.join(d, 'archived', '261119-01.md'), 'x');
  assert.deepEqual([...listRecords(d).keys()].sort(), ['261120-01.md', '261120-100.md']);
});

test('起動前からある記録は待たず、新しく来た記録だけを返す', async () => {
  const d = tmp();
  fs.writeFileSync(path.join(d, '261120-01.md'), 'old');
  const p = waitForNew({ dir: d, interval: 20, settle: 60 });
  setTimeout(() => fs.writeFileSync(path.join(d, '261120-02.md'), 'new'), 50);
  assert.deepEqual(await p, ['261120-02.md']);
});

test('0 バイトの間・書き込み中は待ち、書き終わってから返す', async () => {
  const d = tmp();
  const f = path.join(d, '261120-01.md');
  const p = waitForNew({ dir: d, interval: 20, settle: 150 });
  let done = false; p.then(() => (done = true));
  fs.writeFileSync(f, '');
  await new Promise((r) => setTimeout(r, 200));
  assert.equal(done, false, '0 バイトでは返さない');
  fs.writeFileSync(f, 'a');
  await new Promise((r) => setTimeout(r, 60));
  fs.appendFileSync(f, 'b');
  await new Promise((r) => setTimeout(r, 60));
  assert.equal(done, false, '大きさが変わり続ける間は返さない');
  assert.deepEqual(await p, ['261120-01.md']);
});

test('--any は起動時の未対応の記録も返す', async () => {
  const d = tmp();
  fs.writeFileSync(path.join(d, '261120-01.md'), 'old');
  assert.deepEqual(await waitForNew({ dir: d, any: true, interval: 20, settle: 0 }), ['261120-01.md']);
});

test('timeout なら null', async () => {
  assert.equal(await waitForNew({ dir: tmp(), interval: 20, settle: 0, timeout: 0.1 }), null);
});

test('parseArgs: 既定値・FEEDBACK_DIR・不正な値', () => {
  assert.deepEqual(parseArgs([], {}), { dir: 'feedback', any: false, interval: 2000, settle: 1500, timeout: null });
  assert.equal(parseArgs([], { FEEDBACK_DIR: '.local/feedback' }).dir, '.local/feedback');
  assert.deepEqual(parseArgs(['x', '--any', '--timeout', '5'], {}), { dir: 'x', any: true, interval: 2000, settle: 1500, timeout: 5 });
  assert.throws(() => parseArgs(['--interval', 'abc'], {}), /0 以上の数/);
  assert.throws(() => parseArgs(['--nope'], {}), /知らないオプション/);
});

test('cli: NEW <id> <パス> を出して 0 で終わる', async () => {
  const d = tmp();
  const r = run([d, '--interval', '50', '--settle', '100']);
  setTimeout(() => fs.writeFileSync(path.join(d, '261120-03.md'), 'hello'), 300);
  const { code, out } = await r;
  assert.equal(code, 0);
  assert.equal(out.trim(), `NEW 261120-03 ${path.join(d, '261120-03.md')}`);
});

test('cli: 置き場が無ければ 1、timeout なら TIMEOUT と 2', async () => {
  const missing = await run([path.join(tmp(), 'nope')]);
  assert.equal(missing.code, 1);
  assert.match(missing.err, /置き場が見つかりません/);
  const t = await run([tmp(), '--interval', '20', '--timeout', '0.2']);
  assert.equal(t.code, 2);
  assert.equal(t.out.trim(), 'TIMEOUT');
});
