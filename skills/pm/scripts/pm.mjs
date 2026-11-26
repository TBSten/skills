#!/usr/bin/env node
/**
 * pm skill の決定的な作業をまとめた CLI。AI はこれを読解・書き換えせず、そのまま実行する。
 *
 *   node pm.mjs phases                       タイミング (A〜J) と各タイミングの中心資料
 *   node pm.mjs list [--phase A,B] [--core]  資料カタログ (id / タイミング / 名前 / 判断したいこと)
 *   node pm.mjs new <id|slug>... [--dir D] [--project NAME]
 *                                            テンプレートから資料を作る (既にあればスキップ)
 *   node pm.mjs sync [--dir D]               計算ブロックの再計算・updated の更新・目次 README.md の再生成
 *   node pm.mjs status [--dir D] [--json]    作った資料と残 TODO / 要確認 の数
 *   node pm.mjs check [--dir D] [--render]   TODO 残り・計算の古さ・Mermaid の閉じ忘れを検査 (--render で Mermaid を実描画)
 *
 * 既定の --dir は docs/pm。成功時の最終行は `OK <command> ...`。失敗時は stderr に理由と直し方を出して exit 1。
 */
import { readFileSync, writeFileSync, existsSync, mkdirSync, readdirSync, statSync, mkdtempSync, rmSync } from 'node:fs';
import { join, dirname, basename, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync, spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const SKILL_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const ASSETS = join(SKILL_DIR, 'assets');

function die(what, why, fix) {
  process.stderr.write(`ERROR: ${what}\n  理由: ${why}\n  直し方: ${fix}\n`);
  process.exit(1);
}

function readTsv(path) {
  const [head, ...rows] = readFileSync(path, 'utf8').trim().split('\n');
  const keys = head.split('\t');
  return rows.map((r) => Object.fromEntries(r.split('\t').map((v, i) => [keys[i], v ?? ''])));
}
const CATALOG = readTsv(join(ASSETS, 'catalog.tsv'));
const PHASES = readTsv(join(ASSETS, 'phases.tsv'));

function parseArgs(argv) {
  const pos = [];
  const opt = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const k = a.slice(2);
      const next = argv[i + 1];
      if (next !== undefined && !next.startsWith('--') && ['dir', 'project', 'phase'].includes(k)) {
        opt[k] = next;
        i++;
      } else opt[k] = true;
    } else pos.push(a);
  }
  return { pos, opt };
}

function today() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
function dateOf(ms) {
  const d = new Date(ms);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function findEntry(key) {
  const k = String(key).trim();
  const id = /^\d{1,2}$/.test(k) ? k.padStart(2, '0') : null;
  return CATALOG.find((c) => c.id === id || c.slug === k || c.name === k);
}
function fileName(c) {
  return `${c.id}-${c.slug}.md`;
}

// ---------- markdown helpers ----------
function frontmatter(text) {
  const m = text.match(/^---\n([\s\S]*?)\n---\n/);
  if (!m) return null;
  const fm = {};
  for (const line of m[1].split('\n')) {
    const mm = line.match(/^(\w+):\s*(.*)$/);
    if (mm) fm[mm[1]] = mm[2].replace(/^"(.*)"$/, '$1');
  }
  return fm;
}
function parseTable(block) {
  const rows = block
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.startsWith('|'))
    .map((l) => l.replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim()));
  if (rows.length < 2) return null;
  const [header, , ...body] = rows;
  return { header, body: body.filter((r) => r.some((c) => c !== '')) };
}
function num(v, where) {
  const s = String(v ?? '').replace(/[,，\s]/g, '').replace(/[^\d.\-]/g, '');
  const n = Number(s);
  if (s === '' || !Number.isFinite(n)) die(`数値として読めない値 "${v}" (${where})`, '計算ブロックの表には数値が必要', 'その欄に数値を書く (単位は前提・根拠の欄に書く)');
  return n;
}
const r1 = (n) => Math.round(n * 10) / 10;
const r2 = (n) => Math.round(n * 100) / 100;
const fmt = (n) => (Number.isInteger(n) ? String(n) : String(r2(n)));
const q = (s) => `"${String(s).replace(/"/g, "'")}"`;
function table(header, rows) {
  return [`| ${header.join(' | ')} |`, `|${header.map(() => '---').join('|')}|`, ...rows.map((r) => `| ${r.join(' | ')} |`)].join('\n');
}
function axisMax(values) {
  const m = Math.max(1, ...values);
  const mag = 10 ** Math.floor(Math.log10(m));
  return Math.ceil((m * 1.1) / mag) * mag;
}

