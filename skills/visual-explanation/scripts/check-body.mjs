#!/usr/bin/env node
// PR / チケット本文の Markdown が visual-explanation の決まりに沿っているか調べる。
//
// 使い方:
//   node check-body.mjs <body.md>        (- を渡すと標準入力から読む)
//
// 調べること:
//   - 見た目で分かるもの (画像 / <img> / <details> / コードブロック) が 1 つ以上ある
//   - 画像に alt がある
//   - GitHub の画像 URL がブランチ名でなく 40 桁の SHA で固定されている
//   - .github/pr-assets/ の画像が非公開リポジトリ向けの形 (?raw=true) か raw.githubusercontent.com か混在していない
//
// 出力: 問題が無ければ OK。あれば NG 行を並べて終了コード 1。

import { readFileSync } from 'node:fs';

const arg = process.argv[2];
if (!arg) {
  console.error('ERROR: 本文の Markdown ファイルを渡す。使い方: node check-body.mjs <body.md> (- で標準入力)');
  process.exit(1);
}
let body;
try {
  body = readFileSync(arg === '-' ? 0 : arg, 'utf8');
} catch (e) {
  console.error(`ERROR: 読めない: ${arg} (${e.message})`);
  process.exit(1);
}

// コメントは本文に表示されないので除く
const text = body.replace(/<!--[\s\S]*?-->/g, '');
const problems = [];

const mdImages = [...text.matchAll(/!\[([^\]]*)\]\(([^)\s]+)[^)]*\)/g)].map((m) => ({ alt: m[1], url: m[2] }));
const htmlImages = [...text.matchAll(/<img\b[^>]*>/gi)].map((m) => {
  const tag = m[0];
  return {
    alt: (tag.match(/\balt\s*=\s*"([^"]*)"/i) || [])[1] ?? '',
    url: (tag.match(/\bsrc\s*=\s*"([^"]*)"/i) || [])[1] ?? '',
  };
});
const images = [...mdImages, ...htmlImages];
const hasDetails = /<details\b/i.test(text);
const hasCode = /^(```|~~~)/m.test(text);
const hasVideo = /\.(mp4|mov|webm|gif)(\?|\)|"|\s|$)/i.test(text) || /github\.com\/user-attachments\/assets\//.test(text);

if (images.length === 0 && !hasDetails && !hasCode && !hasVideo) {
  problems.push('見た目で分かるもの (画像 / 動画 / <details> の実際の出力 / diff のコードブロック) が 1 つも無い');
}

for (const { alt, url } of images) {
  if (!alt.trim()) problems.push(`alt が空: ${url} (「何が分かる図か」を 1 文で書く)`);
  else if (/\.(png|jpe?g|gif|webp|svg)$/i.test(alt.trim())) problems.push(`alt がファイル名のまま: ${alt} (「何が分かる図か」を 1 文で書く)`);

  const raw = url.match(/^https:\/\/raw\.githubusercontent\.com\/[^/]+\/[^/]+\/([^/]+)\//);
  const blob = url.match(/^https:\/\/github\.com\/[^/]+\/[^/]+\/(?:blob|raw)\/([^/]+)\//);
  const ref = (raw || blob || [])[1];
  if (ref && !/^[0-9a-f]{40}$/.test(ref)) {
    problems.push(`URL が SHA で固定されていない (ref = ${ref}): ${url}`);
  }
  if (blob && /\/blob\//.test(url) && !/[?&]raw=true\b/.test(url)) {
    problems.push(`blob URL に ?raw=true が無いので画像として表示されない: ${url}`);
  }
}

const assets = images.filter((i) => i.url.includes('/.github/pr-assets/'));
const kinds = new Set(assets.map((i) => (i.url.startsWith('https://raw.githubusercontent.com/') ? 'raw' : 'blob')));
if (kinds.size > 1) {
  problems.push('pr-assets の URL に raw.githubusercontent.com と blob?raw=true が混在している。リポジトリの公開 / 非公開に合わせてどちらかに揃える');
}

if (problems.length === 0) {
  console.log('OK');
  process.exit(0);
}
for (const p of problems) console.log(`NG: ${p}`);
process.exit(1);
