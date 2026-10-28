/**
 * file-sink.mjs — ローカルのファイルに置く Sink。Node 専用（node:fs）。【実績】
 *
 * 参照実装（Electron アプリ）の次の 2 ファイルを、Electron・TypeScript に依存しない形で写して Sink の 1 実装にしたもの:
 *   - src/main/feedbackFile.ts   → putEntry（yymmdd-xx.md の採番・wx 書き込み・競合の再試行）・list / read / archive
 *   - src/main/feedbackShots.ts  → putAttachments（<YYYYMMDD-HHMMSS>/window.png, element.png）
 * 参照実装の具体パス（読み替える箇所）: 置き場 = <リポジトリ>/feedback、対応済み置き場 = feedback/archived、添付 = <リポジトリ>/.local/tmp/feedback/<日時>/
 * 【追加】で足したもの: list / read / archive（参照実装のアプリ側にはなく、Agent 運用で人・オーケストレータが ls / cat / mv していた操作を関数にした）。
 */
import fs from 'node:fs';
import path from 'node:path';
import { ARCHIVE_DIR, ID_PATTERN, entryId, feedbackDay, nextSeqFromIds, safeDocPath } from './feedback-core.mjs';

export const DEFAULT_INBOX_DIR = 'feedback'; // 参照実装の feedback.dir の既定
export const DEFAULT_ATTACH_DIR = '.local/tmp/feedback'; // 参照実装のスクショの置き場（環境変数で変更）
export const MAX_ATTEMPTS = 10;

const p2 = (n) => String(n).padStart(2, '0');

export function feedbackFileName(day, seq) {
  return `${entryId(day, seq)}.md`;
}

/** dir の書き方の問題（絶対パス・..・空）。問題なしなら null（参照実装 feedbackDirProblem） */
export function feedbackDirProblem(dir) {
  if (typeof dir !== 'string' || dir.trim() === '' || dir.includes('\0')) return '空のパスは使えません';
  if (path.isAbsolute(dir)) return 'リポジトリルートからの相対パスで書いてください';
  const parts = dir.split(/[\\/]+/).filter((x) => x !== '' && x !== '.');
  if (parts.length === 0) return 'リポジトリルートそのものは使えません';
  if (parts.includes('..')) return '.. でリポジトリの外には出せません';
  return null;
}

function idsIn(dir) {
  try {
    return fs.readdirSync(dir).flatMap((name) => {
      const m = /^(\d{6}-\d{2,})(?:\.[^/]*)?$/.exec(name); // 空ファイルも数える（拡張子は問わない）
      return m ? [m[1]] : [];
    });
  } catch {
    return [];
  }
}

/** 次の連番（dir と dir/archived の、同じ日付の最大 + 1） */
export function nextFeedbackSeq(dir, day) {
  return nextSeqFromIds([...idsIn(dir), ...idsIn(path.join(dir, ARCHIVE_DIR))], day);
}

/** 保存先（絶対パス）。ルートの外・シンボリックリンクで外に出るものは投げる */
export function resolveInside(repoRoot, dir) {
  const problem = feedbackDirProblem(dir);
  if (problem) throw new Error(`保存先（${dir}）が使えません: ${problem}`);
  const root = path.resolve(repoRoot);
  const abs = path.resolve(root, dir);
  const rel0 = path.relative(root, abs);
  if (rel0.startsWith('..') || path.isAbsolute(rel0)) throw new Error(`保存先（${dir}）がリポジトリの中ではありません`);
  let probe = abs;
  while (!fs.existsSync(probe) && probe !== root) probe = path.dirname(probe);
  const rel = path.relative(fs.realpathSync(root), fs.realpathSync(probe));
  if (rel.startsWith('..') || path.isAbsolute(rel)) throw new Error(`保存先（${dir}）の実体がリポジトリの外です`);
  return abs;
}
export const resolveFeedbackDir = resolveInside;

