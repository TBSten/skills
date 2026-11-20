// feedback-report.e2e.cjs — feedback-report.html をヘッドレスの Chromium で確かめる（Playwright が要る。無ければ skip）。
//   node feedback-report.e2e.cjs [<report.html>]   既定: このディレクトリの example.data.json から一時ファイルを作って使う
// 確かめること（references/setup/web/single-html.md §7）:
//   PC とスマホ幅: 選ぶ → 書く → 保存 → 印 → まとめてコピーの中身 / コンソールのエラー 0・横スクロール 0
//   書きかけが再読み込みで消えない / 2 回目のコピーは新しいものだけ / 保存できない状態でもコピーできる / 回答が JSON で入る
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
let chromium;
try { ({ chromium } = require('playwright')); } catch { console.log('SKIP: playwright が無い'); process.exit(0); }

const assert = (c, m) => { if (!c) throw new Error(`NG: ${m}`); console.log(`ok - ${m}`); };
const file = process.argv[2] ?? (() => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'fb-single-'));
  const out = path.join(dir, 'report.html');
  execFileSync(process.execPath, [path.join(__dirname, '../../../scripts/single-html.mjs'), 'new', out, '--data', path.join(__dirname, 'example.data.json')]);
  return out;
})();
const url = `file://${file}`;

