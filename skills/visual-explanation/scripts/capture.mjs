#!/usr/bin/env node
// HTML の section[id] ごとに PNG を撮る。light / dark の 2 枚ずつ。
// Chrome / Chromium の headless コマンドだけを使う (npm パッケージは要らない)。
//
// 使い方:
//   node capture.mjs <html> <out-dir> [--width=420] [--scale=3] [--schemes=light,dark]
//
// Chrome の場所は環境変数 CHROME_PATH で指定できる (無ければよくある場所を探す)。
//
// 出力: 1 行目に OK、続けて撮った PNG のパスを JSON 配列で出す。
//       見出しの数・長さ、横のはみ出しがあれば、その後に `WARN: ...` を 1 行ずつ出す。

import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, unlinkSync, writeFileSync } from 'node:fs';
import { basename, dirname, join, resolve } from 'node:path';
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

// --- Chrome を探す ---------------------------------------------------------
function onPath(cmd) {
  const r = spawnSync('sh', ['-c', `command -v "${cmd}"`], { encoding: 'utf8' });
  return r.status === 0 ? r.stdout.trim() : null;
}
function findChrome() {
  if (process.env.CHROME_PATH) {
    if (!existsSync(process.env.CHROME_PATH)) fail(`CHROME_PATH のファイルが無い: ${process.env.CHROME_PATH}`);
    return process.env.CHROME_PATH;
  }
  for (const c of ['google-chrome', 'google-chrome-stable', 'chromium', 'chromium-browser', 'chrome']) {
    const p = onPath(c);
    if (p) return p;
  }
  for (const p of [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/opt/pw-browsers/chromium/chrome-linux64/chrome',
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  ]) {
    if (existsSync(p)) return p;
  }
  return null;
}
const chrome = findChrome();
if (!chrome) {
  fail('Chrome / Chromium が見つからない。インストールするか、実行ファイルのパスを CHROME_PATH に入れて再実行する');
}

const baseFlags = ['--headless', '--disable-gpu', '--hide-scrollbars', '--no-first-run', '--no-default-browser-check', '--virtual-time-budget=3000'];
if (typeof process.getuid === 'function' && process.getuid() === 0) baseFlags.push('--no-sandbox');

function runChrome(args) {
  const r = spawnSync(chrome, [...baseFlags, ...args], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: 60000 });
  if (r.error) fail(`Chrome を起動できない: ${r.error.message}`);
  if (r.status !== 0) fail(`Chrome が失敗した (終了コード ${r.status}):\n${(r.stderr || '').trim().split('\n').slice(-5).join('\n')}`);
  return r.stdout;
}

// --- 計測用のスクリプトを差し込んだ一時コピーを作る -----------------------
// 元の HTML と同じ場所に置くので、相対パスの画像・CSS もそのまま読める。
// URL の #<id> で撮る section 以外を隠す。#measure-<幅> なら全 section を計測して <meta> に書く。
const probe = `
<script>
(() => {
  const secs = [...document.querySelectorAll('section')];
  const target = decodeURIComponent(location.hash.slice(1));
  const showOnly = (id) => secs.forEach((s) => { s.style.display = s.id === id ? '' : 'none'; });
  const mm = target.match(/^measure-(\\d+)$/);
  if (!mm) { showOnly(target); return; }
  // --dump-dom では窓の幅が最小幅 (500px 前後) に丸められることがあるので、撮る幅を html に直接当てて測る
  const W = Number(mm[1]);
  document.documentElement.style.width = W + 'px';
  const left = document.documentElement.getBoundingClientRect().left;
  const info = { sections: [], warn: [] };
  for (const s of secs) {
    const id = s.id || '';
    showOnly(id);
    const height = Math.ceil(s.getBoundingClientRect().bottom + window.scrollY);
    const h2 = s.querySelectorAll('h2');
    if (h2.length !== 1) info.warn.push(id + ': <h2> が ' + h2.length + ' 個ある。1 枚 1 メッセージの見出しを 1 つだけ書く');
    else {
      const t = h2[0].textContent.trim().replace(/\\s+/g, ' ');
      const n = [...t].length;
      if (n > 40) info.warn.push(id + ': 見出しが ' + n + ' 文字ある (最大 40 文字)。「' + t + '」を短くする');
    }
    let over = false;
    for (const el of s.querySelectorAll('*')) {
      const r = el.getBoundingClientRect();
      if (r.right - left > W + 1) { over = true; break; }
      const cs = getComputedStyle(el);
      if (cs.overflowX !== 'visible' && el.scrollWidth > el.clientWidth + 1) { over = true; break; }
    }
    if (over) info.warn.push(id + ': 幅 ' + W + 'px から中身がはみ出している。折り返すか短くする');
    info.sections.push({ id, height });
  }
  const m = document.createElement('meta');
  m.name = 'capture-info';
  m.content = btoa(unescape(encodeURIComponent(JSON.stringify(info))));
  document.head.appendChild(m);
})();
</script>
`;

const src = readFileSync(html, 'utf8');
const tmp = join(dirname(html), `.capture-${process.pid}-${basename(html)}`);
writeFileSync(tmp, /<\/body>/i.test(src) ? src.replace(/<\/body>(?![\s\S]*<\/body>)/i, `${probe}</body>`) : src + probe);
const tmpUrl = pathToFileURL(tmp).href;
// fail() は process.exit するので、finally ではなく exit で一時ファイルを消す
process.on('exit', () => { if (existsSync(tmp)) unlinkSync(tmp); });

mkdirSync(outDir, { recursive: true });
const written = [];
let warn = [];
try {
  const dom = runChrome([`--window-size=${opts.width},800`, '--dump-dom', `${tmpUrl}#measure-${opts.width}`]);
  const m = dom.match(/<meta name="capture-info" content="([^"]+)">/);
  if (!m) fail('HTML を計測できなかった。JavaScript のエラーで止まっていないか確かめる');
  const info = JSON.parse(Buffer.from(m[1], 'base64').toString('utf8'));
  warn = info.warn;
  if (info.sections.length === 0) fail('HTML に <section> が無い。1 枚にしたい単位を <section id="..."> で囲む');
  for (const s of info.sections) {
    if (!s.id) fail('id の無い <section> がある。ファイル名になるので <section id="01-before-after"> のように付ける');
    if (!/^[A-Za-z0-9._-]+$/.test(s.id)) fail(`section の id に英数字と . _ - 以外がある: ${s.id} (ファイル名と URL にそのまま入る)`);
  }
  for (const scheme of opts.schemes) {
    for (const s of info.sections) {
      const path = join(outDir, `${s.id}-${scheme}.png`);
      runChrome([
        `--window-size=${opts.width},${Math.max(s.height, 1)}`,
        `--force-device-scale-factor=${opts.scale}`,
        ...(scheme === 'dark' ? ['--force-dark-mode'] : []),
        `--screenshot=${path}`,
        `${tmpUrl}#${encodeURIComponent(s.id)}`,
      ]);
      if (!existsSync(path)) fail(`PNG が書き出されなかった: ${path}`);
      written.push(path);
    }
  }
} finally {
  if (existsSync(tmp)) unlinkSync(tmp);
}

console.log('OK');
console.log(JSON.stringify(written, null, 2));
for (const w of warn) console.log(`WARN: ${w}`);