/** 名前があるか（シンボリックリンクはたどらない。壊れたリンクもあるとみなす） */
const lexists = (p) => { try { fs.lstatSync(p); return true; } catch { return false; } };
/** 実体のファイルか（シンボリックリンクは false。記録の名前をしたリンクで外のファイルを読ませない） */
const isPlainFile = (p) => { try { return fs.lstatSync(p).isFile(); } catch { return false; } };
/** inode。同じファイルか（rename しても変わらない）を見分ける。無ければ null */
const inodeOf = (p) => { try { return fs.lstatSync(p, { bigint: true }).ino; } catch { return null; } };

/**
 * abs が base（Inbox など）の実体の中に収まることを確かめる。収まらなければ投げる。
 * 既にある一番近い親の実体（シンボリックリンクの先）で比べる。base がまだ無ければ、中に作るものは全部新しいので通す。
 */
export function assertRealInside(base, abs, label = abs) {
  const rel0 = path.relative(base, abs);
  if (rel0.startsWith('..') || path.isAbsolute(rel0)) throw new Error(`${label} は置き場の外です`);
  let probe = abs;
  while (!lexists(probe) && probe !== base) probe = path.dirname(probe);
  if (!lexists(probe)) return abs;
  let real;
  try { real = fs.realpathSync(probe); } catch { throw new Error(`${label} は壊れたシンボリックリンクを通るので、置き場の外かもしれません`); }
  const rel = path.relative(fs.realpathSync(base), real);
  if (rel.startsWith('..') || path.isAbsolute(rel)) throw new Error(`${label} の実体が置き場の外です`);
  return abs;
}

/**
 * 文を <repoRoot>/<dir>/yymmdd-xx.md として追加する。上書きしない（wx）。
 * 先に同じ名前を作られたら番号を数え直して最大 MAX_ATTEMPTS 回まで試す。失敗したら理由つきで投げる。
 * 数えてから書くまでの間に、別のプロセスが同じ番号を保存して archived/ へ移すと、wx は通るのに番号が重なる。
 * そのため archived/ に同じ名前があれば、書く前も書いた後も「取られた」として数え直す。
 * ただし書いた後の archived/ の同じ名前が自分の書いたファイル（inode が同じ）なら、消化する側がもう対応済みにしただけなので、
 * 保存できたとして返す（state: 'archived'）。書けなかったら作ったファイルを消す（空の記録を残さない）。
 */
export function saveFeedbackFile({ repoRoot, dir = DEFAULT_INBOX_DIR, text, now = new Date(), beforeWrite, afterWrite }) {
  const abs = resolveInside(repoRoot, dir);
  fs.mkdirSync(abs, { recursive: true });
  const day = feedbackDay(now);
  const archivedHas = (name) => fs.existsSync(path.join(abs, ARCHIVE_DIR, name));
  let last = '';
  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    const name = feedbackFileName(day, nextFeedbackSeq(abs, day));
    const file = path.join(abs, name);
    beforeWrite?.(file, attempt);
    last = name;
    if (archivedHas(name)) continue;
    const cannot = (err) => new Error(`保存できませんでした: ${path.relative(repoRoot, file)} に書けません（原文: ${err?.message ?? err}）`);
    let fd;
    try {
      fd = fs.openSync(file, 'wx');
    } catch (err) {
      if (err?.code !== 'EEXIST') throw cannot(err);
      continue;
    }
    let mine;
    try {
      fs.writeFileSync(fd, text, 'utf8');
      mine = fs.fstatSync(fd, { bigint: true }).ino;
    } catch (err) {
      fs.closeSync(fd);
      fs.rmSync(file, { force: true });
      throw cannot(err);
    }
    fs.closeSync(fd);
    afterWrite?.(file, attempt); // beforeWrite と同じくテスト用（競合を差し込む）
    // 書いた後にも確かめる。Inbox の同じ名前はもう自分のものなので、ここを通れば以後この番号は重ならない。
    const archivedFile = path.join(abs, ARCHIVE_DIR, name);
    const archivedIno = inodeOf(archivedFile);
    const saved = (f, state) => ({ file: f, rel: path.relative(repoRoot, f).split(path.sep).join('/'), name, state, bytes: Buffer.byteLength(text, 'utf8'), attempts: attempt });
    // 自分の記録が、もう対応済みへ移された（消化する側が見つけてすぐ archive した）。二重に保存しない
    if (archivedIno === mine) return saved(archivedFile, 'archived');
    // 別の記録が同じ番号で archived にある。自分の分を消して数え直す
    if (archivedIno !== null) {
      if (inodeOf(file) === mine) fs.rmSync(file, { force: true });
      continue;
    }
    return saved(file, 'open');
  }
  throw new Error(`${MAX_ATTEMPTS} 回試しても空いている名前を取れませんでした（最後は ${last}）`);
}

