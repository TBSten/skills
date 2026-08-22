#!/usr/bin/env node
/**
 * GitHub (gh) と git ローカルの状態を集めて board.json の下書きを書き出す。
 *
 *   node collect.mjs -o <board.json> [--days 7] [--title "..."] [--all] [--no-gh]
 *
 * 生の gh 出力を呼び出し元の context に載せないためのスクリプト。
 * stdout には出力パス、stderr には 1 件 1 行の要約だけを出す。
 *
 * 速度方針:
 *   - GitHub への問い合わせは **GraphQL 1 往復だけ**。PR 一覧・CI・レビュー状態・
 *     未 resolve コメント・issue をまとめて取る。`gh` の起動は 1 回 ≈ 1.2s なので、
 *     PR ごとに引くと本数に比例して遅くなる。
 *   - owner/repo は `git remote` から取る（`gh repo view` の往復を省く）。
 *   - git はローカルなので速いが、それでも並列で回す。
 *
 * ここで決まるのは「機械的に決まる部分」だけ:
 *   - PR / issue / ローカルブランチ の items
 *   - CI・レビュー状態からの status 判定
 *   - stacked PR (base が別 PR の head) からの col と edges
 *
 * 会話コンテキスト由来のノード (kind:'human' の人間待ち・未決、kind:'idea' の構想)、
 * epics、next、ask は **呼び出し元が後から足す**。そこがこの図の価値の中心なので、
 * スクリプトでは推測しない。
 */
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const pexec = promisify(execFile);

/* ---- 引数 --------------------------------------------------------------- */
const argv = process.argv.slice(2);
let out = null, days = 7, title = null, useGh = true, all = false;
for (let i = 0; i < argv.length; i++) {
  const a = argv[i];
  if (a === '-o' || a === '--out') out = argv[++i];
  else if (a === '--days') days = Number(argv[++i]);
  else if (a === '--title') title = argv[++i];
  else if (a === '--all') all = true;
  else if (a === '--no-gh') useGh = false;
}
if (!out) {
  console.error('usage: collect.mjs -o <board.json> [--days 7] [--title "..."] [--all] [--no-gh]');
  process.exit(2);
}

const MAX_MERGED = all ? 40 : 6;
const MAX_ISSUES = all ? 100 : 10;
const MAX_BRANCHES = all ? 100 : 8;
const since = Date.now() - days * 86400000;
const notes = [];

const git = async (...args) => {
  try { return (await pexec('git', args, { maxBuffer: 32 << 20 })).stdout.trim(); }
  catch { return null; }
};

/* ---- リポジトリ（ローカルの remote から。gh の往復を使わない） ------------ */
const remote = await git('remote', 'get-url', 'origin');
const m = remote && remote.match(/github\.com[:/]([^/]+)\/(.+?)(?:\.git)?$/);
const owner = m?.[1] || '';
const name = m?.[2] || '';
const repoUrl = m ? `https://github.com/${owner}/${name}` : '';
const repoLabel = m ? `${owner}/${name}` : (remote || '');
const shortName = name || 'repo';

/* ---- GitHub: GraphQL 1 往復ですべて取る ---------------------------------- */
const QUERY = `
query($o:String!,$r:String!,$openN:Int!,$mergedN:Int!,$issueN:Int!){
  repository(owner:$o,name:$r){
    defaultBranchRef{ name }
    open: pullRequests(states:OPEN, first:$openN, orderBy:{field:UPDATED_AT,direction:DESC}){
      nodes{
        number title url isDraft updatedAt headRefName baseRefName reviewDecision
        reviewThreads(first:100){ nodes{ isResolved isOutdated } }
        commits(last:1){ nodes{ commit{ statusCheckRollup{ contexts(first:100){ nodes{
          __typename
          ... on CheckRun{ status conclusion }
          ... on StatusContext{ state }
        } } } } } }
      }
    }
    merged: pullRequests(states:MERGED, first:$mergedN, orderBy:{field:UPDATED_AT,direction:DESC}){
      nodes{ number title url updatedAt mergedAt headRefName baseRefName }
    }
    issues(states:OPEN, first:$issueN, orderBy:{field:UPDATED_AT,direction:DESC}){
      nodes{ number title url updatedAt assignees(first:1){ totalCount } }
    }
  }
}`;

