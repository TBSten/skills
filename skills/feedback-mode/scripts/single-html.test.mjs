// node --test scripts/single-html.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { extract, replaceData, validate, TEMPLATE } from './single-html.mjs';

const CLI = fileURLToPath(new URL('./single-html.mjs', import.meta.url));
const EXAMPLE = path.join(path.dirname(TEMPLATE), 'example.data.json');
const run = (...args) => spawnSync(process.execPath, [CLI, ...args], { encoding: 'utf8' });
const tmp = () => fs.mkdtempSync(path.join(os.tmpdir(), 'fb-sh-'));

test('ひな形のデータは検査に通り、例のデータも通る', () => {
  assert.deepEqual(validate(JSON.parse(extract(fs.readFileSync(TEMPLATE, 'utf8')).text)), []);
  assert.deepEqual(validate(JSON.parse(fs.readFileSync(EXAMPLE, 'utf8'))), []);
});

test('差し替えは `<` を逃がし、</script> を含む文でも切れない', () => {
  const html = replaceData(fs.readFileSync(TEMPLATE, 'utf8'), { title: 'a </script><b>', sections: [] });
  const { text } = extract(html);
  assert.ok(!text.includes('</script>'));
  assert.equal(JSON.parse(text).title, 'a </script><b>');
});

test('検査: よくある間違いを場所つきで返す', () => {
  const errs = validate({ title: '', sections: [{ id: 'a b', title: 'x', blocks: [{ type: 'chart' }, { type: 'table', columns: [], rows: [] }, { type: 'table', columns: [{ key: 'k', label: 'K' }], rows: [null, ['x']] }] }, { id: 'dup', title: 'y' }, { id: 'dup', title: 'z' }],
    questions: [{ id: 'q1', text: 't', options: [{ value: 'a', label: 'A' }, { value: 'b', label: 'B' }], recommend: 'c' }] });
  const has = (re) => assert.ok(errs.some((e) => re.test(e)), `${re} が無い: ${errs.join(' | ')}`);
  has(/^title:/); has(/^sections\[0\]\.id:/); has(/^sections\[0\]\.blocks\[0\]\.type:.*chart/); has(/^sections\[0\]\.blocks\[1\]\.columns:/); has(/^sections\[0\]\.blocks\[2\]\.rows\[0\]:/); has(/^sections\[0\]\.blocks\[2\]\.rows\[1\]:/); has(/^sections\[2\]\.id:.*重複/); has(/^questions\[0\]\.recommend:/);
});

test('cli: new → get → set → check', () => {
  const d = tmp(); const f = path.join(d, 'report.html');
  assert.equal(run('new', f, '--data', EXAMPLE).status, 0);
  assert.equal(run('new', f).status, 1, '既にあれば中断');
  const got = JSON.parse(run('get', f).stdout);
  assert.equal(got.file, 'report.html');
  got.sections[0].title = '1. 概要（直した）';
  const j = path.join(d, 'data.json'); fs.writeFileSync(j, JSON.stringify(got));
  assert.equal(run('set', f, j).status, 0);
  assert.equal(JSON.parse(run('get', f).stdout).sections[0].title, '1. 概要（直した）');
  assert.equal(run('check', f).status, 0);
});

test('cli: 検査に落ちるデータは書かない・file の名前違いを指摘する', () => {
  const d = tmp(); const f = path.join(d, 'report.html');
  run('new', f, '--data', EXAMPLE);
  const before = fs.readFileSync(f, 'utf8');
  const bad = path.join(d, 'bad.json'); fs.writeFileSync(bad, JSON.stringify({ title: 'x', sections: 'nope' }));
  const r = run('set', f, bad);
  assert.equal(r.status, 1); assert.match(r.stderr, /sections: 配列/);
  assert.equal(fs.readFileSync(f, 'utf8'), before);
  const g = path.join(d, 'other.html'); fs.copyFileSync(f, g);
  assert.match(run('check', g).stderr, /ファイル名「other.html」と違います/);
  assert.equal(run('nope').status, 2);
});
