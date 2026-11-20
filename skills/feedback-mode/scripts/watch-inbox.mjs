#!/usr/bin/env node
// watch-inbox.mjs — Inbox に新しい記録（yymmdd-xx.md）が増えたら、1 件ごとに 1 行出す / コマンドを実行する。
//
// 使い方:
//   node watch-inbox.mjs <Inbox のディレクトリ> [オプション]          ローカルのファイルの Sink（既定）
//   node watch-inbox.mjs --url http://127.0.0.1:7373/__feedback [オプション]   HTTP の受け口（別マシンの Inbox）
// オプション:
//   --exec "<コマンド>"     新しい記録ごとに sh -c で実行する（1 件ずつ順番に。終わってから次）。
//                          環境変数 FEEDBACK_ID・FEEDBACK_REF・FEEDBACK_FILE（ファイルのときだけ）を渡す
//   --once                 新しい記録を 1 回見つけたら（その回の分を出して / 実行して）終わる。Claude Code の Monitor・until ループ向け
//   --include-existing     起動時に既にある記録も「新しい」として扱う（既定は起動後に増えた分だけ）
//   --interval <ms>        見る間隔（既定 2000）
//   --settle <ms>          ファイルの大きさがこの間変わらなくなってから出す（書きかけを拾わない。既定 1000）
//   --json                 1 行を JSON にする（{"event":"new","id":…,"ref":…,"file":…}）
// 出力: 1 件ごとに `NEW <id> <ref>`（stdout）。エラーは stderr。終了コード: 0 = 正常 / 1 = 失敗（--exec が失敗しても続け、最後に 1）/ 2 = 使い方の誤り
// 中身を読んだり、archive したりはしない（archive はオーケストレータだけ: references/loop/loop.md）。
import fs from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';

const RECORD = /^\d{6}-\d{2,}\.md$/; // 記録として数えるのは直下の yymmdd-xx.md だけ（data.md §2）

export function parseArgs(argv) {
  const o = { dir: null, url: null, exec: null, once: false, includeExisting: false, interval: 2000, settle: 1000, json: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    const val = () => { const v = argv[++i]; if (v === undefined) throw new Error(`${a} に値がありません`); return v; };
    if (a === '--url') o.url = val().replace(/\/+$/, '');
    else if (a === '--exec') o.exec = val();
    else if (a === '--once') o.once = true;
    else if (a === '--include-existing') o.includeExisting = true;
    else if (a === '--interval') o.interval = Number(val());
    else if (a === '--settle') o.settle = Number(val());
    else if (a === '--json') o.json = true;
    else if (a.startsWith('--')) throw new Error(`知らないオプション: ${a}`);
    else if (!o.dir) o.dir = a;
    else throw new Error(`引数が多すぎます: ${a}`);
  }
  if (!o.dir === !o.url) throw new Error('Inbox のディレクトリか --url のどちらか 1 つを指定してください');
  if (!(o.interval > 0) || !(o.settle >= 0)) throw new Error('--interval / --settle は数で指定してください');
  return o;
}

// 今ある記録の一覧 [{ id, ref, file?, bytes }]
export async function listRecords(o) {
  if (o.url) {
    const res = await fetch(`${o.url}/entries?state=open`);
    const body = await res.json().catch(() => ({}));
    if (!res.ok || !body.ok) throw new Error(`一覧を取れません: HTTP ${res.status} ${body.error ?? ''}`);
    return body.entries.map((e) => ({ id: e.id, ref: e.ref ?? e.id, bytes: e.bytes ?? 0 }));
  }
  if (!fs.existsSync(o.dir)) return [];
  return fs.readdirSync(o.dir, { withFileTypes: true })
    .filter((d) => d.isFile() && RECORD.test(d.name))
    .map((d) => {
      const file = path.join(o.dir, d.name);
      return { id: d.name.replace(/\.md$/, ''), ref: path.relative(process.cwd(), file), file, bytes: fs.statSync(file).size };
    });
}

export function createWatcher(o, { out = (s) => process.stdout.write(s + '\n'), err = (s) => process.stderr.write(s + '\n'), now = Date.now } = {}) {
  const seen = new Set();
  const pending = new Map(); // id → { rec, bytes, since }
  let failed = false;
  const queue = [];
  let running = null;

  const runExec = (rec) => new Promise((resolve) => {
    const env = { ...process.env, FEEDBACK_ID: rec.id, FEEDBACK_REF: rec.ref, ...(rec.file ? { FEEDBACK_FILE: rec.file } : {}) };
    const p = spawn('sh', ['-c', o.exec], { env, stdio: ['ignore', 'inherit', 'inherit'] });
    p.on('exit', (code) => { if (code !== 0) { failed = true; err(`exec failed (${code}) for ${rec.id}`); } resolve(); });
    p.on('error', (e) => { failed = true; err(`exec error for ${rec.id}: ${e.message}`); resolve(); });
  });
  const drain = async () => {
    if (running) return running;
    running = (async () => { while (queue.length) await runExec(queue.shift()); })();
    await running; running = null;
  };

  // 1 回見る。新しく出せた記録の配列を返す
  async function tick({ initial = false } = {}) {
    const recs = await listRecords(o);
    if (initial && !o.includeExisting) { for (const r of recs) seen.add(r.id); return []; }
    const ready = [];
    for (const r of recs) {
      if (seen.has(r.id)) continue;
      const p = pending.get(r.id);
      // 大きさが 0 か、前回から変わっていれば待つ（書きかけ・作成直後）
      if (!p || p.bytes !== r.bytes || r.bytes === 0) { pending.set(r.id, { bytes: r.bytes, since: now() }); continue; }
      if (now() - p.since < o.settle) continue;
      pending.delete(r.id); seen.add(r.id); ready.push(r);
    }
    for (const r of ready.sort((a, b) => a.id.localeCompare(b.id))) {
      out(o.json ? JSON.stringify({ event: 'new', id: r.id, ref: r.ref, ...(r.file ? { file: r.file } : {}) }) : `NEW ${r.id} ${r.ref}`);
      if (o.exec) queue.push(r);
    }
    if (o.exec) await drain();
    return ready;
  }
  return { tick, failed: () => failed };
}

async function main() {
  let o;
  try { o = parseArgs(process.argv.slice(2)); } catch (e) { process.stderr.write(`${e.message}\n`); process.exit(2); }
  const w = createWatcher(o);
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  let stop = false;
  process.on('SIGINT', () => { stop = true; });
  process.on('SIGTERM', () => { stop = true; });
  try { await w.tick({ initial: true }); } catch (e) { process.stderr.write(`${e.message}\n`); process.exit(1); }
  process.stderr.write(`watching ${o.url ?? o.dir}（${o.interval}ms ごと。Ctrl+C で止める）\n`);
  while (!stop) {
    // 一時的な失敗（受け口の再起動など）は stderr に出して続ける
    try { const got = await w.tick(); if (o.once && got.length) break; } catch (e) { process.stderr.write(`${e.message}\n`); }
    if (!stop) await sleep(o.interval);
  }
  process.exit(w.failed() ? 1 : 0);
}

if (import.meta.url === `file://${process.argv[1]}` || process.argv[1]?.endsWith('watch-inbox.mjs')) main();