async function fetchGitHub() {
  if (!useGh || !owner || !name) return null;
  try {
    const { stdout } = await pexec('gh', [
      'api', 'graphql',
      '-f', `query=${QUERY}`,
      '-F', `o=${owner}`, '-F', `r=${name}`,
      '-F', 'openN=50', '-F', `mergedN=${Math.max(MAX_MERGED, 20)}`, '-F', `issueN=${Math.max(MAX_ISSUES, 30)}`
    ], { maxBuffer: 64 << 20 });
    return JSON.parse(stdout).data?.repository || null;
  } catch (e) {
    notes.push('gh の GraphQL 問い合わせに失敗した。git ローカルの情報だけで作成した');
    return null;
  }
}

/* GitHub と git ローカルを同時に走らせる */
const [gh, branchLines, currentBranch, headBranch] = await Promise.all([
  fetchGitHub(),
  git('for-each-ref', '--sort=-committerdate',
      '--format=%(refname:short)\t%(upstream:short)\t%(committerdate:iso8601)\t%(upstream:track)', 'refs/heads'),
  git('branch', '--show-current'),
  git('symbolic-ref', '--short', 'refs/remotes/origin/HEAD')
]);

const defaultBranch = gh?.defaultBranchRef?.name || headBranch?.split('/').pop() || 'main';

/* ---- PR --------------------------------------------------------------- */
/* CI の集計。CheckRun と StatusContext の両方の形に対応する */
function ci(pr) {
  const rows = pr.commits?.nodes?.[0]?.commit?.statusCheckRollup?.contexts?.nodes || [];
  let pass = 0, fail = 0, pending = 0;
  for (const c of rows) {
    const v = String(c.conclusion || c.state || '').toUpperCase();
    const running = c.status && String(c.status).toUpperCase() !== 'COMPLETED';
    if (running || v === 'PENDING' || v === '') pending++;
    else if (['SUCCESS', 'NEUTRAL', 'SKIPPED'].includes(v)) pass++;
    else fail++;
  }
  return { total: rows.length, pass, fail, pending };
}
const unresolved = pr =>
  (pr.reviewThreads?.nodes || []).filter(t => !t.isResolved && !t.isOutdated).length;

/* status は上から順に評価し、最初に当たったものを採る */
function statusOf(pr, c, nits, merged) {
  if (merged) return 'closed';
  if (c.fail > 0) return 'blocked';
  if (pr.isDraft) return 'implementing';
  if (nits > 0) return 'nits';
  if (pr.reviewDecision === 'APPROVED' && c.fail === 0 && c.pending === 0) return 'merge-wait';
  return 'review';
}

const openPrs = gh?.open?.nodes || [];
const mergedPrs = (gh?.merged?.nodes || [])
  .filter(p => p.mergedAt && Date.parse(p.mergedAt) >= since)
  .slice(0, MAX_MERGED);

const prItems = [...openPrs, ...mergedPrs].map(pr => {
  const merged = !!pr.mergedAt;
  const c = ci(pr);
  const nits = merged ? 0 : unresolved(pr);
  const meta = [];
  if (c.total) meta.push(`CI ${c.pass}/${c.total} pass` + (c.fail ? ` · 失敗 ${c.fail}` : '') + (c.pending ? ` · 待ち ${c.pending}` : ''));
  if (nits) meta.push(`未 resolve コメント ${nits} 件`);
  if (pr.isDraft) meta.push('draft');
  if (merged) meta.push('merged');
  return {
    id: `pr${pr.number}`,
    key: `${shortName}#${pr.number}`,
    title: pr.title,
    kind: 'pr',
    status: statusOf(pr, c, nits, merged),
    merged,
    head: pr.headRefName,
    base: pr.baseRefName,
    meta,
    links: [{ label: `PR #${pr.number}`, url: pr.url, note: pr.title }],
    updated: (pr.updatedAt || '').slice(0, 16).replace('T', ' ')
  };
});

/* ---- stacked PR から col と edges を決める ------------------------------- */
/* base が別 PR の head なら、その PR に積まれている = 依存がある */
const byHead = new Map();
prItems.forEach(i => { if (!byHead.has(i.head)) byHead.set(i.head, i); });
const parentOf = i => { const p = byHead.get(i.base); return p && p !== i ? p : null; };

/* 図に載せるのは「生きている作業」だけ。merged PR は open PR の土台になっている
   ときだけ図に出し、それ以外はカンバンだけに置く。 */
const onMap = new Set(prItems.filter(i => !i.merged).map(i => i.id));
for (let grew = true; grew;) {
  grew = false;
  for (const i of prItems) {
    if (!onMap.has(i.id)) continue;
    const p = parentOf(i);
    if (p && !onMap.has(p.id)) { onMap.add(p.id); grew = true; }
  }
}
/* open PR が 1 本も無いと図が main だけの空図になり「何も映っていない」ボードが出る。
   その場合は期間内の merged PR を図に出し、直近なにが main に入ったかを見せる。 */
