#!/usr/bin/env node
// PR / チケット本文の Markdown が visual-explanation の決まりに沿っているか調べる。
//
// 使い方:
//   node check-body.mjs [--no-visual] <body.md>   (- を渡すと標準入力から読む)
//
//   --no-visual  CI だけ・設定だけなど、見せ方の表で「画像なしでよい」を選んだときに付ける。
//                「見た目で分かるものが無い」の検査だけを外し、画像があればその検査は行う
//
// 調べること:
//   - 見た目で分かるもの (画像 / <img> / <details> / ```diff / 動画) が 1 つ以上ある (--no-visual で外せる)
//   - 「何をしたか」/「背景」の節があれば、1 つ目の見た目がその節の中にある
//   - コードブロック・インラインコードの中の画像記法は検査しない
//   - 画像に alt がある
//   - GitHub の画像 URL がブランチ名でなく 40 桁の SHA で固定されている
//   - .github/pr-assets/ の画像が非公開リポジトリ向けの形 (?raw=true) か raw.githubusercontent.com か混在していない
//
// 出力: 問題が無ければ OK。あれば NG 行を並べて終了コード 1。

import { readFileSync } from 'node:fs';

const args = process.argv.slice(2);
const noVisual = args.includes('--no-visual');
const arg = args.find((a) => a === '-' || !a.startsWith('--'));
const unknown = args.filter((a) => a.startsWith('--') && a !== '--no-visual');
if (unknown.length) {
  console.error(`ERROR: 不明なオプション: ${unknown.join(' ')} (使えるのは --no-visual)`);
  process.exit(1);
}
if (!arg) {
  console.error('ERROR: 本文の Markdown ファイルを渡す。使い方: node check-body.mjs [--no-visual] <body.md> (- で標準入力)');
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
// コードブロック・インラインコードの中の記法は表示される画像ではないので、画像の検査からは除く。
// 見た目として数えるコードブロックは ```diff だけ (コマンドを書いただけのブロックは数えない)。
// <details> の中のコードブロックは <details> として数える。
const DIFF_MARK = '@@VISUAL_DIFF_BLOCK@@';
const prose = text
  .replace(/^ {0,3}(`{3,}|~{3,})([^\n]*)\n[\s\S]*?^ {0,3}\1[^\n]*$/gm, (_, _f, info) =>
    /^\s*diff\b/i.test(info) ? DIFF_MARK : '')
  .replace(/(`+)[^`\n][^\n]*?\1/g, '');
const problems = [];

// 参照形式の定義 [ref]: url
const refs = new Map();
for (const m of prose.matchAll(/^ {0,3}\[([^\]]+)\]:\s*<?([^\s>]+)>?/gm)) {
  refs.set(m[1].trim().toLowerCase(), m[2]);
}

const mdImages = [...prose.matchAll(/!\[([^\]]*)\]\(\s*<?([^)\s>]+)>?[^)]*\)/g)].map((m) => ({ alt: m[1], url: m[2] }));
const refImages = [...prose.matchAll(/!\[([^\]]*)\]\[([^\]]*)\]/g)].map((m) => {
  const key = (m[2] || m[1]).trim().toLowerCase();
  return { alt: m[1], url: refs.get(key) ?? '' };
});
for (const r of refImages) {
  if (!r.url) problems.push(`参照形式の画像の定義が見つからない: ![${r.alt}][...]`);
}
// 属性値は "..." / '...' / 引用符なしのどれでもよい
const attr = (tag, name) => {
  const m = tag.match(new RegExp(`\\b${name}\\s*=\\s*(?:"([^"]*)"|'([^']*)'|([^\\s"'=<>\`]+))`, 'i'));
  return m ? (m[1] ?? m[2] ?? m[3] ?? '') : '';
};
const htmlImages = [...prose.matchAll(/<img\b[^>]*>/gi)].map((m) => ({ alt: attr(m[0], 'alt'), url: attr(m[0], 'src') }));
const images = [...mdImages, ...refImages.filter((r) => r.url), ...htmlImages];
// 1 つ目の見た目の位置 (画像 / <img> / <details> / ```diff / 動画)
const visualRe = new RegExp(
  String.raw`!\[[^\]]*\][(\[]|<img\b|<details\b|${DIFF_MARK}|github\.com/user-attachments/assets/|https?://\S+\.(?:mp4|mov|webm|gif)\b`,
  'i',
);
const firstVisual = prose.search(visualRe);

if (!noVisual && firstVisual < 0) {
  problems.push('見た目で分かるもの (画像 / 動画 / <details> の実際の出力 / ```diff のコードブロック) が 1 つも無い。コマンドを書いただけのコードブロックは数えない');
}

// 「何をしたか」(チケットなら「背景」) の節があれば、1 つ目の見た目がその節の中にあるかを見る
const headings = [...prose.matchAll(/^(#{1,6})\s+(.+?)\s*#*\s*$/gm)].map((m) => ({ level: m[1].length, title: m[2], index: m.index }));
const target = headings.find((h) => /何をしたか|やったこと|背景|what (i|we) did|what changed|background/i.test(h.title));
if (!noVisual && firstVisual >= 0 && target) {
  const next = headings.find((h) => h.index > target.index && h.level <= target.level);
  const end = next ? next.index : prose.length;
  if (firstVisual < target.index || firstVisual >= end) {
    problems.push(`1 つ目の見た目が「${target.title}」の節の中に無い。節の頭に 1 枚目を置く`);
  }
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