// ---------- calc ----------
const CALCS = {
  pert(t, where) {
    let sumE = 0;
    let sumVar = 0;
    const rows = t.body.map((r, i) => {
      const [name, o, m, p] = r;
      const O = num(o, `${where} ${i + 1} 行目 楽観`);
      const M = num(m, `${where} ${i + 1} 行目 最頻`);
      const P = num(p, `${where} ${i + 1} 行目 悲観`);
      if (!(O <= M && M <= P)) die(`${where} ${i + 1} 行目 "${name}" が 楽観 ≤ 最頻 ≤ 悲観 になっていない`, '三点見積りの前提が崩れている', '値を見直す');
      const E = (O + 4 * M + P) / 6;
      const sd = (P - O) / 6;
      sumE += E;
      sumVar += sd * sd;
      return [name, fmt(O), fmt(M), fmt(P), fmt(r1(E)), fmt(r1(sd))];
    });
    const sd = Math.sqrt(sumVar);
    rows.push(['**合計**', '', '', '', `**${fmt(r1(sumE))}**`, `**${fmt(r1(sd))}**`]);
    return [
      table(['作業', '楽観', '最頻', '悲観', 'PERT 見積り', 'σ'], rows),
      '',
      `- 合計見積り: ${fmt(r1(sumE))}（約 84% で ${fmt(r1(sumE + sd))} 以内、約 98% で ${fmt(r1(sumE + 2 * sd))} 以内に収まる目安）`,
    ].join('\n');
  },

  cpm(t, where) {
    const tasks = t.body.map((r, i) => ({
      id: r[0],
      name: r[1],
      dur: num(r[2], `${where} ${i + 1} 行目 日数`),
      preds: (r[3] || '').split(/[,、\s]+/).filter(Boolean),
    }));
    const byId = new Map(tasks.map((x) => [x.id, x]));
    if (byId.size !== tasks.length) die(`${where} の ID が重複している`, 'ID で先行関係を引くため一意である必要がある', 'ID を一意にする');
    for (const x of tasks) for (const p of x.preds) if (!byId.has(p)) die(`${where} "${x.id}" の先行作業 "${p}" が表に無い`, '存在しない ID を参照している', '先行作業欄を表の ID で書く (複数はカンマ区切り)');
    const order = [];
    const state = new Map();
    const visit = (x, stack) => {
      if (state.get(x.id) === 2) return;
      if (state.get(x.id) === 1) die(`${where} に循環した依存がある: ${[...stack, x.id].join(' → ')}`, '循環があると最短期間が決まらない', '先行作業欄を見直す');
      state.set(x.id, 1);
      for (const p of x.preds) visit(byId.get(p), [...stack, x.id]);
      state.set(x.id, 2);
      order.push(x);
    };
    tasks.forEach((x) => visit(x, []));
    for (const x of order) {
      x.es = Math.max(0, ...x.preds.map((p) => byId.get(p).ef));
      x.ef = x.es + x.dur;
    }
    const total = Math.max(...tasks.map((x) => x.ef));
    for (const x of [...order].reverse()) {
      const succ = tasks.filter((s) => s.preds.includes(x.id));
      x.lf = succ.length ? Math.min(...succ.map((s) => s.ls)) : total;
      x.ls = x.lf - x.dur;
      x.float = r2(x.ls - x.es);
    }
    const critical = new Set(tasks.filter((x) => x.float === 0).map((x) => x.id));
    // 1 本の経路として辿る (先頭から、余裕 0 の後続を順に)
    const path = [];
    let cur = order.find((x) => critical.has(x.id) && x.es === 0 && x.preds.every((p) => !critical.has(p)));
    while (cur) {
      path.push(cur.id);
      const c = cur;
      cur = tasks.find((s) => critical.has(s.id) && s.preds.includes(c.id) && s.es === c.ef);
    }
    const rows = tasks.map((x) => [x.id, x.name, fmt(x.dur), fmt(x.es), fmt(x.ef), fmt(x.ls), fmt(x.lf), critical.has(x.id) ? '**0（クリティカル）**' : fmt(x.float)]);
    const nid = (id) => `T_${id.replace(/[^\w]/g, '_')}`;
    const mer = ['```mermaid', 'flowchart LR'];
    for (const x of tasks) mer.push(`  ${nid(x.id)}[${q(`${x.id}: ${x.name}（${fmt(x.dur)}日）`)}]`);
    for (const x of tasks) for (const p of x.preds) mer.push(`  ${nid(p)} --> ${nid(x.id)}`);
    mer.push('  classDef critical fill:#fecaca,stroke:#b91c1c,stroke-width:2px');
    if (critical.size) mer.push(`  class ${[...critical].map(nid).join(',')} critical`);
    mer.push('```');
    return [
      mer.join('\n'),
      '',
      `- 最短完了期間: **${fmt(total)} 日**`,
      `- クリティカルパス: **${path.join(' → ')}**（赤枠。ここが 1 日遅れると全体が 1 日遅れる）`,
      '',
      table(['ID', '作業', '日数', '最早開始', '最早終了', '最遅開始', '最遅終了', '余裕'], rows),
    ].join('\n');
  },

  evm(t, where, text) {
    const pts = t.body.map((r, i) => ({
      label: r[0],
      pv: num(r[1], `${where} ${i + 1} 行目 PV`),
      ev: num(r[2], `${where} ${i + 1} 行目 EV`),
      ac: num(r[3], `${where} ${i + 1} 行目 AC`),
    }));
    const rows = pts.map((x) => [x.label, fmt(x.pv), fmt(x.ev), fmt(x.ac), fmt(x.ev - x.pv), fmt(x.ev - x.ac), x.pv ? fmt(r2(x.ev / x.pv)) : '-', x.ac ? fmt(r2(x.ev / x.ac)) : '-']);
    const last = pts[pts.length - 1];
    const spi = last.pv ? last.ev / last.pv : NaN;
    const cpi = last.ac ? last.ev / last.ac : NaN;
    const mx = axisMax(pts.flatMap((x) => [x.pv, x.ev, x.ac]));
    const out = [
      '```mermaid',
      'xychart-beta',
      '  title "EVM（1 本目 PV / 2 本目 EV / 3 本目 AC）"',
      `  x-axis [${pts.map((x) => q(x.label)).join(', ')}]`,
      `  y-axis 0 --> ${mx}`,
      `  line [${pts.map((x) => x.pv).join(', ')}]`,
      `  line [${pts.map((x) => x.ev).join(', ')}]`,
      `  line [${pts.map((x) => x.ac).join(', ')}]`,
      '```',
      '',
      table(['時点', 'PV', 'EV', 'AC', 'SV (EV-PV)', 'CV (EV-AC)', 'SPI', 'CPI'], rows),
      '',
      `- 最新 (${last.label}): SPI ${fmt(r2(spi))}（${spi < 1 ? '計画より遅れ' : '計画どおり以上'}）/ CPI ${fmt(r2(cpi))}（${cpi < 1 ? 'コスト超過' : 'コスト内'}）`,
    ];
    const bacM = text.match(/BAC[^:：\n]*[:：]\s*([\d.,]+)/);
    if (bacM) {
      const bac = num(bacM[1], 'BAC');
      const eac = bac / cpi;
      out.push(`- BAC ${fmt(bac)} → EAC（完成時総コスト見込み = BAC / CPI）${fmt(r1(eac))}、VAC（予算との差）${fmt(r1(bac - eac))}`);
    }
    return out.join('\n');
  },

  matrix(t, where) {
    const crit = t.header.slice(1);
    const wRow = t.body[0];
    if (!wRow || !/重み|weight/i.test(wRow[0])) die(`${where} の 1 行目が「重み」行になっていない`, '重み付き合計に重みが必要', '表の 1 行目を「| 重み | 5 | 4 | ... |」にする');
    const w = crit.map((c, i) => num(wRow[i + 1], `${where} 重み ${c}`));
    const opts = t.body.slice(1).map((r, i) => ({
      name: r[0],
      vals: crit.map((c, j) => num(r[j + 1], `${where} ${r[0] || i + 2 + ' 行目'} ${c}`)),
    }));
    if (!opts.length) die(`${where} に選択肢の行が無い`, '比べる対象が無い', '重み行の下に選択肢を 1 行ずつ書く');
    opts.forEach((o) => (o.total = o.vals.reduce((s, v, i) => s + v * w[i], 0)));
    const max = w.reduce((s, x) => s + x * 5, 0);
    const ranked = [...opts].sort((a, b) => b.total - a.total);
    const rows = ranked.map((o, i) => [String(i + 1), o.name, ...o.vals.map(fmt), `**${fmt(o.total)}**`]);
    return [
      table(['順位', '選択肢', ...crit.map((c, i) => `${c} (×${fmt(w[i])})`), `合計 (満点 ${fmt(max)})`], rows),
      '',
      '```mermaid',
      'xychart-beta',
      '  title "重み付き合計"',
      `  x-axis [${ranked.map((o) => q(o.name)).join(', ')}]`,
      `  y-axis 0 --> ${max}`,
      `  bar [${ranked.map((o) => o.total).join(', ')}]`,
      '```',
      '',
      `- 最高点: **${ranked[0].name}**（${fmt(ranked[0].total)} 点）${ranked[1] && ranked[0].total - ranked[1].total <= max * 0.05 ? `。2 位 ${ranked[1].name} との差が満点の 5% 以内なので、重みの置き方で逆転しうる` : ''}`,
    ].join('\n');
  },

  pareto(t, where) {
    const items = t.body.map((r, i) => ({ name: r[0], n: num(r[1], `${where} ${i + 1} 行目 件数`) })).sort((a, b) => b.n - a.n);
    const total = items.reduce((s, x) => s + x.n, 0);
    if (!total) die(`${where} の件数の合計が 0`, '割合が計算できない', '件数を書く');
    let cum = 0;
    let crossed = false;
    const rows = items.map((x) => {
      const before = cum;
      cum += x.n;
      const inTop = !crossed;
      if (before / total < 0.8 && cum / total >= 0.8) crossed = true;
      x.cum = cum;
      x.top = inTop;
      return [x.name, fmt(x.n), `${fmt(r1((x.n / total) * 100))}%`, `${fmt(r1((cum / total) * 100))}%`, inTop ? '★' : ''];
    });
    return [
      '```mermaid',
      'xychart-beta',
      '  title "パレート図（棒: 件数 / 線: 累積件数）"',
      `  x-axis [${items.map((x) => q(x.name)).join(', ')}]`,
      `  y-axis 0 --> ${total}`,
      `  bar [${items.map((x) => x.n).join(', ')}]`,
      `  line [${items.map((x) => x.cum).join(', ')}]`,
      '```',
      '',
      table(['分類', '件数', '割合', '累積割合', '累積 80% まで'], rows),
      '',
      `- 累積 80% までに入る分類（★）: **${items.filter((x) => x.top).map((x) => x.name).join('、')}**（全 ${fmt(total)} 件）`,
    ].join('\n');
  },

  control(t, where) {
    const pts = t.body.map((r, i) => ({ label: r[0], v: num(r[1], `${where} ${i + 1} 行目 値`) }));
    if (pts.length < 2) die(`${where} の測定値が 2 件未満`, '標準偏差が計算できない', '測定値を 2 件以上 (できれば 20 件以上) 書く');
    const n = pts.length;
    const mean = pts.reduce((s, x) => s + x.v, 0) / n;
    const sd = Math.sqrt(pts.reduce((s, x) => s + (x.v - mean) ** 2, 0) / (n - 1));
    const ucl = mean + 3 * sd;
    const lcl = mean - 3 * sd;
    const out = pts.filter((x) => x.v > ucl || x.v < lcl).map((x) => x.label);
    // 平均の片側に 7 点以上連続
    const runs = [];
    let side = 0;
    let len = 0;
    let start = 0;
    pts.forEach((x, i) => {
      const s = x.v > mean ? 1 : x.v < mean ? -1 : 0;
      if (s !== 0 && s === side) len++;
      else {
        if (len >= 7) runs.push(`${pts[start].label}〜${pts[i - 1].label}`);
        side = s;
        len = s ? 1 : 0;
        start = i;
      }
    });
    if (len >= 7) runs.push(`${pts[start].label}〜${pts[n - 1].label}`);
    const all = [...pts.map((x) => x.v), ucl, lcl];
    const span = Math.max(...all) - Math.min(...all) || 1;
    const lo = Math.floor(Math.min(...all) - span * 0.1);
    const hi = Math.ceil(Math.max(...all) + span * 0.1);
    const flat = (v) => `[${pts.map(() => r2(v)).join(', ')}]`;
    return [
      '```mermaid',
      'xychart-beta',
      '  title "管理図（上下の線: 管理限界 / 中央: 平均 / 折れ線: 測定値）"',
      `  x-axis [${pts.map((x) => q(x.label)).join(', ')}]`,
      `  y-axis ${lo} --> ${hi}`,
      `  line ${flat(ucl)}`,
      `  line ${flat(lcl)}`,
      `  line ${flat(mean)}`,
      `  line [${pts.map((x) => x.v).join(', ')}]`,
      '```',
      '',
      `- 平均 ${fmt(r2(mean))} / σ ${fmt(r2(sd))} / 上方管理限界 ${fmt(r2(ucl))} / 下方管理限界 ${fmt(r2(lcl))}（n = ${n}${n < 20 ? '。20 件未満なので限界は参考値' : ''}）`,
      `- 管理限界の外: ${out.length ? `**${out.join('、')}**` : 'なし'}`,
      `- 平均の片側に 7 点以上連続: ${runs.length ? `**${runs.join('、')}**` : 'なし'}`,
    ].join('\n');
  },
};

