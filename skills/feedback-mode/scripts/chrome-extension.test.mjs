// chrome-extension.mjs の node:test。node --test scripts/chrome-extension.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { check, create, sync, VENDOR } from './chrome-extension.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const tmp = () => path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'fb-ext-test-')), 'ext');
const read = (d, f) => fs.readFileSync(path.join(d, f), 'utf8');
const run = (...a) => spawnSync(process.execPath, [path.join(here, 'chrome-extension.mjs'), ...a], { encoding: 'utf8' });

test('new: ひな形と vendor を置き、名前を入れる。検査に通る', () => {
  const d = tmp();
  create(d, { name: '社内ツールのフィードバック' });
  for (const f of ['manifest.json', 'background.js', 'content.js', ...Object.keys(VENDOR)]) assert.ok(fs.existsSync(path.join(d, f)), f);
  assert.equal(JSON.parse(read(d, 'manifest.json')).name, '社内ツールのフィードバック');
  assert.deepEqual(check(d), { errors: [], warnings: [] });
});

test('new: 空でないディレクトリには書かない（--force で上書き）', () => {
  const d = tmp();
  fs.mkdirSync(d, { recursive: true });
  fs.writeFileSync(path.join(d, 'memo.txt'), 'x');
  assert.throws(() => create(d), /空ではありません/);
  assert.equal(run('new', d).status, 1);
  assert.equal(run('new', d, '--force').status, 0);
});

test('sync: vendor だけを skill の版に戻し、人が書き換えた content.js・manifest.json は残す', () => {
  const d = tmp();
  create(d);
  fs.writeFileSync(path.join(d, 'content.js'), '// 自分で書き換えた\n');
  const m = JSON.parse(read(d, 'manifest.json')); m.name = '自分の名前'; fs.writeFileSync(path.join(d, 'manifest.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(d, 'vendor/feedback-mode.js'), '// 古い版\n');
  assert.match(check(d).warnings.join('\n'), /vendor\/feedback-mode\.js: skill の版と違います/);
  assert.deepEqual(sync(d), ['vendor/feedback-mode.js']);
  assert.deepEqual(sync(d), [], '2 回目は何も変えない');
  assert.equal(read(d, 'content.js'), '// 自分で書き換えた\n');
  assert.equal(JSON.parse(read(d, 'manifest.json')).name, '自分の名前');
  assert.equal(run('sync', tmp()).status, 1, 'manifest.json が無ければ中断');
});

test('check: 足りない権限・vendor の読み込み口・広すぎる権限を言う', () => {
  const d = tmp();
  create(d);
  const m = JSON.parse(read(d, 'manifest.json'));
  m.permissions = ['activeTab'];
  m.web_accessible_resources = [];
  m.host_permissions = ['<all_urls>'];
  fs.writeFileSync(path.join(d, 'manifest.json'), JSON.stringify(m));
  const { errors, warnings } = check(d);
  assert.ok(errors.some((e) => /scripting/.test(e)) && errors.some((e) => /clipboardWrite/.test(e)) && errors.some((e) => /web_accessible_resources/.test(e)), errors.join('\n'));
  assert.ok(warnings.some((w) => /<all_urls>/.test(w)));
  assert.equal(run('check', d).status, 1);
  fs.rmSync(path.join(d, 'vendor'), { recursive: true });
  assert.ok(check(d).errors.some((e) => /vendor\/feedback-core\.mjs: ありません/.test(e)));
});

test('cli: 使い方の誤りは 2', () => {
  assert.equal(run().status, 2);
  assert.equal(run('build', 'x').status, 2);
});
