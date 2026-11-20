// feedback-report.android-chrome.cjs — Android（エミュレータ・実機）の Chrome で feedback-report.html を確かめる。
//   タップは CDP の Input.dispatchTouchEvent（本物のタッチ）。キーボードの確かめは references/setup/web/single-html.md §7 の手順で
// 準備（references/setup/web/single-html.md §7）:
//   python3 -m http.server 8090 --bind 127.0.0.1（report.html のあるディレクトリで）
//   adb reverse tcp:8090 tcp:8090 && adb forward tcp:9333 localabstract:chrome_devtools_remote
//   Chrome で http://localhost:8090/report.html を開いておく
// 使い方: node feedback-report.android-chrome.cjs http://127.0.0.1:9333     （playwright-core か playwright が要る）
let chromium;
try { ({ chromium } = require('playwright-core')); } catch { try { ({ chromium } = require('playwright')); } catch { console.log('SKIP: playwright が無い'); process.exit(0); } }
const assert = (c, m) => { if (!c) throw new Error(`NG: ${m}`); console.log(`ok - ${m}`); };
(async () => {
  const browser = await chromium.connectOverCDP(process.argv[2]);
  const ctx = browser.contexts()[0];
  const page = ctx.pages().find((p) => p.url().includes('report.html')) ?? ctx.pages()[0];
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await page.evaluate(() => { try { localStorage.clear(); } catch {} });
  await page.reload(); await page.waitForTimeout(800);
  console.log('viewport', await page.evaluate(() => [innerWidth, innerHeight, devicePixelRatio, navigator.userAgent]));
  const cdp = await ctx.newCDPSession(page);
  const touch = async (x, y) => {
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y }] });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  };
  const tap = async (sel) => { const l = page.locator(sel).first(); await l.scrollIntoViewIfNeeded(); const b = await l.boundingBox(); await touch(b.x + Math.min(12, b.width / 2), b.y + Math.min(10, b.height / 2)); await page.waitForTimeout(400); };
  await tap('input[name=q-q1][value=vm]');
  await tap('#fb-toggle');
  assert(await page.locator('#fb-banner').isVisible(), '入ると帯が出る');
  const y0 = await page.evaluate(() => scrollY);
  await tap('[data-fb="sections[id=overview].blocks[0].rows[id=frameworks].value"]');
  assert(await page.locator('#fb-editor').isVisible(), 'タップで入力欄が開く');
  await tap('#fb-editor textarea');
  await page.keyboard.type('version please');
  await page.waitForTimeout(500);
  const fs = await page.evaluate(() => getComputedStyle(document.querySelector('#fb-editor textarea')).fontSize);
  assert(parseFloat(fs) >= 16, `入力欄の文字が 16px 以上（${fs}。iOS の自動ズームを避ける）`);
  await tap('#fb-editor [data-act=save]');
  await page.waitForTimeout(1000); // キーボードが閉じて画面の高さが戻るのを待つ
  assert(await page.locator('.fb-badge').first().isVisible(), '保存すると印が出る');
  // 閉じるボタンで出る（モバイルには Esc が無い）
  await tap('#fb-toggle');
  assert(!(await page.locator('#fb-banner').isVisible()), 'もう一度押すとモードを出る');
  // クリップボードを読むと権限の確認が出るので、同じ文を出す「コピーされる内容を見る」で確かめる
  await tap('#fb-menu [data-act=preview]');
  const txt = await page.locator('#fb-preview textarea').inputValue();
  console.log('preview:', txt.slice(0, 160).replace(/\n/g, '⏎'));
  assert(txt.includes('> version please') && txt.includes('"vm"'), 'コピーされる内容に文と回答が入る');
  await tap('#fb-preview [data-act=close]');
  // まとめてコピー
  await tap('#fb-menu [data-act=copy]');
  await page.waitForTimeout(500);
  const toast = await page.locator('#fb-toast').innerText().catch(() => '');
  console.log('toast:', toast);
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), '横スクロールが無い');
  assert(errors.length === 0, `コンソールのエラーが無い ${errors.join(' / ')}`);
  await browser.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