// ---- スクリーンショット（参照実装 feedbackShots.ts）----

function stamp(d) {
  return `${d.getFullYear()}${p2(d.getMonth() + 1)}${p2(d.getDate())}-${p2(d.getHours())}${p2(d.getMinutes())}${p2(d.getSeconds())}`;
}

/** <baseDir>/<YYYYMMDD-HHMMSS>/（同じ秒なら -2, -3 …）を作って返す */
export function allocateShotDir(baseDir, now = new Date()) {
  let dir = path.join(baseDir, stamp(now));
  for (let i = 2; fs.existsSync(dir); i++) dir = path.join(baseDir, `${stamp(now)}-${i}`);
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

/** 画像を保存する。失敗しても投げず error に理由を入れる（参照実装と同じ。Node だけでは PNG をデコードできないので、切り抜きは撮る側が cropRect で決める） */
export function saveShots({ windowPng, elementPng, baseDir, now = new Date() }) {
  try {
    if (!windowPng || windowPng.length === 0) return { dir: null, window: null, element: null, error: 'スクリーンショットが空でした' };
    const dir = allocateShotDir(baseDir, now);
    const windowFile = path.join(dir, 'window.png');
    fs.writeFileSync(windowFile, windowPng);
    let elementFile = null;
    if (elementPng && elementPng.length > 0) {
      elementFile = path.join(dir, 'element.png');
      fs.writeFileSync(elementFile, elementPng);
    }
    return { dir, window: windowFile, element: elementFile, error: elementFile ? null : '要素の切り抜きは作っていません' };
  } catch (err) {
    return { dir: null, window: null, element: null, error: err instanceof Error ? err.message : String(err) };
  }
}

// ---- Sink ----

/**
 * @param {{ repoRoot: string, dir?: string, attachDir?: string }} o
 *   dir: 置き場（リポジトリルートからの相対）。attachDir: 添付の置き場（相対。参照実装は .local/tmp/feedback）
 *   上書きする文書（writeDoc / readDoc。メモ機能が使う）は置き場の中（<dir>/<relPath>）に置く。list は直下の記録だけを数えるので混ざらない。
 * @returns {import('./feedback-core.mjs').Sink}
 */
export function createFileSink({ repoRoot, dir = DEFAULT_INBOX_DIR, attachDir = DEFAULT_ATTACH_DIR }) {
  const root = path.resolve(repoRoot);
  const inbox = () => resolveInside(root, dir);
  const rel = (f) => path.relative(root, f).split(path.sep).join('/');
  // 文書は Inbox の実体の中だけ（Inbox の中のリンクが、リポジトリの別の場所を指していても拒否する）。Inbox がまだ無くてもよい
  const docFile = (relPath) => {
    const ib = inbox();
    return assertRealInside(ib, path.join(ib, safeDocPath(relPath)), `文書（${relPath}）`);
  };
  // archived/ も Inbox の実体の中だけ（リンクで外を指していたら、読まない・移さない）
  const archivedDir = () => {
    const ib = inbox();
    return assertRealInside(ib, path.join(ib, ARCHIVE_DIR), `対応済みの置き場（${ARCHIVE_DIR}/）`);
  };
  const locate = (id) => {
    if (!ID_PATTERN.test(id)) throw new Error(`id が不正です: ${id}`); // パス区切りを含められない
    for (const [state, d] of [['open', inbox], ['archived', archivedDir]]) {
      const f = path.join(d(), `${id}.md`);
      if (isPlainFile(f)) return { state, file: f };
    }
    throw new Error(`見つかりません: ${id}`);
  };
  const info = (id, state, file) => {
    const st = fs.statSync(file);
    return { id, ref: rel(file), state, bytes: st.size, createdAt: st.birthtime.toISOString() };
  };
  return {
    kind: 'file',
    capabilities: { put: true, list: true, read: true, archive: true, doc: true },
    async putAttachments(files, { now = new Date() } = {}) {
      const byName = Object.fromEntries(files.map((f) => [f.name, f.data]));
      const r = saveShots({ windowPng: byName['window.png'], elementPng: byName['element.png'], baseDir: resolveInside(root, attachDir), now });
      return { refs: { ...(r.window ? { 'window.png': r.window } : {}), ...(r.element ? { 'element.png': r.element } : {}) }, dir: r.dir, error: r.error };
    },
    async putEntry({ text, now = new Date() }) {
      const s = saveFeedbackFile({ repoRoot: root, dir, text, now });
      return { id: s.name.replace(/\.md$/, ''), ref: s.rel, state: s.state, bytes: s.bytes, createdAt: now.toISOString(), attempts: s.attempts, file: s.file };
    },
    async list({ state = 'all' } = {}) {
      const out = [];
      const scan = (d, st) => {
        let names = [];
        try { names = fs.readdirSync(d); } catch { /* 無ければ空 */ }
        for (const n of names) {
          const m = /^(\d{6}-\d{2,})\.md$/.exec(n);
          if (m && isPlainFile(path.join(d, n))) out.push(info(m[1], st, path.join(d, n)));
        }
      };
      if (state !== 'archived') scan(inbox(), 'open');
      if (state !== 'open') scan(archivedDir(), 'archived');
      return out.sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));
    },
    async read(id) {
      const { state, file } = locate(id);
      return { ...info(id, state, file), text: fs.readFileSync(file, 'utf8') };
    },
    async archive(id, { note } = {}) {
      const { state, file } = locate(id);
      if (state === 'archived') return info(id, state, file);
      const dest = path.join(archivedDir(), `${id}.md`);
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      if (lexists(dest)) throw new Error(`既にあります: ${rel(dest)}`);
      if (note) fs.appendFileSync(file, `\n---\n対応メモ: ${note}\n`);
      fs.renameSync(file, dest); // 参照実装の「mv」
      return info(id, 'archived', dest);
    },
    /** 上書きする文書（メモ）。同じディレクトリの tmp に書いてから rename する（読み手に書きかけが見えない）【追加】 */
    async writeDoc(relPath, text) {
      const abs = docFile(relPath);
      fs.mkdirSync(path.dirname(abs), { recursive: true });
      const tmpFile = `${abs}.${process.pid}.${Math.random().toString(36).slice(2)}.tmp`;
      try {
        fs.writeFileSync(tmpFile, text, 'utf8');
        fs.renameSync(tmpFile, abs);
      } catch (err) {
        fs.rmSync(tmpFile, { force: true });
        throw err;
      }
    },
    /** 無ければ null */
    async readDoc(relPath) {
      const abs = docFile(relPath);
      try {
        return fs.readFileSync(abs, 'utf8');
      } catch (err) {
        if (err?.code === 'ENOENT') return null;
        throw err;
      }
    },
  };
}