const HAS_CALC = /<!-- pm:calc \w+ -->/;
const CALC_RE = /<!-- pm:calc (\w+) -->\n([\s\S]*?)<!-- \/pm:calc -->/g;
function computeDoc(text, file) {
  let result = text;
  for (const m of text.matchAll(CALC_RE)) {
    const kind = m[1];
    const fn = CALCS[kind];
    if (!fn) die(`${file}: 未知の計算種別 "${kind}"`, `対応しているのは ${Object.keys(CALCS).join(', ')}`, 'テンプレートの pm:calc 行を書き換えない');
    const t = parseTable(m[2]);
    if (!t || !t.body.length) die(`${file}: pm:calc ${kind} の中に表が無い`, '計算の入力は Markdown の表', 'テンプレートの表の形のまま行を足す');
    const out = fn(t, `${file} (${kind})`, text);
    const resRe = new RegExp(`<!-- pm:result ${kind} -->\\n[\\s\\S]*?<!-- \\/pm:result -->`);
    if (!resRe.test(result)) die(`${file}: pm:result ${kind} の書き込み先が無い`, '計算結果の置き場所が消えている', `<!-- pm:result ${kind} --> と <!-- /pm:result --> の 2 行を計算ブロックの下に戻す`);
    result = result.replace(resRe, `<!-- pm:result ${kind} -->\n<!-- pm.mjs sync が書く。手で直さない -->\n${out}\n<!-- /pm:result -->`);
  }
  return result;
}

