#!/usr/bin/env node
/**
 * board.json を検証し、テンプレートのデータ部を差し替えて 1 枚の HTML を書き出す。
 *
 *   node build-board.mjs <board.json> -o <out.html>
 *
 * 検証に落ちたら黙って直さず、非ゼロ終了して指摘を出す。
 * 図のレイアウトは col / epic の整合が崩れるとまともに描けないため、
 * 「作れてしまうが読めない HTML」を出すよりエラーで止める方がよい。
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const TEMPLATE = resolve(HERE, '../assets/board-template.html');
const START = '/* ===== BOARD DATA START ===== */';
const END = '/* ===== BOARD DATA END ===== */';
const KINDS = ['anchor', 'pr', 'task', 'human', 'idea'];

/* ---- 引数 --------------------------------------------------------------- */
const argv = process.argv.slice(2);
let input = null, out = null, overlay = null, allowEmptyMap = false;
for (let i = 0; i < argv.length; i++) {
  if (argv[i] === '-o' || argv[i] === '--out') out = argv[++i];
  else if (argv[i] === '--overlay') overlay = argv[++i];
  else if (argv[i] === '--allow-empty-map') allowEmptyMap = true;
  else if (!input) input = argv[i];
}
if (!input || !out) {
  console.error('usage: build-board.mjs <board.json> [--overlay <overlay.json>] [--allow-empty-map] -o <out.html>');
  process.exit(2);
}

/* ---- 読み込み ----------------------------------------------------------- */
const load = (p, what) => {
  try { return JSON.parse(readFileSync(resolve(p), 'utf8')); }
  catch (e) { console.error(`${what} を読めません: ${e.message}`); process.exit(2); }
};
let data = load(input, 'board.json');

/* ---- overlay の合成 ------------------------------------------------------
   collect.mjs が出した下書きに、会話コンテキスト由来の差分だけを重ねるための仕組み。
   7KB の board.json を書き直させず、足したいものだけ 10〜20 行書けば済むようにする。

     meta / props      … 浅くマージ
     statuses / epics  … id で upsert（無ければ末尾に追加）
     items             … id で upsert（既存はフィールド単位で上書き）
     edges             … from+to が同じものは置き換え、無ければ追加
     urgency           … あれば丸ごと差し替え
*/
if (overlay) {
  const o = load(overlay, 'overlay.json');
  const upsert = (base, add, key = 'id') => {
    const list = (base || []).slice();
    (add || []).forEach(x => {
      const ix = list.findIndex(y => y[key] === x[key]);
      if (ix >= 0) list[ix] = Object.assign({}, list[ix], x);
      else list.push(x);
    });
    return list;
  };
  data = {
    ...data,
    meta: { ...(data.meta || {}), ...(o.meta || {}) },
    props: { ...(data.props || {}), ...(o.props || {}) },
    statuses: upsert(data.statuses, o.statuses),
    epics: upsert(data.epics, o.epics),
    items: upsert(data.items, o.items),
    urgency: o.urgency || data.urgency,
    edges: (() => {
      const list = (data.edges || []).slice();
      (o.edges || []).forEach(e => {
        const ix = list.findIndex(y => y.from === e.from && y.to === e.to);
        if (ix >= 0) list[ix] = { ...list[ix], ...e }; else list.push(e);
      });
      return list;
    })()
  };
}

/* ---- block の起点を相手の真下へ寄せる ------------------------------------
   「未決がこれを止めている」は、横に長い線より真下から突き上げる矢印の方が読める。
   そのためには起点が相手と同じレーンに居る必要があるので、col / epic は
   書かせずにここで相手から導出する（複数を止めているときは一番左の相手に合わせる）。
   縦に並べる都合で items の末尾へ回す。 */
{
  const items = Array.isArray(data.items) ? data.items : [];
  const at = new Map(items.map(i => [i.id, i]));
  const moved = [];
  (data.edges || []).filter(e => e.kind === 'block').forEach(e => {
    const src = at.get(e.from), dst = at.get(e.to);
    if (!src || !dst || dst.col === undefined) return;
    /* 起点のレーンは常に相手が決める。書かれていた col は当てにしない
       （2 件目以降は一番左の相手に合わせ、残りは横向きの線に落ちる）。 */
    const first = !moved.includes(src.id);
    if (!first && !(dst.col < src.col)) return;
    src.col = dst.col;
    src.epic = dst.epic;
    delete src.anchorY;                   /* レーン内で相手の下に積ませる */
    if (first) moved.push(src.id);
  });
  if (moved.length) {
    data.items = items.filter(i => !moved.includes(i.id))
      .concat(moved.map(id => at.get(id)));
    console.error(`note: block の起点を相手の真下へ寄せた: ${moved.join(', ')}`);
  }
}

