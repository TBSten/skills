#!/usr/bin/env node
// single-html.mjs — agent が作って人に渡す 1 枚の HTML（assets/templates/single-html/feedback-report.html）のデータを読み書きする cli。
// node の標準だけで動く（Node 18 以上、依存なし）。この script を読解・書き換え・再実装せず、そのまま実行する。
// 形と流れは references/setup/web/single-html.md。
//
//   node single-html.mjs new <out.html> [--data <data.json>] [--title <題>] [--kind <種類>]   ひな形から作る（既にあれば中断。--force で上書き）
//   node single-html.mjs get <file.html>                                                      データの JSON を出す
//   node single-html.mjs set <file.html> <data.json | ->                                      データを差し替える（検査に通ったときだけ書く）
//   node single-html.mjs check <file.html | data.json>                                        データを検査する
//
// 終了コード: 0 = 成功 / 1 = 検査に落ちた・ファイルが無い / 2 = 使い方の誤り。エラーは stderr に「何が・どこで・どう直すか」。
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
export const TEMPLATE = path.join(HERE, '..', 'assets', 'templates', 'single-html', 'feedback-report.html');
const OPEN = '<script type="application/json" id="report">';
const CLOSE = '</script>';
const BLOCK_TYPES = new Set(['p', 'list', 'table', 'callout', 'details']);

export function extract(html) {
  const i = html.indexOf(`\n${OPEN}`) + 1; // 行頭の開始タグだけを見る（コメントの中の文字列に引っかからない）
  if (i < 1) throw new Error(`データの場所（${OPEN}）が見つかりません。ひな形から作った HTML か確かめてください`);
  const start = i + OPEN.length;
  const end = html.indexOf(CLOSE, start);
  if (end < 0) throw new Error('データの終わり（</script>）が見つかりません');
  return { start, end, text: html.slice(start, end) };
}

/** JSON を HTML に埋める形にする（`<` を \u003c にして </script> で切れないようにする） */
export const encode = (data) => `\n${JSON.stringify(data, null, 2).replace(/</g, '\\u003c')}\n`;

export function replaceData(html, data) {
  const { start, end } = extract(html);
  return html.slice(0, start) + encode(data) + html.slice(end);
}

/** 検査。問題の一覧（空なら OK）を返す */
export function validate(data) {
  const errs = [];
  const where = (p, msg) => errs.push(`${p}: ${msg}`);
  if (!data || typeof data !== 'object' || Array.isArray(data)) return ['(ルート): オブジェクトにしてください'];
  if (typeof data.title !== 'string' || !data.title) where('title', '空でない文字列にしてください');
  for (const k of ['kind', 'file', 'subtitle']) if (data[k] != null && typeof data[k] !== 'string') where(k, '文字列にしてください');
  if (data.file && !/^[^/\\]+\.html?$/.test(data.file)) where('file', 'このファイルの名前（例: report.html）にしてください');
  const ids = (arr, p) => {
    const seen = new Set();
    arr.forEach((x, i) => { if (x && x.id != null) { if (typeof x.id !== 'string' || !/^[\w.-]+$/.test(x.id)) where(`${p}[${i}].id`, '英数字・_・-・. だけにしてください（JSON の場所に使う）'); else if (seen.has(x.id)) where(`${p}[${i}].id`, `「${x.id}」が重複しています`); seen.add(x.id); } });
  };
  const blocks = (arr, p) => {
    if (!Array.isArray(arr)) return where(p, '配列にしてください');
    arr.forEach((b, i) => {
      const bp = `${p}[${i}]`;
      if (!b || typeof b !== 'object') return where(bp, 'オブジェクトにしてください');
      const type = b.type ?? 'p';
      if (!BLOCK_TYPES.has(type)) return where(`${bp}.type`, `「${type}」は使えません（${[...BLOCK_TYPES].join(' / ')}）`);
      if ((type === 'p' || type === 'callout') && typeof b.text !== 'string') where(`${bp}.text`, '文字列にしてください');
      if (type === 'list') { if (!Array.isArray(b.items)) where(`${bp}.items`, '配列にしてください'); else b.items.forEach((it, j) => { if (typeof it !== 'string' && typeof it?.text !== 'string') where(`${bp}.items[${j}]`, '文字列か { text } にしてください'); }); }
      if (type === 'table') {
        if (!Array.isArray(b.columns) || !b.columns.length) where(`${bp}.columns`, '1 つ以上の { key, label } の配列にしてください');
        else b.columns.forEach((c, j) => { if (typeof c?.key !== 'string' || !/^[\w-]+$/.test(c.key)) where(`${bp}.columns[${j}].key`, '英数字・_・- の文字列にしてください'); });
        if (!Array.isArray(b.rows)) where(`${bp}.rows`, '配列にしてください'); else { b.rows.forEach((r, j) => { if (!r || typeof r !== 'object' || Array.isArray(r)) where(`${bp}.rows[${j}]`, '{ <列の key>: 値 } のオブジェクトにしてください'); }); ids(b.rows, `${bp}.rows`); }
      }
      if (type === 'details') { if (typeof b.summary !== 'string') where(`${bp}.summary`, '文字列にしてください'); blocks(b.blocks ?? [], `${bp}.blocks`); }
    });
  };
  if (!Array.isArray(data.sections)) where('sections', '配列にしてください');
  else { ids(data.sections, 'sections'); data.sections.forEach((s, i) => { if (typeof s?.title !== 'string') where(`sections[${i}].title`, '文字列にしてください'); blocks(s?.blocks ?? [], `sections[${i}].blocks`); }); }
  if (data.questions != null) {
    if (!Array.isArray(data.questions)) where('questions', '配列にしてください');
    else {
      ids(data.questions, 'questions');
      data.questions.forEach((q, i) => {
        const qp = `questions[${i}]`;
        if (typeof q?.id !== 'string') where(`${qp}.id`, '必須です（回答の鍵になる）');
        if (typeof q?.text !== 'string') where(`${qp}.text`, '文字列にしてください');
        if (!Array.isArray(q?.options) || q.options.length < 2) where(`${qp}.options`, '2 つ以上の { value, label } にしてください');
        else {
          const vals = q.options.map((o) => o?.value);
          if (vals.some((v) => typeof v !== 'string')) where(`${qp}.options`, 'value は文字列にしてください');
          if (q.recommend != null && !vals.includes(q.recommend)) where(`${qp}.recommend`, `options の value のどれかにしてください（${vals.join(' / ')}）`);
        }
      });
    }
  }
  if (data.answers != null && (typeof data.answers !== 'object' || Array.isArray(data.answers))) where('answers', '{ <質問の id>: { answer, note } } にしてください');
  return errs;
}