// ---------- docs dir ----------
function docsDir(opt) {
  return resolve(typeof opt.dir === 'string' ? opt.dir : 'docs/pm');
}
function listDocs(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((f) => /^\d{2}-.+\.md$/.test(f))
    .sort()
    .map((f) => {
      const path = join(dir, f);
      const text = readFileSync(path, 'utf8');
      const fm = frontmatter(text) || {};
      const lines = text.split('\n');
      const todo = [];
      const ask = [];
      lines.forEach((l, i) => {
        if (/\bTODO\b/.test(l)) todo.push(i + 1);
        if (/要確認[:：]/.test(l)) ask.push({ line: i + 1, text: l.trim() });
      });
      const c = CATALOG.find((x) => x.id === fm.pm_id) || CATALOG.find((x) => f.startsWith(x.id + '-'));
      return { file: f, path, text, fm, todo, ask, entry: c };
    });
}

function writeIndex(dir, docs) {
  const lines = [
    '# プロジェクトマネジメント資料',
    '',
    '<!-- このファイルは pm skill の pm.mjs sync が生成する。手で直さない -->',
    '',
  ];
  for (const p of PHASES) {
    const ds = docs.filter((d) => d.entry?.phase === p.phase);
    if (!ds.length) continue;
    lines.push(`## ${p.phase}. ${p.name}`, '', '| 資料 | 判断したいこと | 残 TODO | 要確認 | 更新日 |', '|---|---|---|---|---|');
    for (const d of ds) lines.push(`| [${d.entry.name}](./${d.file}) | ${d.entry.decision} | ${d.todo.length} | ${d.ask.length} | ${d.fm.updated || '-'} |`);
    lines.push('');
  }
  const asks = docs.flatMap((d) => d.ask.map((a) => `- [${d.entry?.name || d.file}](./${d.file}#L${a.line}): ${a.text.replace(/^[-*|\s]+/, '')}`));
  if (asks.length) lines.push('## 要確認（未決の問い）', '', ...asks, '');
  writeFileSync(join(dir, 'README.md'), lines.join('\n'));
}

