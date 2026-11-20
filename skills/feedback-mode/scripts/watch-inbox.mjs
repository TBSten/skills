#!/usr/bin/env node
// watch-inbox.mjs — フィードバック置き場（Inbox）を見張り、新しい記録が来たら 1 行ずつ出して終わる。node の標準だけで動く（Node 18 以上、依存なし）。
// loop を「記録が来たらすぐ始める」ために使う（references/loop/loop.md §2.6）。この script を読解・書き換え・再実装せず、そのまま実行する。
//
//   node watch-inbox.mjs [<Inbox>] [--any] [--interval <ms>] [--settle <ms>] [--timeout <秒>]
//
//   <Inbox>        既定: $FEEDBACK_DIR、無ければ ./feedback
//   --any          起動した時点で未対応の記録があれば、それを出してすぐ終わる（無ければ新しい記録を待つ）
//   --interval     見に行く間隔。既定 2000ms（ネットワークのマウントでも動くよう、fs.watch ではなく一覧を見比べる）
//   --settle       大きさがこの間変わらず 0 バイトでもないとき「書き終わった」とみなす。既定 1500ms
//   --timeout      この秒数のあいだ新しい記録が無ければ TIMEOUT を出して終わる（終了コード 2）。既定: 待ち続ける
//
// 出力（標準出力）: 新しい記録 1 件ごとに `NEW <id> <パス>`。同じ周回で書き終わったものはまとめて出してから終わる（終了コード 0）。
// 数えるのは Inbox 直下の yymmdd-xx.md だけ（archived/・memo/・review/ などは見ない。references/spec/data.md §2）。
// 置き場が無い・読めないときは stderr に理由を出して終了コード 1。
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

export const RECORD_RE = /^\d{6}-\d{2,}\.md$/;

export function listRecords(dir) {
  const out = new Map();
  for (const name of fs.readdirSync(dir)) {
    if (!RECORD_RE.test(name)) continue;
    const st = fs.statSync(path.join(dir, name), { throwIfNoEntry: false });
    if (st?.isFile()) out.set(name, st.size);
  }
  return out;
}

export function parseArgs(argv, env = process.env) {
  const o = { dir: env.FEEDBACK_DIR || 'feedback', any: false, interval: 2000, settle: 1500, timeout: null };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    const num = (name) => {
      const v = Number(argv[++i]);
      if (!Number.isFinite(v) || v < 0) throw new Error(`${name} には 0 以上の数を渡してください（受け取った値: ${argv[i]}）`);
      return v;
    };
    if (a === '--any') o.any = true;
    else if (a === '--interval') o.interval = num(a);
    else if (a === '--settle') o.settle = num(a);
    else if (a === '--timeout') o.timeout = num(a);
    else if (a.startsWith('--')) throw new Error(`知らないオプションです: ${a}（使えるもの: --any --interval --settle --timeout）`);
    else o.dir = a;
  }
  return o;
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** 新しい記録（書き終わったもの）が 1 件以上そろうまで待ち、名前の配列を返す。timeout なら null。 */
export async function waitForNew({ dir, any = false, interval = 2000, settle = 1500, timeout = null, now = () => Date.now() }) {
  const baseline = any ? new Map() : listRecords(dir);
  const seen = new Map(); // name -> { size, since }
  const start = now();
  for (;;) {
    const cur = listRecords(dir);
    const ready = [];
    for (const [name, size] of cur) {
      if (baseline.has(name)) continue;
      const prev = seen.get(name);
      if (!prev || prev.size !== size) seen.set(name, { size, since: now() }); // 大きさが変わったら数え直す
      if (size > 0 && now() - seen.get(name).since >= settle) ready.push(name);
    }
    if (ready.length) return ready.sort();
    if (timeout != null && now() - start >= timeout * 1000) return null;
    await sleep(interval);
  }
}

async function main() {
  let o;
  try { o = parseArgs(process.argv.slice(2)); } catch (e) { console.error(`watch-inbox: ${e.message}`); process.exit(1); }
  const dir = path.resolve(o.dir);
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    console.error(`watch-inbox: 置き場が見つかりません: ${dir}（引数か FEEDBACK_DIR で Inbox のディレクトリを渡してください）`);
    process.exit(1);
  }
  const ready = await waitForNew({ ...o, dir });
  if (!ready) { console.log('TIMEOUT'); process.exit(2); }
  for (const name of ready) console.log(`NEW ${name.replace(/\.md$/, '')} ${path.join(dir, name)}`);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) await main();