if (onMap.size === 0 && prItems.some(i => i.merged)) {
  prItems.forEach(i => { if (i.merged) onMap.add(i.id); });
  notes.push('open PR が無いので、直近 merged PR を図に出した（空図防止）');
}

const depthOf = (i, seen = new Set()) => {
  if (seen.has(i.id)) return 1;                     /* 循環よけ */
  seen.add(i.id);
  const p = parentOf(i);
  return p && onMap.has(p.id) ? depthOf(p, seen) + 1 : 1;
};

const items = [{
  id: 'main', key: 'branch', title: defaultBranch, kind: 'anchor', status: 'closed',
  col: 0, kanban: false,
  desc: ['取り込み先。'], updated: new Date().toISOString().slice(0, 10)
}];
const edges = [];
prItems.forEach(i => {
  const { merged, head, base, ...rest } = i;
  if (onMap.has(i.id)) {
    rest.col = depthOf(i);
    const p = parentOf(i);
    edges.push({ from: p && onMap.has(p.id) ? p.id : 'main', to: i.id });
  }
  items.push(rest);
});

/* ---- epics / next の既定を決める ----------------------------------------
   呼び出し元に考えさせないための既定値。そのまま使ってよく、会話で分かっている
   ことがあるときだけ上書きする。 */
const epics = [];
{
  /* stacked PR の連なり（2 本以上）を 1 エピックにする。
     ただし col 範囲に別のノードが入ると図の枠が重なるので、その場合は作らない。 */
  const mapItems = items.filter(i => i.col !== undefined && i.id !== 'main');
  const chains = new Map();                     /* 根の id -> メンバー */
  prItems.filter(i => onMap.has(i.id)).forEach(i => {
    let root = i, guard = 0;
    while (parentOf(root) && onMap.has(parentOf(root).id) && guard++ < 50) root = parentOf(root);
    if (!chains.has(root.id)) chains.set(root.id, []);
    chains.get(root.id).push(i);
  });
  let n = 0;
  for (const [rootId, mem] of chains) {
    if (mem.length < 2) continue;
    const cols = mem.map(x => items.find(i => i.id === x.id).col);
    const lo = Math.min(...cols), hi = Math.max(...cols);
    const ids = new Set(mem.map(x => x.id));
    if (mapItems.some(i => i.col >= lo && i.col <= hi && !ids.has(i.id))) continue;  /* 枠が重なる */
    const nums = mem.map(x => x.key.split('#')[1]).filter(Boolean);
    const id = `stack${++n}`;
    epics.push({ id, label: `PR 積み上げ #${nums[0]} → #${nums[nums.length - 1]}`, fill: '#EAF6F7', border: '#AECAD5' });
    mem.forEach(x => { items.find(i => i.id === x.id).epic = id; });
  }
}

const URGENCY = ['blocked', 'discussing', 'review', 'nits', 'implementing', 'merge-wait', 'backlog', 'closed'];
{
  /* 図に出ている未完了のものを緊急度順に並べ、上位 3 件へ NEXT を振る */
  const rank = s => { const i = URGENCY.indexOf(s); return i < 0 ? 99 : i; };
  items.filter(i => i.col !== undefined && i.status !== 'closed' && i.id !== 'main')
    .sort((a, b) => rank(a.status) - rank(b.status) || String(b.updated).localeCompare(String(a.updated)))
    .slice(0, 3)
    .forEach((i, ix) => { i.next = ix + 1; });
}

/* ---- issue -------------------------------------------------------------- */
/* 図には出さずカンバンだけに出す。関係するものは呼び出し元が col を振って図へ上げる */
const rawIssues = gh?.issues?.nodes || [];
const issues = (all ? rawIssues
  : rawIssues.filter(is => Date.parse(is.updatedAt) >= since || (is.assignees?.totalCount || 0) > 0))
  .slice(0, MAX_ISSUES);
issues.forEach(is => {
  items.push({
    id: `issue${is.number}`, key: `#${is.number}`, title: is.title,
    kind: 'task', status: 'backlog',
    links: [{ label: `issue #${is.number}`, url: is.url, note: is.title }],
    updated: (is.updatedAt || '').slice(0, 10)
  });
});

/* ---- git ローカル -------------------------------------------------------- */
/* PR になっていない作業を拾う。カンバンだけに出す。
   `%(upstream:track)` を使うので、ブランチごとに rev-list を回さない。 */