function projectName(opt) {
  if (typeof opt.project === 'string') return opt.project;
  try {
    return basename(execFileSync('git', ['rev-parse', '--show-toplevel'], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim());
  } catch {
    return basename(process.cwd());
  }
}

// ---------- commands ----------
const cmds = {
  phases() {
    for (const p of PHASES) {
      const core = CATALOG.filter((c) => c.phase === p.phase && c.core === '1').map((c) => `${c.id} ${c.name}`);
      console.log(`${p.phase}. ${p.name}（${p.focus}）`);
      console.log(`   よくある状況: ${p.signals}`);
      console.log(`   中心の資料: ${core.join(' / ')}`);
    }
    console.log(`OK phases count=${PHASES.length}`);
  },

  list({ opt }) {
    let rows = CATALOG;
    if (typeof opt.phase === 'string') {
      const ps = opt.phase.toUpperCase().split(/[,\s]+/);
      rows = rows.filter((c) => ps.includes(c.phase));
    }
    if (opt.core) rows = rows.filter((c) => c.core === '1');
    for (const c of rows) console.log(`${c.id}\t${c.phase}\t${c.core === '1' ? '中心' : '任意'}\t${c.name}\t${c.decision}${c.calc ? `\t[計算: ${c.calc}]` : ''}`);
    console.log(`OK list count=${rows.length}`);
  },

  new({ pos, opt }) {
    if (!pos.length) die('作る資料が指定されていない', 'new には資料の id か slug が必要', '`node pm.mjs list` で id を確認し `node pm.mjs new 01 02` のように渡す');
    const dir = docsDir(opt);
    mkdirSync(dir, { recursive: true });
    const project = projectName(opt);
    let created = 0;
    let skipped = 0;
    for (const key of pos) {
      const c = findEntry(key);
      if (!c) die(`資料 "${key}" がカタログに無い`, 'id (01〜46)・slug・資料名のいずれかで指定する', '`node pm.mjs list` で確認する');
      const dest = join(dir, fileName(c));
      if (existsSync(dest)) {
        console.log(`SKIP ${dest}（既にある。上書きしない）`);
        skipped++;
        continue;
      }
      const tpl = readFileSync(join(ASSETS, 'templates', fileName(c)), 'utf8');
      writeFileSync(dest, tpl.replaceAll('{{project}}', project).replaceAll('{{date}}', today()));
      console.log(`NEW  ${dest}`);
      created++;
    }
    writeIndex(dir, listDocs(dir));
    console.log(`OK new created=${created} skipped=${skipped} dir=${dir}`);
  },

  sync({ opt }) {
    const dir = docsDir(opt);
    if (!existsSync(dir)) die(`資料のディレクトリ ${dir} が無い`, 'まだ資料を作っていない', '`node pm.mjs new <id>...` で作る (別の場所なら --dir)');
    let calculated = 0;
    for (const d of listDocs(dir)) {
      let text = d.text;
      if (HAS_CALC.test(text)) {
        text = computeDoc(text, d.file);
        calculated++;
      }
      const changed = text !== d.text;
      const mdate = changed ? today() : dateOf(statSync(d.path).mtimeMs);
      if (d.fm.updated && d.fm.updated !== mdate) text = text.replace(/^updated: .*$/m, `updated: ${mdate}`);
      if (text !== d.text) writeFileSync(d.path, text);
    }
    const docs = listDocs(dir);
    writeIndex(dir, docs);
    console.log(`OK sync docs=${docs.length} calculated=${calculated} index=${join(dir, 'README.md')}`);
  },

  status({ opt }) {
    const dir = docsDir(opt);
    const docs = listDocs(dir);
    if (opt.json) {
      console.log(JSON.stringify(docs.map((d) => ({ file: d.file, id: d.entry?.id, phase: d.entry?.phase, name: d.entry?.name, todo: d.todo.length, ask: d.ask.map((a) => a.text), updated: d.fm.updated })), null, 2));
    } else {
      for (const d of docs) console.log(`${d.entry?.phase ?? '?'}\t${d.file}\tTODO ${d.todo.length}\t要確認 ${d.ask.length}\t${d.fm.updated ?? '-'}`);
    }
    const phases = [...new Set(docs.map((d) => d.entry?.phase).filter(Boolean))].sort();
    console.log(`OK status docs=${docs.length} phases=${phases.join(',') || '-'} dir=${dir}`);
  },

  check({ opt }) {
    const dir = docsDir(opt);
    const docs = listDocs(dir);
    if (!docs.length) die(`${dir} に資料が無い`, '検査する対象が無い', '`node pm.mjs new <id>...` で作る (別の場所なら --dir)');
    const errors = [];
    const warns = [];
    const blocks = [];
    for (const d of docs) {
      if (!d.fm.pm_id) errors.push(`${d.file}: frontmatter (pm_id) が無い。テンプレートの先頭を消さない`);
      for (const l of d.todo) errors.push(`${d.file}:${l}: TODO が残っている。分かることで埋め、分からないことは「要確認: <問い>」に書き換える`);
      for (const a of d.ask) warns.push(`${d.file}:${a.line}: ${a.text}`);
      const fences = d.text.split('\n').filter((l) => /^\s*```/.test(l)).length;
      if (fences % 2) errors.push(`${d.file}: コードブロック (\`\`\`) が閉じていない`);
      if (HAS_CALC.test(d.text) && computeDoc(d.text, d.file) !== d.text) errors.push(`${d.file}: 計算結果が表と合っていない。\`node pm.mjs sync\` を実行する`);
      let nth = 0;
      for (const m of d.text.matchAll(/```mermaid\n([\s\S]*?)```/g)) blocks.push({ file: d.file, nth: ++nth, src: m[1] });
    }
    if (opt.render && blocks.length) errors.push(...renderMermaid(blocks));
    for (const w of warns) console.log(`ASK  ${w}`);
    for (const e of errors) console.log(`FAIL ${e}`);
    if (errors.length) {
      process.stderr.write(`ERROR: check で ${errors.length} 件の問題\n  直し方: FAIL 行の指示に従って直し、もう一度 check する\n`);
      process.exit(1);
    }
    console.log(`OK check docs=${docs.length} ask=${warns.length} mermaid=${blocks.length}${opt.render ? ' rendered' : ''}`);
  },
};

function renderMermaid(blocks) {
  // mmdc を使う。PM_MMDC で上書き可 (例: "npx -y @mermaid-js/mermaid-cli@11.17.1")。
  const cmd = (process.env.PM_MMDC || 'npx -y @mermaid-js/mermaid-cli@11.17.1').split(' ');
  const tmp = mkdtempSync(join(tmpdir(), 'pm-mmd-'));
  const errors = [];
  try {
    blocks.forEach((b, i) => {
      const inp = join(tmp, `b${i}.mmd`);
      writeFileSync(inp, b.src);
      const args = [...cmd.slice(1), '-q', '-i', inp, '-o', join(tmp, `b${i}.svg`)];
      if (process.env.PM_PUPPETEER_CONFIG) args.push('-p', process.env.PM_PUPPETEER_CONFIG);
      const r = spawnSync(cmd[0], args, { encoding: 'utf8' });
      if (r.error) die('Mermaid の描画コマンドを起動できない', String(r.error.message), 'node / npx を入れるか、PM_MMDC に mmdc のパスを設定する');
      if (r.status !== 0) {
        const msg = (r.stderr || r.stdout || '').split('\n').find((l) => /error|Parse|Expecting/i.test(l)) || 'mmdc が失敗した';
        errors.push(`${b.file}: Mermaid を描画できない（${b.nth} 番目の図）: ${msg.trim()}`);
      }
    });
  } finally {
    rmSync(tmp, { recursive: true, force: true });
  }
  return errors;
}

const [cmd, ...rest] = process.argv.slice(2);
if (!cmd || !cmds[cmd]) die(`未知のコマンド "${cmd ?? ''}"`, `使えるのは ${Object.keys(cmds).join(' / ')}`, 'scripts/pm.mjs の先頭コメントを参照');
cmds[cmd](parseArgs(rest));
