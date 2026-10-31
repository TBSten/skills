// Dev Toolbar のアプリ（ひな形）: モードに入る → 要素を選ぶ（マウスはクリック、タッチはタップ）→ そばの popover（幅 600px 以下はシート）に書く → 送る。
// 【実績: Astro 7 + Starlight の docs サイトを縮めたもの】。テキスト選択・右クリックの入口・複数選択・スクショ・ドラッグ・下書きは references/build/interaction.md。
import { defineToolbarApp } from 'astro/toolbar'
import { EVENT_RESULT, EVENT_SAVE, LOG_GLOBAL, type SaveRequest, type SaveResult } from './protocol'

const TAP_SLOP_PX = 10
const TAP_MAX_MS = 600
const TIMEOUT_MS = 10_000
const isNarrow = () => matchMedia('(max-width: 600px)').matches
/** Astro の Dev Toolbar の要素名。 */
const TOOLBAR_TAG = 'astro-dev-toolbar'

/** 次の Esc の keyup を toolbar に渡さない（Esc で閉じたあとに届く keyup が toolbar を隠さないように）。 */
function swallowEscKeyup() {
	const swallow = (e: KeyboardEvent) => {
		if (e.key !== 'Escape') return
		e.stopPropagation()
		window.removeEventListener('keyup', swallow, true)
	}
	window.addEventListener('keyup', swallow, true)
	setTimeout(() => window.removeEventListener('keyup', swallow, true), 1000)
}

/**
 * host の color-scheme をページのテーマに合わせる。host は `all: initial` で color-scheme が既定に戻るので、
 * ページの computed を写す。色は STYLE の `light-dark()` で持つ（interaction.md §10）。テーマの切り替えにも追従する。
 * host は `<html>` の直下に置くので、`<html>` の子の増減も見る（host を外したときに監視をやめるため。属性だけを見ると、
 * テーマが変わるまで observer と外した host が残り、開け閉めのたびに溜まる）。
 */
function syncColorScheme(host: HTMLElement) {
	const apply = () => {
		const page = getComputedStyle(document.documentElement).colorScheme
		host.style.colorScheme = page && page !== 'normal' ? page : 'light dark'
	}
	apply()
	const observer = new MutationObserver(() => (host.isConnected ? apply() : observer.disconnect()))
	observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'class', 'style'], childList: true })
}

/** ページの編集欄（input・textarea・select・contenteditable）。 */
const isEditor = (n: unknown): n is HTMLElement =>
	n instanceof HTMLElement && (n.matches('input, textarea, select') || n.isContentEditable)

/** フォーカスのある要素を shadow root の中までたどる（Astro の toolbar の中は除く）。 */
function deepActiveElement(): Element | null {
	let a = document.activeElement
	while (a?.shadowRoot?.activeElement && a.localName !== TOOLBAR_TAG) a = a.shadowRoot.activeElement
	return a
}