const readJsonArg = (p) => JSON.parse(p === '-' ? fs.readFileSync(0, 'utf8') : fs.readFileSync(p, 'utf8'));

function fail(msg, code = 1) { console.error(`single-html: ${msg}`); process.exit(code); }

async function main(argv) {
  const [cmd, ...rest] = argv;
  const flags = {}; const pos = [];
  for (let i = 0; i < rest.length; i++) { const a = rest[i]; if (a === '--force') flags.force = true; else if (a.startsWith('--')) flags[a.slice(2)] = rest[++i]; else pos.push(a); }
  const usage = 'usage: single-html.mjs new <out.html> [--data f] [--title t] [--kind k] [--force] | get <file.html> | set <file.html> <data.json|-> | check <file.html|data.json>';
  if (cmd === 'new') {
    const out = pos[0]; if (!out) fail(usage, 2);
    if (fs.existsSync(out) && !flags.force) fail(`${out} は既にあります。上書きするなら --force を付けてください`);
    let data = flags.data ? readJsonArg(flags.data) : { title: flags.title || 'レポート', kind: flags.kind || 'レポート', file: path.basename(out), subtitle: '', sections: [], questions: [] };
    if (flags.title) data.title = flags.title;
    if (flags.kind) data.kind = flags.kind;
    data.file = path.basename(out);
    const errs = validate(data); if (errs.length) fail(`データが検査に落ちました:\n  ${errs.join('\n  ')}`);
    fs.writeFileSync(out, replaceData(fs.readFileSync(TEMPLATE, 'utf8'), data));
    console.log(`OK: ${out} を作りました（file=${data.file}）`);
  } else if (cmd === 'get') {
    if (!pos[0]) fail(usage, 2);
    const { text } = extract(fs.readFileSync(pos[0], 'utf8'));
    try { console.log(JSON.stringify(JSON.parse(text), null, 2)); } catch (e) { fail(`${pos[0]} のデータの JSON が壊れています: ${e.message}`); }
  } else if (cmd === 'set') {
    if (pos.length < 2) fail(usage, 2);
    let data; try { data = readJsonArg(pos[1]); } catch (e) { fail(`${pos[1]} を JSON として読めません: ${e.message}`); }
    const errs = validate(data); if (errs.length) fail(`データが検査に落ちたので書きませんでした:\n  ${errs.join('\n  ')}`);
    const html = fs.readFileSync(pos[0], 'utf8');
    const next = replaceData(html, data);
    JSON.parse(extract(next).text); // 読み戻して確かめる
    fs.writeFileSync(pos[0], next);
    console.log(`OK: ${pos[0]} のデータを差し替えました`);
  } else if (cmd === 'check') {
    if (!pos[0]) fail(usage, 2);
    const raw = fs.readFileSync(pos[0], 'utf8');
    let data; try { data = JSON.parse(/\.html?$/.test(pos[0]) ? extract(raw).text : raw); } catch (e) { fail(`JSON が壊れています: ${e.message}`); }
    const errs = validate(data);
    if (/\.html?$/.test(pos[0]) && data.file && data.file !== path.basename(pos[0])) errs.push(`file: 「${data.file}」がファイル名「${path.basename(pos[0])}」と違います（コピーの 1 行目に出る名前）`);
    if (errs.length) fail(`検査に落ちました:\n  ${errs.join('\n  ')}`);
    console.log('OK');
  } else fail(usage, 2);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) await main(process.argv.slice(2));
