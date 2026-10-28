import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createFileSink, feedbackDirProblem, nextFeedbackSeq, saveFeedbackFile, saveShots, MAX_ATTEMPTS } from './file-sink.mjs';

const tmp = () => fs.mkdtempSync(path.join(os.tmpdir(), 'fbfile-'));

test('採番: archived も数える・空ファイルも数える・日付が変われば 01', () => {
  const root = tmp();
  const dir = path.join(root, 'feedback');
  fs.mkdirSync(path.join(dir, 'archived'), { recursive: true });
  fs.writeFileSync(path.join(dir, '261023-01.md'), '');
  fs.writeFileSync(path.join(dir, 'archived', '261023-05.md'), 'x');
  fs.writeFileSync(path.join(dir, 'memo.md'), 'x');
  assert.equal(nextFeedbackSeq(dir, '261023'), 6);
  assert.equal(nextFeedbackSeq(dir, '261024'), 1);
});

test('保存: 相対パスを返し、上書きしない。競合したら番号を上げる', () => {
  const root = tmp();
  const now = new Date(2026, 9, 23, 10);
  const a = saveFeedbackFile({ repoRoot: root, text: 'a', now });
  assert.equal(a.rel, 'feedback/261023-01.md');
  const b = saveFeedbackFile({ repoRoot: root, text: 'b', now, beforeWrite: (f, n) => { if (n === 1) fs.writeFileSync(f, 'race'); } });
  assert.equal(b.name, '261023-03.md');
  assert.equal(b.attempts, 2);
  assert.equal(fs.readFileSync(path.join(root, 'feedback/261023-02.md'), 'utf8'), 'race');
});

test(`競合が ${MAX_ATTEMPTS} 回続いたら諦める`, () => {
  const root = tmp();
  assert.throws(() => saveFeedbackFile({ repoRoot: root, text: 'x', now: new Date(2026, 9, 23), beforeWrite: (f) => fs.writeFileSync(f, 'race') }), /回試しても/);
});

test('保存先: 絶対パス・..・外を指すシンボリックリンクは拒否', () => {
  assert.ok(feedbackDirProblem('/etc'));
  assert.ok(feedbackDirProblem('../x'));
  assert.equal(feedbackDirProblem('feedback'), null);
  const root = tmp();
  const outside = tmp();
  fs.symlinkSync(outside, path.join(root, 'link'));
  assert.throws(() => saveFeedbackFile({ repoRoot: root, dir: 'link/fb', text: 'x' }), /リポジトリの外/);
});

test('saveShots: window.png / element.png。空なら理由を返す', () => {
  const base = tmp();
  const r = saveShots({ windowPng: Buffer.from([1, 2, 3]), elementPng: Buffer.from([4]), baseDir: base, now: new Date(2026, 9, 23, 1, 2, 3) });
  assert.ok(r.window?.endsWith('20261023-010203/window.png') && fs.existsSync(r.element));
  assert.equal(saveShots({ windowPng: Buffer.alloc(0), baseDir: base }).error, 'スクリーンショットが空でした');
});

test('FileSink の id はパス区切りを含められない（list/read/archive）', async () => {
  const sink = createFileSink({ repoRoot: tmp() });
  await assert.rejects(() => sink.read('../../etc/passwd'), /不正/);
  await assert.rejects(() => sink.archive('261023-01/../x'), /不正/);
});