async function run(browser, { name, viewport, storage = true }) {
  console.log(`# ${name}`);
  const ctx = await browser.newContext({ viewport, hasTouch: viewport.width < 600, permissions: ['clipboard-read', 'clipboard-write'] });
  if (!storage) await ctx.addInitScript(() => { Object.defineProperty(window, 'localStorage', { get() { throw new Error('blocked'); } }); });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await page.goto(url);
  let lastScroll = 0;
  const click = async (selector, { shift = false } = {}) => { const loc = page.locator(selector).first(); await loc.evaluate((el) => el.scrollIntoView({ block: 'center' })); lastScroll = await page.evaluate(() => scrollY); const b = await loc.boundingBox(); if (shift) await page.keyboard.down('Shift'); await page.mouse.click(b.x + Math.min(10, b.width / 2), b.y + Math.min(8, b.height / 2)); if (shift) await page.keyboard.up('Shift'); };
  const copied = async () => { await page.click('#fb-menu [data-act=copy]'); await page.waitForTimeout(200); return page.evaluate(() => navigator.clipboard.readText()).catch(() => page.locator('#fb-preview textarea').inputValue()); };

  // 回答
  await page.check('input[name=q-q1][value=vm]');
  // 入る → 選ぶ → 書く → 保存
  await page.click('#fb-toggle');
  assert(await page.locator('#fb-banner').isVisible(), '入ると帯が出る');
  await click('[data-fb="sections[id=overview].blocks[0].rows[id=frameworks].value"]');
  assert(await page.locator('#fb-editor').isVisible(), 'クリックで入力欄が開く');
  assert(Math.abs((await page.evaluate(() => scrollY)) - lastScroll) < 5, '入力欄を開いてもページが飛ばない');
  await page.fill('#fb-editor textarea', 'バージョンも書いて');
  // Shift + クリックで宛先を足す（PC だけ）
  if (viewport.width >= 600) {
    await click('[data-fb="sections[id=overview].blocks[0].rows[id=targets].item"]', { shift: true });
    assert((await page.locator('#fb-editor .targets > div').count()) === 2, 'Shift + クリックで宛先が 2 つになる');
    assert((await page.inputValue('#fb-editor textarea')) === 'バージョンも書いて', '宛先を足しても書いた文は残る');
  }
  await page.click('#fb-editor [data-act=save]');
  assert(await page.locator('.fb-badge').first().isVisible(), '保存すると印が出る');
  // 保存の直後に出るお知らせが、終えるボタン（#fb-toggle）に重ならない（実機の Android Chrome で押せなかった）。
  // 保存できないときは「保存できない」を知らせる一覧を開いているので見ない
  // 文の長さ（フォント）で横幅が変わるので、縦に重ならないこと（お知らせの下端 ≦ ボタンの上端）で見る
  if (storage) assert(await page.evaluate(() => { const t = document.querySelector('#fb-toggle'); const r = t.getBoundingClientRect(); const toast = document.querySelector('#fb-toast'); const ok = t.contains(document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2)); return ok && (toast.hidden || toast.getBoundingClientRect().bottom <= r.top); }), '保存の直後も終えるボタンを押せる（お知らせが重ならない）');
  // 書きかけ: 別の項目に書いて、再読み込みしても残る
  await click('[data-fb="sections[id=roles].blocks[0]"]');
  await page.fill('#fb-editor textarea', '書きかけ');
  if (storage) { // 保存できない環境では再読み込みで消える（だからすぐコピーを促す）
    await page.reload();
    await page.click('#fb-toggle');
    await click('[data-fb="sections[id=roles].blocks[0]"]');
    assert((await page.inputValue('#fb-editor textarea')) === '書きかけ', '書きかけが再読み込みで消えない');
  }
  await page.fill('#fb-editor textarea', 'Repository の説明が足りない');
  await page.click('#fb-editor [data-act=save]');
  if (viewport.width >= 600) {
    // 文字をドラッグで引用
    const loc = page.locator('[data-fb="sections[id=roles].blocks[2]"]'); await loc.evaluate((el) => el.scrollIntoView({ block: 'center' }));
    const r = await loc.boundingBox();
    await page.mouse.move(r.x + 16, r.y + r.height / 2); await page.mouse.down(); await page.mouse.move(r.x + 200, r.y + r.height / 2, { steps: 8 }); await page.mouse.up();
    assert((await page.locator('#fb-editor .targets').innerText()).includes('引用: 「'), 'ドラッグで選んだ文字が引用になる');
    await page.fill('#fb-editor textarea', '引用への指摘');
    await page.click('#fb-editor [data-act=save]');
    // ページ全体
    await click('[data-fb="page"]');
    await page.fill('#fb-editor textarea', '全体に図が欲しい');
    await page.click('#fb-editor [data-act=save]');
  }
  // 閉じた details の見出しを開け閉めできる
  await click('summary');
  assert(await page.locator('details').evaluate((d) => d.open), 'モード中も details を開ける');
  await page.keyboard.press('Escape');
  assert(!(await page.locator('#fb-banner').isVisible()), 'Esc で出る');

  // まとめてコピー
  const first = await copied();
  assert(first.includes('```json') && first.includes('"q1"') && first.includes('"vm"'), 'コピーに回答の JSON が入る');
  assert(first.includes('コードベースのレポート（report.html）へのフィードバックです'), 'コピーの 1 行目にファイルと件数');
  assert(first.includes('対象: sections[id=overview].blocks[0].rows[id=frameworks].value'), 'コピーに JSON の場所');
  assert(first.includes('内容: 「Compose, Koin」') && first.includes('> バージョンも書いて'), 'コピーに内容と文');
  if (viewport.width >= 600) assert(/引用: 「.+」\n   > 引用への指摘/.test(first) && first.includes('対象: page'), 'コピーに引用とページ全体');
  if (storage) {
    await page.click('#fb-toggle');
    await click('[data-fb="sections[id=roles].blocks[2]"]');
    await page.fill('#fb-editor textarea', '新しい指摘');
    await page.click('#fb-editor [data-act=save]');
    await page.keyboard.press('Escape');
    const second = await copied();
    assert(second.includes('（1 件）') && second.includes('新しい指摘') && !second.includes('バージョンも書いて'), '2 回目のコピーは新しいものだけ');
    // すべて消す → 戻す
    await page.click('#fb-menu [data-act=clear]');
    assert((await page.locator('.fb-badge').count()) === 0, 'すべて消すと印が消える');
    await page.click('#fb-toast button');
    assert((await page.locator('.fb-badge').count()) > 0, '「戻す」で戻る');
  } else {
    assert(await page.locator('#fb-list [data-role=nostore]').isVisible() || (await page.locator('#fb-toast').innerText()).includes('保存できない'), '保存できないことが見える');
  }
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), '横スクロールが無い');
  assert(errors.length === 0, `コンソールのエラーが無い ${errors.join(' / ')}`);
  await ctx.close();
}

(async () => {
  const browser = await chromium.launch();
  try {
    await run(browser, { name: 'PC', viewport: { width: 1280, height: 800 } });
    await run(browser, { name: 'スマホ幅', viewport: { width: 390, height: 844 } });
    await run(browser, { name: '保存できない', viewport: { width: 1280, height: 800 }, storage: false });
    console.log('PASS');
  } finally { await browser.close(); }
})().catch((e) => { console.error(e.message); process.exit(1); });