const STYLE = `
:host { all: initial; }
.frame { position: fixed; pointer-events: none; outline: 2px solid #2563eb; background: rgba(37,99,235,.08); z-index: 2147483646; }
.frame.picked { outline-color: #dc2626; background: rgba(220,38,38,.08); }
.banner { position: fixed; left: 12px; bottom: 56px; display: flex; gap: 8px; align-items: center; padding: 8px 12px; background: #1c1c1c; color: #fff; border-radius: 8px; font: 14px system-ui, sans-serif; z-index: 2147483647; }
.banner button, .panel button { min-height: 32px; padding: 0 12px; font: inherit; border-radius: 6px; border: 1px solid #71717a; background: #3f3f46; color: #fff; cursor: pointer; }
.panel { position: fixed; width: 340px; max-width: calc(100vw - 16px); padding: 10px; display: flex; flex-direction: column; gap: 8px; background: light-dark(#fff, #1f1f23); color: light-dark(#111, #f4f4f5); border: 1px solid light-dark(#d4d4d4, #3f3f46); border-radius: 10px; box-shadow: 0 8px 28px rgba(0,0,0,.3); font: 14px system-ui, sans-serif; z-index: 2147483647; }
.panel textarea { min-height: 96px; font: inherit; }
.panel .actions { display: flex; gap: 8px; justify-content: flex-end; }
.panel .send { background: #2563eb; border-color: #2563eb; }
.panel .error { color: light-dark(#b91c1c, #fca5a5); }
/* 幅 600px 以下とタッチ端末: 入力は 16px（iOS が拡大しない）、押せる高さは 44px。 */
@media (max-width: 600px), (pointer: coarse) {
  .panel textarea { font-size: 16px; }
  .banner button, .panel button { min-height: 44px; min-width: 64px; }
}
/* 幅 600px 以下: 下から出るシート。top と height は visualViewport に合わせる（ソフトキーボードで縮む）。 */
@media (max-width: 600px) {
  .sheet { position: fixed; left: 0; right: 0; display: flex; align-items: flex-end; z-index: 2147483647; pointer-events: none; }
  .panel { position: static; width: 100%; max-width: none; border-radius: 16px 16px 0 0; pointer-events: auto; }
}
`

type Picked = { kind: 'element'; el: Element } | { kind: 'page' }

// ---- 場所: 保存の前に引き直し、同じ要素に戻ったものだけを書く（references/build/locate.md） ----

function xpathOf(el: Element): string | null {
	const steps: string[] = []
	for (let cur: Element | null = el; cur; cur = cur.parentElement) {
		// id はセレクタに入れる前に CSS.escape する（`a\` のような id でセレクタが壊れて投げないように）。
		if (cur.id && !cur.id.includes('"') && document.querySelectorAll(`#${CSS.escape(cur.id)}`).length === 1) {
			steps.unshift(`//*[@id="${cur.id}"]`)
			break
		}
		let n = 1
		for (let s = cur.previousElementSibling; s; s = s.previousElementSibling) if (s.localName === cur.localName) n++
		steps.unshift(`/${cur.localName}[${n}]`)
	}
	const xpath = steps.join('')
	try {
		const r = document.evaluate(xpath, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)
		return r.snapshotLength === 1 && r.snapshotItem(0) === el ? xpath : null
	} catch {
		return null
	}
}

function selectorOf(el: Element): string | null {
	const parts: string[] = []
	for (let cur: Element | null = el; cur && cur !== document.documentElement; cur = cur.parentElement) {
		if (cur.id && document.querySelectorAll(`#${CSS.escape(cur.id)}`).length === 1) {
			parts.unshift(`#${CSS.escape(cur.id)}`)
			break
		}
		const same = cur.parentElement ? [...cur.parentElement.children].filter((c) => c.localName === cur!.localName) : []
		parts.unshift(same.length > 1 ? `${cur.localName}:nth-of-type(${same.indexOf(cur) + 1})` : cur.localName)
	}
	const sel = parts.join(' > ')
	try {
		const all = document.querySelectorAll(sel)
		return all.length === 1 && all[0] === el ? sel : null
	} catch {
		return null
	}
}

/** 要素と祖先の data-fb を外側からつなぐ（無ければ空）。 */
const namePath = (el: Element) => {
	const names: string[] = []
	for (let cur: Element | null = el; cur; cur = cur.parentElement) if (cur.getAttribute('data-fb')) names.unshift(cur.getAttribute('data-fb')!)
	return names.join('/')
}
const describe = (el: Element) => `${el.localName}${el.id ? `#${el.id}` : ''}「${(el.textContent ?? '').replace(/\s+/g, ' ').trim().slice(0, 40)}」`