/* ---- 検証 --------------------------------------------------------------- */
const errors = [], warnings = [];
const err = m => errors.push(m);
const warn = m => warnings.push(m);

const meta = data.meta || {};
if (!meta.title) err('meta.title が無い');

const statuses = Array.isArray(data.statuses) ? data.statuses : [];
if (!statuses.length) err('statuses が空。カンバンの列が 1 つも作れない');
const statusIds = new Set();
statuses.forEach((s, ix) => {
  ['id', 'label', 'short', 'stroke', 'fill', 'pane'].forEach(k => {
    if (!s[k]) err(`statuses[${ix}] に ${k} が無い`);
  });
  if (s.id) {
    if (statusIds.has(s.id)) err(`statuses の id が重複: ${s.id}`);
    statusIds.add(s.id);
  }
});

const epics = Array.isArray(data.epics) ? data.epics : [];
const epicIds = new Set();
epics.forEach((e, ix) => {
  ['id', 'label', 'fill', 'border'].forEach(k => {
    if (!e[k]) err(`epics[${ix}] に ${k} が無い`);
  });
  if (e.id) {
    if (epicIds.has(e.id)) err(`epics の id が重複: ${e.id}`);
    epicIds.add(e.id);
  }
});

const items = Array.isArray(data.items) ? data.items : [];
if (!items.length) err('items が空');
const itemIds = new Set();
const nextSeen = new Map();
items.forEach((i, ix) => {
  const at = i.id ? `items[${i.id}]` : `items[${ix}]`;
  ['id', 'key', 'title', 'status', 'kind'].forEach(k => {
    if (!i[k]) err(`${at} に ${k} が無い`);
  });
  if (i.id) {
    if (itemIds.has(i.id)) err(`items の id が重複: ${i.id}`);
    itemIds.add(i.id);
  }
  if (i.status && !statusIds.has(i.status)) err(`${at}.status "${i.status}" が statuses に無い`);
  if (i.kind && !KINDS.includes(i.kind)) err(`${at}.kind "${i.kind}" は ${KINDS.join(' / ')} のいずれかにする`);
  if (i.epic && !epicIds.has(i.epic)) err(`${at}.epic "${i.epic}" が epics に無い`);
  if (i.col !== undefined && !Number.isInteger(i.col)) err(`${at}.col は整数にする`);
  if (i.anchorY !== undefined && typeof i.anchorY !== 'number') err(`${at}.anchorY は数値にする`);
  if (i.next !== undefined) {
    if (!Number.isInteger(i.next) || i.next < 1) err(`${at}.next は 1 以上の整数にする`);
    else if (nextSeen.has(i.next)) err(`next=${i.next} が ${nextSeen.get(i.next)} と ${i.id} で重複`);
    else nextSeen.set(i.next, i.id);
  }
  (i.links || []).forEach((l, li) => {
    if (!l.url) err(`${at}.links[${li}] に url が無い`);
    if (!l.label) err(`${at}.links[${li}] に label が無い`);
  });
  (i.children || []).forEach((c, ci) => {
    if (!c.key || !c.title) err(`${at}.children[${ci}] は {key,title} が要る`);
  });
  if (i.ask !== undefined && typeof i.ask !== 'string' && !Array.isArray(i.ask)) {
    err(`${at}.ask は文字列か文字列の配列にする`);
  }
});

const byId = new Map(items.filter(i => i.id).map(i => [i.id, i]));

/* エピックのレーンは連続していないと図の枠が重なる。
   「そのエピックが使う col の範囲に、他のエピック / エピック無しのノードが居ない」
   を条件として見る。 */
epics.forEach(e => {
  const mem = items.filter(i => i.epic === e.id && i.col !== undefined);
  if (!mem.length) { warn(`epics "${e.id}" に図へ出るチケットが無い（枠は描かれない）`); return; }
  const lo = Math.min(...mem.map(m => m.col)), hi = Math.max(...mem.map(m => m.col));
  const intruders = items.filter(i => i.col !== undefined && i.col >= lo && i.col <= hi && i.epic !== e.id);
  if (intruders.length) {
    const noEpic = intruders.filter(i => !i.epic).map(i => i.id);
    err(`epics "${e.id}" の col 範囲 ${lo}..${hi} に別のノードが入っている: `
      + intruders.map(i => `${i.id}(col=${i.col}${i.epic ? `, epic=${i.epic}` : ''})`).join(', ')
      + (noEpic.length
          ? ` → 直し方: ${noEpic.join(' / ')} に "epic": "${e.id}" を付ける（その枠の中に置く）か、`
            + `枠の外の col (${lo - 1} 以下 か ${hi + 1} 以上) にずらす`
          : ' → 直し方: 同じエピックのメンバーの col が連続するように振り直す'));
  }
});