const prHeads = new Set(prItems.map(i => i.head));
const rawBranches = (branchLines || '').split('\n').filter(Boolean)
  .map(l => { const [name, upstream, date, track] = l.split('\t'); return { name, upstream, date, track }; })
  .filter(b => b.name !== defaultBranch && !prHeads.has(b.name));
const branches = (all ? rawBranches
  : rawBranches.filter(b => b.name === currentBranch || Date.parse(b.date) >= since))
  .slice(0, MAX_BRANCHES);

branches.forEach(b => {
  const ahead = (b.track || '').match(/ahead (\d+)/)?.[1];
  const meta = [];
  if (ahead) meta.push(`未 push ${ahead} commit`);
  if (!b.upstream) meta.push('upstream 無し');
  if (b.name === currentBranch) meta.push('作業中');
  items.push({
    id: `branch-${b.name.replace(/[^\w.-]+/g, '-')}`,
    key: 'branch', title: b.name, kind: 'task',
    status: b.name === currentBranch ? 'implementing' : 'backlog',
    meta,
    desc: [`PR になっていないローカルブランチ。最終コミット ${b.date.slice(0, 10)}。`],
    updated: b.date.slice(0, 10)
  });
});

/* ---- 書き出し ------------------------------------------------------------ */
const STATUSES = [
  { id: 'discussing',   label: '調査中 / 議論中', short: '議論中',     stroke: '#C69A08', fill: '#FBF2D8', pane: '#FBF6E6' },
  { id: 'implementing', label: '実装中',          short: '実装中',     stroke: '#3F4B7F', fill: '#DBEFF1', pane: '#EAF6F7' },
  { id: 'review',       label: 'PR レビュー中',   short: 'レビュー中', stroke: '#E0A32E', fill: '#FCF3E4', pane: '#FBF5EA' },
  { id: 'nits',         label: 'nits 対応中',     short: 'nits',       stroke: '#9EB7C9', fill: '#EAF6F7', pane: '#F5FBFC' },
  { id: 'merge-wait',   label: 'マージ待ち',      short: 'マージ待ち', stroke: '#3E9E7A', fill: '#E7F3EE', pane: '#EDF6F2' },
  { id: 'blocked',      label: 'ブロック中',      short: 'ブロック',   stroke: '#E2596B', fill: '#FCEBEE', pane: '#FCF0F2' },
  { id: 'backlog',      label: '未着手',          short: '未着手',     stroke: '#849AB5', fill: '#F7FAFB', pane: '#F7FAFB' },
  { id: 'closed',       label: '完了 / クローズ', short: '完了',       stroke: '#C4D0D3', fill: '#EDF2F3', pane: '#F7FAFB', collapsedByDefault: true }
];

const now = new Date();
const pad = n => String(n).padStart(2, '0');
const board = {
  meta: {
    title: title || `${shortName} — 作業状況`,
    repoUrl, repoLabel,
    updatedAt: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
  },
  props: { showChildren: false, dimUnrelated: true, nodeWidth: 198 },
  statuses: STATUSES,
  epics,
  urgency: URGENCY,
  items, edges
};

const outPath = resolve(out);
mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, JSON.stringify(board, null, 2), 'utf8');

/* stderr は 1 件 1 行だけ。生の gh 出力は絶対にここへ出さない */
const mapped = items.filter(i => i.col !== undefined).length;
console.error(`収集: PR ${prItems.length} (open ${openPrs.length} / merged ${mergedPrs.length}) · issue ${issues.length} · branch ${branches.length} · edges ${edges.length} · 図 ${mapped} · epic ${epics.length} · NEXT ${items.filter(i => i.next).length}`);
const LIST_CAP = 30;
items.slice(0, LIST_CAP).forEach(i => {
  console.error(`  ${(i.col === undefined ? '--' : 'c' + i.col).padEnd(3)} ${i.status.padEnd(12)} ${i.id.padEnd(20)} ${i.title}`);
});
if (items.length > LIST_CAP) console.error(`  ... 他 ${items.length - LIST_CAP} 件（board.json 参照）`);
const dropI = rawIssues.length - issues.length, dropB = rawBranches.length - branches.length;
if (dropI > 0) notes.push(`${days} 日以内に動きの無い issue ${dropI} 件を除外した（--all で全件）`);
if (dropB > 0) notes.push(`${days} 日以内に動きの無いローカルブランチ ${dropB} 本を除外した（--all で全件）`);
notes.forEach(n => console.error(`note: ${n}`));
console.error('epics / next は既定を入れてある。そのまま使ってよい。');
console.error('足すのは会話で分かっていることだけ: 人間待ち (kind:"human")・未決 (+ ask)・構想 (kind:"idea")');
console.log(outPath);