export default defineToolbarApp({
	init(_canvas, app, server) {
		// 結果は requestId で依頼と対応づける。toolbar の購読は外せないので、init で 1 回だけ登録する。
		const waiting = new Map<string, (r: SaveResult) => void>()
		server.on<SaveResult>(EVENT_RESULT, (r) => waiting.get(r?.requestId)?.(r))
		let stop: (() => void) | null = null

		app.onToggled(({ state }) => {
			stop?.()
			stop = state ? enter() : null
		})

		function enter(): () => void {
			const abort = new AbortController()
			const opts = { capture: true, signal: abort.signal }
			const host = document.createElement('feedback-mode-ui')
			const root = host.attachShadow({ mode: 'open' })
			const style = document.createElement('style')
			style.textContent = STYLE
			const hover = Object.assign(document.createElement('div'), { className: 'frame' })
			const picked = Object.assign(document.createElement('div'), { className: 'frame picked' })
			hover.hidden = picked.hidden = true
			const banner = Object.assign(document.createElement('div'), { className: 'banner', textContent: '要素を選んでコメント。Esc で終わる' })
			const pageBtn = Object.assign(document.createElement('button'), { textContent: 'このページについて' })
			banner.append(pageBtn)
			root.append(style, hover, picked, banner)
			document.documentElement.append(host)
			syncColorScheme(host)
			let panel: { el: HTMLElement; pick: Picked; textarea: HTMLTextAreaElement } | null = null
			let down: { x: number; y: number; t: number } | null = null
			let lastPointer = 'mouse'
			let undoViewport = () => {}

			// Astro の toolbar も別の host。自前の UI と同じく選ぶ対象にせず、操作（アプリの切り替えなど）を止めない。
			const inToolbar = (e: Event) => e.composedPath().some((n) => n instanceof Element && n.localName === TOOLBAR_TAG)
			const isOwn = (e: Event) => e.composedPath().includes(host) || inToolbar(e)
			const place = (frame: HTMLElement, el: Element | null) => {
				frame.hidden = !el
				if (!el) return
				const r = el.getBoundingClientRect()
				Object.assign(frame.style, { left: `${r.left}px`, top: `${r.top}px`, width: `${r.width}px`, height: `${r.height}px` })
			}
			const exit = () => app.toggleState({ state: false })

			function closePanel() {
				undoViewport()
				panel?.el.closest('.sheet')?.remove()
				panel?.el.remove()
				panel = null
				place(picked, null)
			}

			function open(pick: Picked) {
				// 書きかけの文は、帯の「このページについて」で選び直しても捨てない（選び直しに持っていく）。
				// ページの上を押しての選び直しはしない（pickAt）。
				const kept = panel?.textarea.value ?? ''
				closePanel()
				const el = document.createElement('div')
				el.className = 'panel'
				const textarea = document.createElement('textarea')
				textarea.value = kept
				textarea.placeholder = pick.kind === 'page' ? 'このページについて' : 'この要素について'
				const error = Object.assign(document.createElement('div'), { className: 'error' })
				const send = Object.assign(document.createElement('button'), { className: 'send', textContent: '送る' })
				const cancel = Object.assign(document.createElement('button'), { textContent: 'やめる' })
				const actions = Object.assign(document.createElement('div'), { className: 'actions' })
				actions.append(cancel, send)
				el.append(textarea, error, actions)
				panel = { el, pick, textarea }
				if (isNarrow()) {
					// シート: 見える範囲（ソフトキーボードで縮む）の下端に置く。
					const sheet = Object.assign(document.createElement('div'), { className: 'sheet' })
					sheet.append(el)
					root.append(sheet)
					const vv = window.visualViewport
					const fit = () => { if (vv) Object.assign(sheet.style, { top: `${vv.offsetTop}px`, height: `${vv.height}px` }) }
					fit()
					vv?.addEventListener('resize', fit)
					vv?.addEventListener('scroll', fit)
					undoViewport = () => { vv?.removeEventListener('resize', fit); vv?.removeEventListener('scroll', fit) }
				} else {
					// popover: 選んだもののそば。幕を敷かないので、ページは見えたまま。
					root.append(el)
					const r = pick.kind === 'element' ? pick.el.getBoundingClientRect() : new DOMRect(innerWidth / 2 - 170, 80, 0, 0)
					el.style.left = `${Math.max(8, Math.min(r.left, innerWidth - el.offsetWidth - 8))}px`
					el.style.top = `${r.bottom + 8 + el.offsetHeight < innerHeight ? r.bottom + 8 : Math.max(8, r.top - el.offsetHeight - 8)}px`
				}
				if (pick.kind === 'element') place(picked, pick.el)
				cancel.onclick = closePanel
				send.onclick = async () => {
					send.disabled = true
					error.textContent = ''
					let r: SaveResult
					try {
						r = await save(pick, textarea.value)
					} catch (e) {
						// 場所の情報を集める途中で投げても、ボタンを戻して理由を出す（送れないまま固まらない）。
						r = { requestId: '', ok: false, error: e instanceof Error ? e.message : String(e) }
					} finally {
						send.disabled = false
					}
					if (r.ok) exit()
					else error.textContent = `保存できませんでした: ${r.error}`
				}
				textarea.focus()
			}

			function save(pick: Picked, comment: string): Promise<SaveResult> {
				const buffer = (window as unknown as Record<string, { entries(): unknown[] } | undefined>)[LOG_GLOBAL]
				const logs = buffer?.entries() ?? []
				const target = pick.kind === 'element'
					? (() => {
						const r = pick.el.getBoundingClientRect()
						return { screen: location.pathname, path: namePath(pick.el), element: describe(pick.el), selector: selectorOf(pick.el) ?? undefined, xpath: xpathOf(pick.el) ?? undefined, rect: { x: r.x, y: r.y, width: r.width, height: r.height } }
					})()
					: undefined
				const requestId = `${Date.now()}-${Math.random().toString(36).slice(2)}`
				const req: SaveRequest = {
					requestId,
					payload: { comment, kind: pick.kind, target, logs, screen: { url: location.href, viewport: `${innerWidth}x${innerHeight}`, dpr: devicePixelRatio }, pageOpenedAt: Math.round(performance.timeOrigin) },
					options: { screen: true, shots: false },
				}
				return new Promise((resolve) => {
					const timer = setTimeout(() => { waiting.delete(requestId); resolve({ requestId, ok: false, error: 'サーバーから応答がありません' }) }, TIMEOUT_MS)
					waiting.set(requestId, (r) => { clearTimeout(timer); waiting.delete(requestId); resolve(r) })
					server.send(EVENT_SAVE, req)
				})
			}

			const targetAt = (x: number, y: number) => document.elementsFromPoint(x, y).find((el) => el !== host && !host.contains(el) && el.localName !== TOOLBAR_TAG) ?? null
			/**
			 * ページの上を押した（クリック・タップ）。入力欄を開いているときは選び直さない（書いた文の対象が変わるため。interaction.md §1）:
			 * コメントが空なら閉じ、書いてあれば何もしない。閉じるのは「やめる」と Esc。
			 */
			const pickAt = (x: number, y: number) => {
				if (panel) {
					if (panel.textarea.value.trim() === '') closePanel()
					return
				}
				const t = targetAt(x, y)
				if (t) open({ kind: 'element', el: t })
			}
			pageBtn.onclick = () => open({ kind: 'page' })
			document.addEventListener('pointermove', (e) => { if (e.pointerType === 'mouse' && !isOwn(e)) place(hover, targetAt(e.clientX, e.clientY)) }, opts)
			// リンクやボタンを動かさない（タップも最後に click になるので、これでスマホでも動かない）。
			document.addEventListener('click', (e) => {
				if (isOwn(e)) return
				e.preventDefault()
				e.stopPropagation()
				if (lastPointer === 'touch') return // タッチは pointerup で選び済み
				pickAt(e.clientX, e.clientY)
			}, opts)
			// iOS Safari は、click ハンドラを持たない要素（段落など）のタップでは click を出さない。
			// document の capture リスナーは数えられないので、タッチのタップは pointerup で自分で判定する。
			document.addEventListener('pointerdown', (e) => {
				lastPointer = e.pointerType
				if (e.pointerType === 'touch' && !isOwn(e)) down = { x: e.clientX, y: e.clientY, t: e.timeStamp }
			}, opts)
			document.addEventListener('pointerup', (e) => {
				const d = down
				down = null
				if (e.pointerType !== 'touch' || !d || isOwn(e)) return
				if (Math.hypot(e.clientX - d.x, e.clientY - d.y) >= TAP_SLOP_PX || e.timeStamp - d.t >= TAP_MAX_MS) return // スクロール・長押し
				pickAt(e.clientX, e.clientY)
			}, opts)
			// キーはページに渡さない（サイトのショートカットやキーボードで動く部品を止める。architecture.md §1.2、interaction.md §9）。
			// window の capture で stopImmediatePropagation だけをする。preventDefault はしないので、コピー・ページ内検索・スクロール・Tab は残る。
			// Enter / Space でリンクやボタンが動くのは、それが click になり、上の click の横取りで止まるため。
			// 自前の UI（textarea・ボタン）のキーもここで止める。host の bubble で止めるのでは、document の capture にある
			// サイトのショートカット（検索を開くなど）が先に動く。文字の入力・改行・Tab・ボタンの Enter / Space は既定の動作なので残る。
			// 自前の UI にキーの処理（Ctrl+Enter で送る など）を足すときは、host の中のリスナーには届かないので、ここで扱う。
			for (const type of ['keydown', 'keyup', 'keypress'] as const) {
				window.addEventListener(type, (e) => {
					if (inToolbar(e)) return // toolbar 自身のキー操作
					// IME の変換中の Esc は変換の取り消しなので、モードを終えない（書きかけの文を失わない）。
					if (e.key === 'Escape' && !e.isComposing) {
						e.preventDefault()
						e.stopPropagation()
						if (type === 'keydown') {
							swallowEscKeyup() // 届くと toolbar が隠れる
							exit()
						}
						return
					}
					e.stopImmediatePropagation()
				}, opts)
			}
			// 背景の編集欄にフォーカスを入れない（interaction.md §9）。キーの伝播を止めても既定の編集（文字の入力・貼り付け・
			// select の矢印）は止まらず、Tab やクリックの mousedown（click の横取りより前）でフォーカスが入ると中身を変えられる。
			// 入ったら外す。モードに入る前から入っていた分も外す。外すだけだと Firefox は次の Tab がまた同じ欄に入って先へ進めないので、
			// 外した欄は Tab の順から抜き（tabindex=-1）、モードを終えるときに元の tabindex に戻す。
			const untabbed = new Map<HTMLElement, string | null>()
			const keepOut = (el: HTMLElement) => {
				if (!untabbed.has(el)) {
					untabbed.set(el, el.getAttribute('tabindex'))
					el.tabIndex = -1
				}
				el.blur()
			}
			window.addEventListener('focusin', (e) => {
				const t = e.composedPath()[0]
				if (!isOwn(e) && isEditor(t)) keepOut(t)
			}, opts)
			const active = deepActiveElement()
			if (isEditor(active)) keepOut(active)
			// 枠と popover をスクロールに追従させる代わりに、ここでは枠だけ付け直す（popover の追従は interaction.md）。
			window.addEventListener('scroll', () => { if (panel?.pick.kind === 'element') place(picked, panel.pick.el) }, opts)
			// ページ遷移（View Transitions を含む）に UI を持ち越さない。
			document.addEventListener('astro:before-swap', exit, opts)

			return () => {
				abort.abort()
				for (const [el, tabindex] of untabbed) tabindex === null ? el.removeAttribute('tabindex') : el.setAttribute('tabindex', tabindex)
				closePanel()
				host.remove()
			}
		}
	},
})
