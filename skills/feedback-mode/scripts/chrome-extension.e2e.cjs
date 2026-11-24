// chrome-extension.e2e.cjs — ひな形から作った Chrome 拡張を Chromium に読み込み、本物のキー（Alt+Shift+F）とマウスで確かめる。
//   node chrome-extension.e2e.cjs [<拡張のディレクトリ>]   既定: 一時ディレクトリに chrome-extension.mjs new で作って使う
// 要るもの: Playwright の Chromium（拡張を読み込めるのは画面ありだけ）・xdotool（拡張のショートカットは OS のキーでしか動かない）。
// DISPLAY が無ければ xvfb-run の中で自分を起動し直す。どれかが無ければ SKIP。
// 確かめること: キーで入る → 部品を選んでも押されない → 書く → Ctrl+Enter → クリップボードに記録（schema 1・文・部品の道筋・URL）
//   → 出る / リンクを選んでも移動しない / もう一度のキーで出る / Esc で出る / CSP の厳しいページでも動く / ページのエラーが無い
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
const { execFileSync, spawnSync } = require('node:child_process');

const has = (cmd) => spawnSync('sh', ['-c', `command -v ${cmd}`]).status === 0;
if (!process.env.DISPLAY) {
  if (!has('xvfb-run')) { console.log('SKIP: 画面（DISPLAY）も xvfb-run も無い'); process.exit(0); }
  const r = spawnSync('xvfb-run', ['-a', process.execPath, __filename, ...process.argv.slice(2)], { stdio: 'inherit' });
  process.exit(r.status ?? 1);
}
if (!has('xdotool')) { console.log('SKIP: xdotool が無い'); process.exit(0); }
let chromium;
try { ({ chromium } = require('playwright')); } catch { console.log('SKIP: playwright が無い'); process.exit(0); }

const assert = (c, m) => { if (!c) throw new Error(`NG: ${m}`); console.log(`ok - ${m}`); };
const ext = process.argv[2] ? path.resolve(process.argv[2]) : (() => {
  const dir = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'fb-ext-')), 'ext');
  execFileSync(process.execPath, [path.join(__dirname, 'chrome-extension.mjs'), 'new', dir]);
  return dir;
})();

// content.js の CONFIG.pointer を書き換えていれば、記録に入ることも確かめる（人のカスタムが効くか）
const pointer = /pointer: '([^']*)'/.exec(fs.readFileSync(path.join(ext, 'content.js'), 'utf8'))?.[1] ?? '';

const PAGE = `<!doctype html><html lang="ja"><head><meta charset="utf-8"><title>Shop sample</title></head>
<body><h1 data-fb="ホーム/見出し">お店</h1><p>商品の一覧です。</p>
<a id="link" href="/other" data-fb="ホーム/リンク">ほかのページ</a>
<button id="buy" data-fb="ホーム/買う" onclick="document.title='bought'">買う</button></body></html>`;

(async () => {
  const server = http.createServer((req, res) => {
    const csp = req.url.startsWith('/csp') ? { 'content-security-policy': "default-src 'self'; script-src 'self'" } : {};
    res.writeHead(200, { 'content-type': 'text/html; charset=utf-8', ...csp });
    res.end(PAGE);
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const ctx = await chromium.launchPersistentContext(fs.mkdtempSync(path.join(os.tmpdir(), 'fb-prof-')), {
    headless: false, viewport: null, args: [`--disable-extensions-except=${ext}`, `--load-extension=${ext}`, '--window-size=1200,800'],
  });
  try {
    await ctx.grantPermissions(['clipboard-read'], { origin }); // 読むのは確かめのため。書くのは拡張がページの許可なしで行う
    const sw = ctx.serviceWorkers()[0] ?? await ctx.waitForEvent('serviceworker', { timeout: 10000 });
    const cmds = await sw.evaluate(() => chrome.commands.getAll());
    assert(cmds.some((c) => c.name === '_execute_action' && c.shortcut === 'Alt+Shift+F'), '拡張が読み込まれ、Alt+Shift+F が割り当たっている');
    const page = ctx.pages()[0] ?? await ctx.newPage();
    const errors = [];
    page.on('pageerror', (e) => errors.push(e.message));
    page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
    const key = async () => { execFileSync('xdotool', ['key', '--clearmodifiers', 'alt+shift+f']); await page.waitForTimeout(400); };
    const active = () => page.evaluate(() => document.documentElement.classList.contains('feedback-mode'));
    const clickOn = async (sel) => { const b = await page.locator(sel).boundingBox(); await page.mouse.move(b.x + 5, b.y + 5); await page.mouse.click(b.x + 5, b.y + 5); };

    for (const url of [`${origin}/`, `${origin}/csp`]) {
      console.log(`# ${url}`);
      await page.goto(url);
      await page.bringToFront();
      await key();
      await page.waitForFunction(() => document.documentElement.classList.contains('feedback-mode'), null, { timeout: 8000 });
      assert(true, 'Alt+Shift+F で入る');
      await clickOn('#buy');
      assert((await page.title()) === 'Shop sample', '部品を選んでも押されない');
      await page.locator('.fb-text').fill('買うボタンが小さい');
      await page.keyboard.press('Control+Enter');
      await page.waitForFunction(() => !document.documentElement.classList.contains('feedback-mode'), null, { timeout: 5000 });
      const clip = await page.evaluate(() => navigator.clipboard.readText());
      assert(clip.startsWith('---\nschema: 1') && clip.includes('> 買うボタンが小さい') && clip.includes('`ホーム/買う`') && clip.includes('- セレクタ: `') && clip.includes(url), 'クリップボードに記録（schema 1・文・部品の道筋・セレクタ・URL）');
      if (pointer) assert(clip.includes(pointer), `content.js の CONFIG の案内（${pointer}）が記録に入る`);
      assert((await page.locator('.fb-toast').innerText()).includes('コピーしました'), 'コピーしたと出て、モードを出る');
      await key();
      assert(await active(), 'もう一度入れる');
      await clickOn('#link');
      await page.waitForTimeout(300);
      assert(page.url() === url, 'リンクを選んでも移動しない');
      await page.keyboard.press('Escape');
      await page.keyboard.press('Escape');
      assert(!(await active()), 'Esc で出る');
      await key();
      assert(await active(), 'キーで入る');
      await key();
      assert(!(await active()), 'もう一度のキーで出る');
    }
    assert(errors.length === 0, `ページのエラーが無い ${errors.join(' / ')}`);
    console.log('PASS');
  } finally {
    await ctx.close();
    server.close();
  }
})().catch((e) => { console.error(e.message); process.exit(1); });
