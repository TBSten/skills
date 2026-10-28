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

test('採番と書き込みの間に、別のプロセスが同じ番号を保存して archived/ へ移しても、番号が重ならない', () => {
  const root = tmp();
  const now = new Date(2026, 9, 23, 10);
  saveFeedbackFile({ repoRoot: root, text: 'a', now });
  const arch = path.join(root, 'feedback', 'archived');
  // 1 回目: 数えた直後に 02 が archived に現れる（書く前に気づく）
  const b = saveFeedbackFile({ repoRoot: root, text: 'b', now, beforeWrite: (f, n) => { if (n === 1) { fs.mkdirSync(arch, { recursive: true }); fs.writeFileSync(path.join(arch, path.basename(f)), 'other'); } } });
  assert.equal(b.name, '261023-03.md');
  assert.equal(fs.readFileSync(path.join(arch, '261023-02.md'), 'utf8'), 'other');
  assert.ok(!fs.existsSync(path.join(root, 'feedback', '261023-02.md')));
  // 2 回目: 確かめた後・書いた直後に 04 が archived に現れる（書いた後に気づき、自分の分を消して数え直す）
  const c = saveFeedbackFile({ repoRoot: root, text: 'c', now, afterWrite: (f, n) => { if (n === 1) fs.writeFileSync(path.join(arch, path.basename(f)), 'other2'); } });
  assert.equal(c.name, '261023-05.md');
  assert.equal(c.attempts, 2);
  assert.ok(!fs.existsSync(path.join(root, 'feedback', '261023-04.md')));
  const ids = [...fs.readdirSync(path.join(root, 'feedback')), ...fs.readdirSync(arch)].filter((n) => n.endsWith('.md'));
  assert.equal(new Set(ids).size, ids.length);
});

test('writeDoc / readDoc: 置き場の中に上書きで書き、tmp を残さない。記録には数えない', async () => {
  const root = tmp();
  const sink = createFileSink({ repoRoot: root });
  await sink.writeDoc('memo/ja/a.md', 'v1');
  await sink.writeDoc('memo/ja/a.md', 'v2');
  assert.equal(await sink.readDoc('memo/ja/a.md'), 'v2');
  assert.equal(fs.readFileSync(path.join(root, 'feedback/memo/ja/a.md'), 'utf8'), 'v2');
  assert.deepEqual(fs.readdirSync(path.join(root, 'feedback/memo/ja')), ['a.md']);
  assert.equal(await sink.readDoc('memo/none.md'), null);
  assert.deepEqual(await sink.list(), []);
  const outside = tmp();
  fs.symlinkSync(outside, path.join(root, 'feedback', 'link'));
  await assert.rejects(() => sink.writeDoc('link/x.md', 'x'), /外/);
  assert.deepEqual(fs.readdirSync(outside), []);
});
