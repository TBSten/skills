#!/usr/bin/env node
// HTML の section[id] ごとに PNG を撮る。light / dark の 2 枚ずつ。
//
// 使い方:
//   node capture.mjs <html> <out-dir> [--width=420] [--scale=3] [--schemes=light,dark]
//
// 出力: 1 行目に OK、続けて撮った PNG のパスを JSON 配列で出す。

import { existsSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { resolve, join } from 'node:path';
import { pathToFileURL } from 'node:url';

function fail(msg) {
  console.error(`ERROR: ${msg}`);
  process.exit(1);
}

const positional = [];
const opts = { width: 420, scale: 3, schemes: ['light', 'dark'] };
for (const a of process.argv.slice(2)) {
  const m = a.match(/^--(width|scale|schemes)=(.+)$/);
  if (m) {
    if (m[1] === 'schemes') opts.schemes = m[2].split(',').map((s) => s.trim()).filter(Boolean);
    else opts[m[1]] = Number(m[2]);
  } else if (a.startsWith('--')) {
    fail(`不明なオプション: ${a} (使えるのは --width= / --scale= / --schemes=)`);
  } else {
    positional.push(a);
  }
}
if (positional.length !== 2) {
  fail('引数が足りない。使い方: node capture.mjs <html> <out-dir> [--width=420] [--scale=3] [--schemes=light,dark]');
}
const [htmlArg, outArg] = positional;
const html = resolve(htmlArg);
const outDir = resolve(outArg);
if (!existsSync(html)) fail(`HTML が見つからない: ${html}`);
if (!Number.isFinite(opts.width) || opts.width <= 0) fail('--width は正の数で指定する');
if (!Number.isFinite(opts.scale) || opts.scale <= 0) fail('--scale は正の数で指定する');
for (const s of opts.schemes) {
  if (s !== 'light' && s !== 'dark') fail(`--schemes には light / dark だけ指定できる: ${s}`);
}

// playwright はカレント → グローバルの順に探す
let chromium;
try {
  const require = createRequire(join(process.cwd(), 'noop.js'));
  ({ chromium } = require('playwright'));
} catch {
  try {
    ({ chromium } = await import('playwright'));
  } catch {
    fail('playwright が見つからない。`npm i -D playwright && npx playwright install chromium` を実行してから再実行する');
  }
}

mkdirSync(outDir, { recursive: true });
const browser = await chromium.launch();
const written = [];
try {
  for (const scheme of opts.schemes) {
    const page = await browser.newPage({
      viewport: { width: opts.width, height: 800 },
      deviceScaleFactor: opts.scale,
      colorScheme: scheme,
    });
    await page.goto(pathToFileURL(html).href);
    await page.evaluate(() => document.fonts && document.fonts.ready);
    const sections = await page.$$('section');
    if (sections.length === 0) fail('HTML に <section> が無い。1 枚にしたい単位を <section id="..."> で囲む');
    for (const el of sections) {
      const id = await el.getAttribute('id');
      if (!id) fail('id の無い <section> がある。ファイル名になるので <section id="01-before-after"> のように付ける');
      const path = join(outDir, `${id}-${scheme}.png`);
      await el.screenshot({ path });
      written.push(path);
    }
    await page.close();
  }
} finally {
  await browser.close();
}

console.log('OK');
console.log(JSON.stringify(written, null, 2));