const edges = Array.isArray(data.edges) ? data.edges : [];
edges.forEach((e, ix) => {
  const at = `edges[${ix}]`;
  if (!e.from || !e.to) { err(`${at} に from / to が無い`); return; }
  const a = byId.get(e.from), b = byId.get(e.to);
  if (!a) { err(`${at}.from "${e.from}" が items に無い`); return; }
  if (!b) { err(`${at}.to "${e.to}" が items に無い`); return; }
  if (a.col === undefined) err(`${at}.from "${e.from}" は col を持たない（図に出ないので線を引けない）`);
  if (b.col === undefined) err(`${at}.to "${e.to}" は col を持たない（図に出ないので線を引けない）`);
  /* block は同じレーンの下から上へ引く（col は相手から導出済み）。
     flow / idea は横向きなので左レーン → 右レーンに限る。 */
  if (a.col !== undefined && b.col !== undefined && e.kind !== 'block' && a.col >= b.col) {
    err(`${at} は ${e.from}(col=${a.col}) → ${e.to}(col=${b.col})。依存線は左レーン → 右レーンだけにする`);
  }
  if (e.kind && !['flow', 'block', 'idea'].includes(e.kind)) {
    err(`${at}.kind "${e.kind}" は flow / block / idea のいずれかにする`);
  }
});

/* 図に出る「作業」ノードが無いボードは、カンバンに作業がある限りほぼミス。
   main などのアンカーだけの図は何も伝えないので、warn ではなくエラーで止める。 */
{
  const mapWork = items.filter(i => i.col !== undefined && i.kind !== 'anchor');
  const kanbanWork = items.filter(i => i.kanban !== false && i.kind !== 'anchor');
  if (!mapWork.length && kanbanWork.length && !allowEmptyMap) {
    err(`図に出る作業ノードが 1 つも無い（アンカーのみ）。カンバンには ${kanbanWork.length} 件ある`
      + ' → 直し方: collect.mjs を再実行する（open PR が無いときは直近 merged PR を自動で図に出す）か、'
      + '図に出したい item に overlay で col を振る。意図して空の図にするなら --allow-empty-map を付ける');
  } else if (!items.some(i => i.col !== undefined)) {
    warn('col を持つ items が無い。図が空になる');
  }
}

const urgency = Array.isArray(data.urgency) && data.urgency.length
  ? data.urgency
  : statuses.map(s => s.id);
urgency.forEach(u => { if (!statusIds.has(u)) err(`urgency の "${u}" が statuses に無い`); });
statuses.forEach(s => { if (!urgency.includes(s.id)) warn(`urgency に "${s.id}" が無い（畳んだエピックの代表色が決まらない場合がある）`); });

if (warnings.length) console.error(warnings.map(w => `warn: ${w}`).join('\n'));
if (errors.length) {
  console.error(`board.json の検証に失敗しました (${errors.length} 件):`);
  console.error(errors.map(e => `  - ${e}`).join('\n'));
  process.exit(1);
}

/* ---- データ部の生成 ------------------------------------------------------ */
const lit = v => JSON.stringify(v, null, 2);
const props = Object.assign({ showChildren: false, dimUnrelated: true, nodeWidth: 198 }, data.props || {});
if (props.nodeWidth < 160 || props.nodeWidth > 260) {
  console.error(`warn: props.nodeWidth=${props.nodeWidth} は 160〜260 の想定外`);
}

const block = [
  START,
  `const META = ${lit({
    title: meta.title,
    repoUrl: meta.repoUrl || '',
    repoLabel: meta.repoLabel || meta.repoUrl || '',
    updatedAt: meta.updatedAt || ''
  })};`,
  '',
  `const PROPS = ${lit(props)};`,
  '',
  `const STATUSES = ${lit(statuses)};`,
  '',
  `const EPICS = ${lit(epics)};`,
  '',
  `const URGENCY = ${lit(urgency)};`,
  '',
  `const ITEMS = ${lit(items)};`,
  '',
  `const EDGES = ${lit(edges)};`,
  END
].join('\n');

/* ---- 差し替えて書き出し -------------------------------------------------- */
const tpl = readFileSync(TEMPLATE, 'utf8');
const s = tpl.indexOf(START), e = tpl.indexOf(END);
if (s < 0 || e < 0) {
  console.error('テンプレートに BOARD DATA のマーカーが見つかりません');
  process.exit(2);
}
const html = tpl.slice(0, s) + block + tpl.slice(e + END.length);

const outPath = resolve(out);
mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, html, 'utf8');

const onMap = items.filter(i => i.col !== undefined).length;
const onKanban = items.filter(i => i.kanban !== false).length;
console.log(`${outPath}`);
console.error(`  items ${items.length} (図 ${onMap} / カンバン ${onKanban}) · edges ${edges.length} · epics ${epics.length} · statuses ${statuses.length}`);
