// Astro の Dev Toolbar に載せるフィードバックモードの統合（ひな形）。【実績: Astro 7 + Starlight の docs サイトを縮めたもの】
//
// 使い方（astro.config.mjs）:
//   import feedbackMode from './<dev 専用の場所>/feedback-integration'
//   export default defineConfig({ integrations: [feedbackMode({ pointer: '案内: <サイト名> のフィードバック。ソースは <docs-dir>/ …' })] })
// 前提: 同じフォルダの core/ に skill の core（scripts/copy-core.sh でコピーした feedback-core.mjs・file-sink.mjs）。
//
// 守っていること:
//   - `command === 'dev'` のときだけ登録する。本番ビルド（astro build）には toolbar アプリも log バッファも出ない。
//     確かめるのは check-no-dev-tools-in-dist.sh（CI の build の後に 1 ステップ）。
//   - 保存は toolbar のメッセージ（Vite の HMR の WebSocket）で受ける。HTTP の受け口・ポートを新しく開けない。
//   - log バッファは head-inline で、ページのどの script より前に入れる（toolbar アプリの init より前のエラーも拾う）。
import { resolve } from 'node:path'
import type { AstroIntegration } from 'astro'
import { submitFeedback } from './core/feedback-core.mjs'
import { createFileSink } from './core/file-sink.mjs'
import { APP_ID, EVENT_RESULT, EVENT_SAVE, LIMITS, LOG_GLOBAL, type SaveRequest, type SaveResult } from './protocol'

/** head-inline に差す script。バンドルを通らないので ES5 の文字列で書く。console・未捕捉の例外・rejection・読み込み失敗・Vite のエラーを溜める。 */
const LOG_BUFFER_SCRIPT = `(function () {
  var G = ${JSON.stringify(LOG_GLOBAL)}, MAX = ${LIMITS.logEntries}, LEN = ${LIMITS.logChars};
  if (window[G]) return;
  var buf = [];
  window[G] = { entries: function () { return buf.slice(); } };
  function text(v) {
    if (typeof v === 'string') return v;
    if (v && typeof v.message === 'string') return v.stack || v.name + ': ' + v.message;
    try { return JSON.stringify(v); } catch (e) { return String(v); }
  }
  function push(level, m) {
    m = String(m); if (m.length > LEN) m = m.slice(0, LEN) + '…';
    buf.push({ ts: new Date().toISOString().slice(11, 23), level: level, text: m });
    if (buf.length > MAX) buf.shift();
  }
  ['error', 'warn', 'log'].forEach(function (level) {
    var orig = console[level];
    console[level] = function () {
      try { push(level === 'log' ? 'info' : level, Array.prototype.map.call(arguments, text).join(' ')); } catch (e) {}
      return orig.apply(console, arguments);
    };
  });
  // 画像・script の読み込み失敗は window まで bubble しないので capture で受ける。
  window.addEventListener('error', function (e) {
    var t = e.target;
    if (t && t !== window && t.tagName) push('error', '<' + t.tagName.toLowerCase() + '> の読み込みに失敗: ' + (t.currentSrc || t.src || t.href || ''));
    else push('error', (e.message || text(e.error)) + (e.filename ? ' (' + e.filename + ':' + e.lineno + ')' : ''));
  }, true);
  window.addEventListener('unhandledrejection', function (e) { push('error', 'unhandledrejection ' + text(e.reason)); });
  // Vite の HMR のエラー（CSS の構文エラーなど）。MDX の構文エラーはエラーページへの全リロードになり、ここには残らない。
  try {
    import('/@vite/client').then(function (m) {
      m.createHotContext('/@feedback-mode-log').on('vite:error', function (p) { push('error', 'vite ' + text(p && p.err ? (p.err.message || p.err) : p)); });
    }).catch(function () {});
  } catch (e) {}
})();`

export type FeedbackModeOptions = {
	/** 記録の見出しに出る名前。 */
	appName?: string
	/** 読む Agent への案内の 1 行（ソースの場所・正本 docs・文の決まり）。記録の冒頭に入る。 */
	pointer?: string
	/** リポジトリのルート。既定は astro を起動したディレクトリ。 */
	repoRoot?: string
	/** 置き場（repoRoot からの相対）。環境変数 FEEDBACK_DIR が優先（検証で本物の置き場を汚さないため）。 */
	dir?: string
}

/** 受け取った依頼の形と大きさ。理由つきで返す（例外は投げない）。 */
function check(v: unknown): { ok: true; req: SaveRequest } | { ok: false; requestId?: string; reason: string } {
	const o = (typeof v === 'object' && v !== null ? v : {}) as Partial<SaveRequest>
	const requestId = typeof o.requestId === 'string' && o.requestId.length > 0 && o.requestId.length <= 100 ? o.requestId : undefined
	if (!requestId) return { ok: false, reason: 'requestId が不正です' }
	if (typeof o.payload !== 'object' || o.payload === null || typeof o.payload.comment !== 'string') return { ok: false, requestId, reason: 'payload が不正です' }
	if (o.payload.comment.length > LIMITS.commentChars) return { ok: false, requestId, reason: `コメントが長すぎます（上限 ${LIMITS.commentChars} 字）` }
	if (JSON.stringify(o).length > LIMITS.payloadChars) return { ok: false, requestId, reason: '依頼が大きすぎます' }
	return { ok: true, req: o as SaveRequest }
}

export default function feedbackMode(opts: FeedbackModeOptions = {}): AstroIntegration {
	return {
		name: APP_ID,
		hooks: {
			'astro:config:setup': ({ command, addDevToolbarApp, injectScript }) => {
				// dev だけ。build・preview・sync では何も足さない。
				if (command !== 'dev') return
				injectScript('head-inline', LOG_BUFFER_SCRIPT)
				addDevToolbarApp({ id: APP_ID, name: 'Feedback', icon: 'lightbulb', entrypoint: new URL('./feedback-app.ts', import.meta.url) })
			},
			// dev サーバーのときだけ呼ばれる。保存は toolbar のメッセージで受け、Sink に書く。
			'astro:server:setup': ({ server, toolbar, logger }) => {
				// --host で LAN に出すと、同じネットワークからページを開いた人も保存できる。止めはしないが、起動時に知らせる。
				if (server.config.server.host) logger.warn('dev サーバーが LAN に開いています。同じネットワークからフィードバックを書き込めます')
				const repoRoot = resolve(opts.repoRoot ?? process.cwd())
				const sink = createFileSink({ repoRoot, dir: process.env.FEEDBACK_DIR ?? opts.dir ?? 'feedback', attachDir: '.local/tmp/feedback' })
				toolbar.on(EVENT_SAVE, async (data: unknown) => {
					const c = check(data)
					if (!c.ok) {
						if (c.requestId) toolbar.send(EVENT_RESULT, { requestId: c.requestId, ok: false, error: c.reason } satisfies SaveResult)
						return
					}
					const { requestId, payload, options } = c.req
					let result: SaveResult
					try {
						// meta を渡すと schema 1 の frontmatter（kind・omitted…）が付く。dirty / stale はソースのファイルが分かる場合に足す（references/build/locate.md）。
						const r = await submitFeedback({ sink, payload: { ...payload, appName: opts.appName ?? 'site', pointer: opts.pointer }, options, meta: {} })
						logger.info(`フィードバックを保存しました: ${r.ref}`)
						result = { requestId, ok: true, ref: r.ref }
					} catch (e) {
						result = { requestId, ok: false, error: e instanceof Error ? e.message : String(e) }
					}
					toolbar.send(EVENT_RESULT, result)
				})
			},
		},
	}
}
