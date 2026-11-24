#!/usr/bin/env node
// chrome-extension.mjs — フィードバックモードの Chrome 拡張（ローカルで読み込んで使う）を作る・skill の版に合わせる・検査する cli。
// node の標準だけで動く（Node 18 以上、依存なし）。この script を読解・書き換え・再実装せず、そのまま実行する。
// 形と流れは references/setup/web/chrome-extension.md。
//
//   node chrome-extension.mjs new <dir> [--name <拡張の名前>] [--force]   ひな形から作る（<dir> に何かあれば中断。--force で上書き）
//   node chrome-extension.mjs sync <dir>                                   vendor/（skill の DOM 版とコア）だけを skill の版で上書きする
//   node chrome-extension.mjs check <dir>                                  検査する（manifest・必要なファイル・vendor が skill の版と同じか）
//
// 人が書き換えてよいのは manifest.json・background.js・content.js（の CONFIG）。vendor/ は sync が上書きする。
// 終了コード: 0 = 成功 / 1 = 検査に落ちた・失敗 / 2 = 使い方の誤り。
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const TEMPLATES = path.join(HERE, '..', 'assets', 'templates');
export const OWN = ['manifest.json', 'background.js', 'content.js'];
export const VENDOR = {
  'vendor/feedback-mode.js': path.join(TEMPLATES, 'adapters', 'web', 'dom', 'feedback-mode.js'),
  'vendor/feedback-core.mjs': path.join(TEMPLATES, 'core', 'feedback-core.mjs'),
};
const SRC = path.join(TEMPLATES, 'adapters', 'web', 'chrome-extension');

function copyVendor(dir) {
  const changed = [];
  for (const [rel, src] of Object.entries(VENDOR)) {
    const dst = path.join(dir, rel);
    const next = fs.readFileSync(src);
    if (fs.existsSync(dst) && Buffer.compare(fs.readFileSync(dst), next) === 0) continue;
    fs.mkdirSync(path.dirname(dst), { recursive: true });
    fs.writeFileSync(dst, next);
    changed.push(rel);
  }
  return changed;
}

export function create(dir, { name, force = false } = {}) {
  if (fs.existsSync(dir) && fs.readdirSync(dir).length && !force) throw new Error(`${dir} は空ではありません。上書きするなら --force を付けてください`);
  fs.mkdirSync(dir, { recursive: true });
  for (const f of OWN) {
    let text = fs.readFileSync(path.join(SRC, f), 'utf8');
    if (f === 'manifest.json' && name) { const m = JSON.parse(text); m.name = name; text = `${JSON.stringify(m, null, 2)}\n`; }
    fs.writeFileSync(path.join(dir, f), text);
  }
  copyVendor(dir);
}

export const sync = (dir) => {
  if (!fs.existsSync(path.join(dir, 'manifest.json'))) throw new Error(`${dir} に manifest.json がありません。先に new で作ってください`);
  return copyVendor(dir);
};

/** 検査。{ errors, warnings } を返す */
export function check(dir) {
  const errors = [];
  const warnings = [];
  for (const f of [...OWN, ...Object.keys(VENDOR)]) if (!fs.existsSync(path.join(dir, f))) errors.push(`${f}: ありません（new で作るか、vendor は sync で入れてください）`);
  let m = null;
  try { m = JSON.parse(fs.readFileSync(path.join(dir, 'manifest.json'), 'utf8')); } catch (e) { if (fs.existsSync(path.join(dir, 'manifest.json'))) errors.push(`manifest.json: JSON として読めません（${e.message}）`); }
  if (m) {
    if (m.manifest_version !== 3) errors.push('manifest.json: manifest_version は 3 にしてください');
    for (const k of ['name', 'version']) if (typeof m[k] !== 'string' || !m[k]) errors.push(`manifest.json: ${k} が要ります`);
    if (m.background?.service_worker !== 'background.js') warnings.push('manifest.json: background.service_worker が background.js ではありません（ボタンで content.js を入れるのは background.js）');
    const war = (m.web_accessible_resources ?? []).some((w) => (w.resources ?? []).some((r) => r === 'vendor/*' || r.startsWith('vendor/')));
    if (!war) errors.push('manifest.json: web_accessible_resources に vendor/* が要ります（content.js が import する）');
    for (const need of ['activeTab', 'scripting', 'clipboardWrite']) if (!(m.permissions ?? []).includes(need)) errors.push(`manifest.json: permissions に ${need} が要ります（activeTab: 押したタブに入る / scripting: content.js を入れる / clipboardWrite: ページの許可なしでコピーする）`);
    const broad = [...(m.host_permissions ?? []), ...(m.content_scripts ?? []).flatMap((c) => c.matches ?? [])].filter((p) => p === '<all_urls>' || p.startsWith('*://*/') || p.startsWith('http://*/') || p.startsWith('https://*/'));
    if (broad.length) warnings.push(`manifest.json: どのサイトにも入る権限があります（${broad.join(', ')}）。ボタンを押したタブにだけ入る activeTab で足りないか確かめてください`);
  }
  for (const [rel, src] of Object.entries(VENDOR)) {
    const dst = path.join(dir, rel);
    if (fs.existsSync(dst) && Buffer.compare(fs.readFileSync(dst), fs.readFileSync(src)) !== 0) warnings.push(`${rel}: skill の版と違います（skill を更新したなら sync。自分で直したなら、その直しは content.js の側に移してください）`);
  }
  return { errors, warnings };
}

function main(argv) {
  const [cmd, ...rest] = argv;
  const flags = {}; const pos = [];
  for (let i = 0; i < rest.length; i++) { const a = rest[i]; if (a === '--force') flags.force = true; else if (a.startsWith('--')) flags[a.slice(2)] = rest[++i]; else pos.push(a); }
  const usage = 'usage: chrome-extension.mjs new <dir> [--name n] [--force] | sync <dir> | check <dir>';
  const fail = (msg, code = 1) => { console.error(`chrome-extension: ${msg}`); process.exit(code); };
  if (!pos[0] || !['new', 'sync', 'check'].includes(cmd)) fail(usage, 2);
  const dir = path.resolve(pos[0]);
  try {
    if (cmd === 'new') {
      create(dir, { name: flags.name, force: flags.force });
      console.log(`OK: ${dir} に作りました。chrome://extensions →「デベロッパー モード」→「パッケージ化されていない拡張機能を読み込む」でこのディレクトリを選んでください`);
    } else if (cmd === 'sync') {
      const changed = sync(dir);
      console.log(changed.length ? `OK: 上書きしました: ${changed.join(', ')}。chrome://extensions で拡張を再読み込みしてください` : 'OK: vendor は skill の版と同じです');
    } else {
      const { errors, warnings } = check(dir);
      for (const w of warnings) console.error(`警告: ${w}`);
      if (errors.length) fail(`検査に落ちました:\n  ${errors.join('\n  ')}`);
      console.log('OK');
    }
  } catch (e) { fail(e.message); }
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main(process.argv.slice(2));
