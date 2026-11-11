#!/usr/bin/env node
// review.mjs — Review（agent → 人の指摘）の置き場を読み書きする cli。node の標準だけで動く（Node 18 以上、依存なし）。
// 形・流れ・ボタンの意味は references/review/review.md。この script を読解・書き換え・再実装せず、そのまま実行する。
//
//   node review.mjs list [--status=open|resolved|all] [--action=<名前>] [--impact=あり|なし] [--json]
//   node review.mjs show <id>
//   node review.mjs check
//   node review.mjs resolve <id> [--note "<ひとこと>"]          人の代わりに解決済みにする（UI が無いとき・人がチャットで答えたとき）
//   node review.mjs remove <id> [--force]
//   node review.mjs reopen <id> --note "<理由>"
//   node review.mjs send <id> [--note "<ひとこと>"]             「直してもらう」: 指摘を Feedback の記録にする
//   node review.mjs archived <記録の id かパス>                 記録を archive したあとに回す。記録が指していた指摘を消す
//   node review.mjs actions [--json]
//
// 共通のオプション（環境変数でも渡せる）:
//   --inbox <dir>        フィードバック置き場（Inbox）。既定: $FEEDBACK_DIR、無ければ <root>/.local/feedback。指摘は <inbox>/review/
//   --root <dir>         指摘の file を読む基準（リポジトリ直下）。既定: カレント
//   --file-prefix <dir>  file がそのままで見つからないときに前に付けて探すフォルダ（例: docs/src/content/docs/）。既定: $FEEDBACK_FILE_PREFIX
//   --link <テンプレート> show が出すリンク。{file} {line} {id} を置き換える（例: http://localhost:4321/{file}#review={id}）。既定: $FEEDBACK_REVIEW_LINK
//
// import すると、下の純粋な関数（parseReview・applyStatus・removeItem・relocate・parseActions など）と createReviewStore を使える。
// 導入先の UI（popup）の受け口は、これを import して同じ規則で読み書きする（規則を二重に持たない）。
import { randomBytes } from 'node:crypto'
import { mkdir, open, readdir, readFile, rename, rm, stat, writeFile } from 'node:fs/promises'
import { basename, isAbsolute, join, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

// ---- 決まった名前 ----

/** 置き場の中で、指摘のファイルを置くフォルダ。 */
export const REVIEW_SUBDIR = 'review'
/** 人が足す選択肢のファイル（`_` で始まるので、指摘のまとまりとしては読まない）。 */
export const ACTIONS_FILE = '_actions.md'
/** 「直さない」で解決済みにしたときの note の頭。 */
export const KEEP_NOTE = '直さない'
/** 「直してもらう」で Feedback の記録にしたときの note の頭（`Feedback に送った: <記録の id>`）。 */
export const SENT_NOTE = 'Feedback に送った'
/** 「直してもらう」のボタンの名前。 */
export const SEND_LABEL = '直してもらう'
/** 「解決済みにする」のボタンの名前。 */
export const RESOLVE_LABEL = '解決済みにする'
/** 初めから並んでいるボタン（`- recommend:` に書ける名前）。 */
export const BUILTIN_CHOICES = [RESOLVE_LABEL, KEEP_NOTE, SEND_LABEL]
/** Feedback の記録の中で、元の指摘を指す行の名前（`- Review の指摘: <id>`）。 */
export const LINK_LABEL = 'Review の指摘'
/** 判断の記録（decisions.py）の種類。`_actions.md` の `- decision:` に書ける値。 */
export const DECISIONS = ['直さない', 'このまま', '後で', '方針']

export const REVIEW_STATUSES = ['open', 'resolved']
/** 指摘のメタとして読むキー。本文の箇条書き（`- 該当: …` など）を飲み込まないよう、ここにあるキーだけをメタにする。 */
const META_KEYS = ['file', 'line', 'priority', 'quote', 'impact', 'recommend', 'status', 'note', 'resolved-at', 'ai-note', 'id']
const META_LINE = new RegExp(`^- (${META_KEYS.join('|')}):(?:\\s+(.*))?$`)
/** `- id:` に書く形（まとまりの名前を除いた id。`k3x9a2b` / `k3x9a2b-2`）。 */
const PINNED_ID = /^[0-9a-z]{1,13}(?:-\d{1,4})?$/
/** まとまりの名前（ファイル名）に使える形。 */
export const SET_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$/
/** 1 行に書けるメモの文字数。 */
export const NOTE_CHARS = 500
/** 推奨度の上限（0〜10）。 */
export const RECOMMEND_MAX = 10
const FENCE = /^ {0,3}(`{3,}|~{3,})(.*)$/

/**
 * コードフェンスの中かを 1 行ずつ追う（CommonMark と同じ規則）。開いた記号と本数を覚え、同じ記号で同じ本数以上・後ろが空白だけの行で閉じる。
 * 4 本の ```` で囲んだ例の中の ``` では閉じない。返す関数は「この行がフェンスの中（開き・閉じの行を含む）か」。
 */
export function fenceTracker() {
	let open = null
	return (line) => {
		const m = FENCE.exec(line)
		if (open) {
			if (m && m[1][0] === open[0] && m[1].length >= open.length && m[2].trim() === '') open = null
			return true
		}
		if (m && !(m[1][0] === '`' && m[2].includes('`'))) {
			open = m[1]
			return true
		}
		return false
	}
}

// ---- 指摘のファイルを読む ----

/** 32bit の FNV-1a（UTF-16 の単位）を 36 進 7 桁で。id を短く安定させるためだけ（暗号ではない）。decisions.py と同じ。 */
export function hash(s) {
	let h = 0x811c9dc5
	for (let i = 0; i < s.length; i++) {
		h ^= s.charCodeAt(i)
		h = Math.imul(h, 0x01000193)
	}
	return (h >>> 0).toString(36).padStart(7, '0')
}

/** `168` / `160-186` / `L160〜L186` / `L168`。読めなければ null。 */
export function parseLine(v) {
	const m = /^L?(\d+)(?:\s*[-〜~–]\s*L?(\d+))?$/.exec(String(v).trim())
	if (!m) return null
	const start = Number(m[1])
	const end = Number(m[2] ?? m[1])
	if (start < 1 || end < start) return null
	return { start, end }
}

export const formatLine = (l) => (l.start === l.end ? `L${l.start}` : `L${l.start}-${l.end}`)

const IMPACT_YES = ['あり', '有り', '有', 'yes', 'true', 'user']
const IMPACT_NO = ['なし', '無し', '無', 'no', 'false', 'none']

/** `- impact:` の値。`あり` / `なし` のあとに `: 理由`・`（理由）`・` 理由` を付けられる。読めなければ null。 */
export function parseImpact(v) {
	const t = String(v).trim()
	for (const [words, user] of [[IMPACT_YES, true], [IMPACT_NO, false]]) {
		for (const w of words) {
			const m = new RegExp(`^${w}(?:\\s*[:：]\\s*([\\s\\S]*)|\\s*[（(]([\\s\\S]*)[）)]|\\s+([\\s\\S]*))?$`, 'i').exec(t)
			if (!m) continue
			const reason = (m[1] ?? m[2] ?? m[3] ?? '').trim()
			return { user, ...(reason ? { reason } : {}) }
		}
	}
	return null
}

/** `- recommend:` の値。`直してもらう 8, 直さない 3`（`名前: 8`・`名前 8/10` も読む）。読めない項目は errors に入れて飛ばす。 */
export function parseRecommend(v) {
	const items = []
	const errors = []
	for (const part of String(v).split(/[,、，]/).map((p) => p.trim()).filter(Boolean)) {
		const m = /^(.+?)\s*[:：=]?\s*(\d{1,3})(?:\s*\/\s*10)?$/.exec(part)
		const score = m ? Number(m[2]) : NaN
		if (!m || score > RECOMMEND_MAX) {
			errors.push(`推奨度が読めません: ${part}（「名前 0〜${RECOMMEND_MAX}」の形）`)
			continue
		}
		const name = m[1].trim()
		if (items.some((x) => x.name === name)) errors.push(`推奨度の「${name}」が 2 回あります（先のものを使います）`)
		else items.push({ name, score })
	}
	return { items, errors }
}

export const formatRecommend = (r) => r.map((x) => `${x.name} ${x.score}`).join(', ')

const unquote = (v) => String(v).replace(/^`+|`+$/g, '').trim()

/** ファイルの行。改行は \n にそろえる（書き戻しも \n で書く）。 */
export const splitLines = (text) => text.replace(/\r\n?/g, '\n').split('\n')

function readFront(lines) {
	if (lines[0]?.trim() !== '---') return { front: {}, next: 0 }
	const front = {}
	for (let i = 1; i < lines.length; i++) {
		if (lines[i].trim() === '---') return { front, next: i + 1 }
		const kv = /^([A-Za-z][\w-]*):\s*(.*)$/.exec(lines[i])
		if (kv) front[kv[1]] = kv[2].replace(/^["']|["']$/g, '').trim()
	}
	return { front: {}, next: 0 }
}

/**
 * 指摘のファイル（まとまり 1 つ）を読む。壊れた指摘は飛ばして errors に理由を書く（投げない）。
 * 戻り値: { name, title, base?, items: [{ id, set, title, priority, file, line, quote?, status, note?, resolvedAt?, aiNote?, impact?, recommend?, body, span }], errors }
 */
export function parseReview(name, text) {
	const lines = splitLines(text)
	const { front, next } = readFront(lines)
	const errors = []
	const items = []
	const heads = []
	const inFence = fenceTracker()
	for (let i = next; i < lines.length; i++) {
		if (!inFence(lines[i]) && /^## \S/.test(lines[i])) heads.push(i)
	}
	const pending = []
	heads.forEach((h, k) => {
		const end = heads[k + 1] ?? lines.length
		const where = `L${h + 1}`
		let title = lines[h].slice(3).trim()
		let priority = null
		const p = /^\[p(\d{1,3})\]\s*(.*)$/i.exec(title)
		if (p) {
			priority = Number(p[1])
			title = p[2].trim()
		}
		const meta = {}
		let metaEnd = h + 1
		for (let i = h + 1; i < end; i++) {
			const m = META_LINE.exec(lines[i])
			if (!m) break
			meta[m[1]] = (m[2] ?? '').trim()
			metaEnd = i + 1
		}
		if (!title) return void errors.push(`${where}: 見出しが空です`)
		if (!meta.file) return void errors.push(`${where}: 「- file:」がありません（${title}）`)
		let line = null
		if (meta.line) {
			line = parseLine(meta.line)
			if (!line) errors.push(`${where}: line の形が読めません: ${meta.line}（ファイル全体への指摘として扱います）`)
		}
		if (meta.priority) {
			const n = Number(meta.priority.replace(/^p/i, ''))
			if (Number.isInteger(n) && n >= 0) priority = n
			else errors.push(`${where}: priority が数ではありません: ${meta.priority}`)
		}
		let status = 'open'
		if (meta.status) {
			if (REVIEW_STATUSES.includes(meta.status)) status = meta.status
			else errors.push(`${where}: status は open か resolved です: ${meta.status}（open として扱います）`)
		}
		let impact = null
		if (meta.impact) {
			impact = parseImpact(meta.impact)
			if (!impact) errors.push(`${where}: impact は あり か なし です: ${meta.impact}（${title}）`)
		}
		let recommend = []
		if (meta.recommend) {
			const r = parseRecommend(meta.recommend)
			recommend = r.items
			for (const e of r.errors) errors.push(`${where}: ${e}（${title}）`)
		}
		const file = unquote(meta.file).replace(/^\.\//, '')
		let pin = null
		if (meta.id) {
			if (PINNED_ID.test(meta.id)) pin = `${name}/${meta.id}`
			else errors.push(`${where}: id の形が読めません: ${meta.id}（見出しから id を作ります）`)
		}
		pending.push({
			key: `${file}\n${title}`,
			pin,
			item: {
				set: name,
				title,
				priority,
				file,
				line,
				...(meta.quote ? { quote: unquote(meta.quote) } : {}),
				status,
				...(meta.note ? { note: meta.note } : {}),
				...(meta['resolved-at'] ? { resolvedAt: meta['resolved-at'] } : {}),
				...(meta['ai-note'] ? { aiNote: meta['ai-note'] } : {}),
				...(impact ? { impact } : {}),
				...(recommend.length ? { recommend } : {}),
				body: lines.slice(metaEnd, end).join('\n').trim(),
				span: { heading: h, metaEnd, end },
			},
		})
	})
	// id: `- id:` に書いてあればそれ。無ければ file と見出しのハッシュで、同じものが並ぶと 2 件目から `-2`・`-3`（書いてある id とは重ねない）。
	const taken = new Set()
	for (const p of pending) {
		if (!p.pin) continue
		if (taken.has(p.pin)) {
			errors.push(`L${p.item.span.heading + 1}: 同じ id がもうあります: ${p.pin}（見出しから id を作ります）`)
			p.pin = null
		} else taken.add(p.pin)
	}
	for (const p of pending) {
		let id = p.pin
		if (!id) {
			const h = `${name}/${hash(p.key)}`
			let n = 1
			while (taken.has(n > 1 ? `${h}-${n}` : h)) n++
			id = n > 1 ? `${h}-${n}` : h
			taken.add(id)
		}
		items.push({ id, ...p.item })
	}
	return { name, title: front.title || name, ...(front.base ? { base: front.base } : {}), items, errors }
}

// ---- 書き戻し（純粋な関数。全文を受け取り、書き換えた全文を返す） ----

/** 1 行に収める（改行・連続する空白は 1 つの空白に）。 */
export const oneLine = (s, max = NOTE_CHARS) => String(s).replace(/\s+/g, ' ').trim().slice(0, max)

/**
 * id の指摘のメタを書き換えた全文を返す。patch は
 *   { status: 'resolved', note?, at }（人が解決済みにした）/ { status: 'open' }（人が戻した）/ { status: 'open', aiNote }（agent が差し戻した）。
 * メタのうち status・note・resolved-at（差し戻しなら ai-note も）だけを入れ替え、ほかの行・本文・ほかの指摘には触らない。
 * 同じ file・見出しの指摘がほかにもあれば `- id:` を書いて固定する（前の指摘を消しても `x-2` が `x` に詰まらない）。
 */
export function applyStatus(name, text, id, patch) {
	const set = parseReview(name, text)
	const item = set.items.find((i) => i.id === id)
	if (!item) throw new Error(`指摘が見つかりません（ファイルが書き換わったかもしれません。読み込み直してください）: ${id}`)
	const lines = splitLines(text)
	const drop = new Set(['status', 'note', 'resolved-at', 'id'])
	if ('aiNote' in patch) drop.add('ai-note')
	const before = lines.slice(item.span.heading + 1, item.span.metaEnd)
	const meta = before.filter((l) => !drop.has(META_LINE.exec(l)?.[1] ?? ''))
	const pinned = before.some((l) => META_LINE.exec(l)?.[1] === 'id')
	if (pinned || set.items.some((x) => x !== item && x.file === item.file && x.title === item.title)) meta.push(`- id: ${item.id.slice(name.length + 1)}`)
	if (patch.status === 'resolved') {
		meta.push('- status: resolved', `- resolved-at: ${patch.at}`)
		const note = oneLine(patch.note ?? '')
		if (note) meta.push(`- note: ${note}`)
	} else if ('aiNote' in patch) {
		const ai = oneLine(patch.aiNote)
		if (ai) meta.push(`- ai-note: ${ai}`)
	}
	return [...lines.slice(0, item.span.heading + 1), ...meta, ...lines.slice(item.span.metaEnd)].join('\n')
}

/** id の指摘を見出しから次の見出しの手前まで消した全文を返す。同じ file・見出しの残りの指摘は `- id:` を書いて固定する。 */
export function removeItem(name, text, id) {
	const set = parseReview(name, text)
	const item = set.items.find((i) => i.id === id)
	if (!item) throw new Error(`指摘が見つかりません: ${id}`)
	const lines = splitLines(text)
	// 書き足しと消すのを後ろの行から順にして、まだ触っていない場所の行番号をずらさない。
	const edits = [{ at: item.span.heading, run: () => lines.splice(item.span.heading, item.span.end - item.span.heading) }]
	for (const x of set.items) {
		if (x === item || x.file !== item.file || x.title !== item.title) continue
		if (lines.slice(x.span.heading + 1, x.span.metaEnd).some((l) => META_LINE.exec(l)?.[1] === 'id')) continue
		edits.push({ at: x.span.metaEnd, run: () => lines.splice(x.span.metaEnd, 0, `- id: ${x.id.slice(name.length + 1)}`) })
	}
	for (const e of edits.sort((a, b) => b.at - a.at)) e.run()
	return lines.join('\n')
}

// ---- 行の引き直し ----

const norm = (s) => String(s).replace(/`/g, '').replace(/\s+/g, ' ').trim()

/** 行が quote を含むか。quote の `…`・`...` は省略とみなし、前後の部分がこの順で含まれていればよい。 */
export function quoteMatches(text, quote) {
	const parts = norm(quote).split(/\s*(?:…|\.\.\.)\s*/).filter((p) => p !== '')
	if (!parts.length) return false
	const t = norm(text)
	let from = 0
	for (const p of parts) {
		const i = t.indexOf(p, from)
		if (i < 0) return false
		from = i + p.length
	}
	return true
}

/**
 * 指摘の行を今のソースに合わせる。place は ok（書かれた行のまま）/ moved（quote で探し直して動かした）/
 * lost（quote が見つからない。もう直っているかも）/ page（行も quote も無い = ファイル全体への指摘）。
 */
export function relocate(line, quote, source) {
	const q = quote?.split('\n').find((l) => norm(l) !== '') ?? ''
	if (!line) {
		if (!norm(q)) return { line: null, place: 'page' }
		const i = source.findIndex((l) => quoteMatches(l, q))
		// quote を書いたのに見つからないなら、行が無くても lost（もう直っているかも）。quote が無いときだけ page
		return i < 0 ? { line: null, place: 'lost' } : { line: { start: i + 1, end: i + 1 }, place: 'ok' }
	}
	if (!norm(q)) return { line, place: line.start <= source.length ? 'ok' : 'lost' }
	if (quoteMatches(source.slice(line.start - 1, line.end).join(' '), q)) return { line, place: 'ok' }
	let best = -1
	source.forEach((l, i) => {
		if (quoteMatches(l, q) && (best < 0 || Math.abs(i + 1 - line.start) < Math.abs(best + 1 - line.start))) best = i
	})
	if (best < 0) return { line, place: 'lost' }
	// 範囲の指摘は、quote の行が範囲の先頭にあるとみなして範囲ごと動かす（幅はそのまま）。
	const delta = best + 1 - line.start
	return { line: { start: line.start + delta, end: line.end + delta }, place: 'moved' }
}

// ---- 人が足す選択肢（_actions.md） ----

export const LABEL_CHARS = 20
export const AGENT_CHARS = 300
export const MAX_ACTIONS = 8
const RESERVED_LABELS = [KEEP_NOTE, SEND_LABEL, RESOLVE_LABEL, SENT_NOTE]
const ACTION_META = /^- (agent|decision):(?:\s+(.*))?$/
const ACTIONS_HEADER = [
	'# Review の選択肢',
	'',
	'popup の「直さない」「直してもらう」「解決済みにする」の隣に並ぶボタン。1 つ = 見出し 1 つ。',
	'押すと指摘を解決済みにして、note の頭に見出しの文字を書く。agent は `- agent:` に書いたとおりに扱う。',
	'',
]

/** ボタンの文字として使えるか。使えなければ理由を返す。 */
export function labelProblem(label) {
	if (!label) return '名前が空です'
	if ([...label].length > LABEL_CHARS) return `名前は ${LABEL_CHARS} 文字までです`
	if (/[:：\n\r]/.test(label)) return '名前に「:」は使えません（note の「名前: ひとこと」の区切りに使うため）'
	if (/^\[p\d/i.test(label)) return '名前を [pN] で始められません'
	if (RESERVED_LABELS.includes(label)) return `「${label}」は初めから並んでいるボタンか、決まった文字です`
	return null
}

/** `_actions.md` を読む。読めない選択肢は飛ばして errors に理由を書く（投げない）。戻り値の actions は [{ label, agent?, decision? }]。 */
export function parseActions(text) {
	const lines = splitLines(text)
	const actions = []
	const errors = []
	let cur = null
	let inMeta = false
	const inFence = fenceTracker()
	const flush = () => {
		if (!cur) return
		const { line, ...a } = cur
		const problem = labelProblem(a.label)
		if (problem) errors.push(`L${line}: ${problem}（${a.label}）`)
		else if (actions.some((x) => x.label === a.label)) errors.push(`L${line}: 同じ名前の選択肢がもうあります（${a.label}）`)
		else if (actions.length >= MAX_ACTIONS) errors.push(`L${line}: 選択肢は ${MAX_ACTIONS} 個までです（${a.label} から先は出しません）`)
		else actions.push(a)
		cur = null
	}
	lines.forEach((l, i) => {
		if (inFence(l)) return
		if (/^## /.test(l)) {
			flush()
			cur = { label: l.slice(3).trim(), line: i + 1 }
			inMeta = true
			return
		}
		if (!cur || !inMeta) return
		const m = ACTION_META.exec(l)
		if (!m) return void (inMeta = false)
		const v = (m[2] ?? '').trim()
		if (m[1] === 'agent' && v) cur.agent = v
		if (m[1] === 'decision' && v) {
			if (DECISIONS.includes(v)) cur.decision = v
			else errors.push(`L${i + 1}: decision は ${DECISIONS.join(' / ')} のどれかです: ${v}（${cur.label}）`)
		}
	})
	flush()
	return { actions, errors }
}

function actionBlock(label, a) {
	const agent = a.agent ? oneLine(a.agent, AGENT_CHARS) : ''
	return [`## ${label}`, ...(agent ? [`- agent: ${agent}`] : []), ...(a.decision ? [`- decision: ${a.decision}`] : [])]
}

function findAction(lines, label) {
	let start = -1
	let end = lines.length
	const inFence = fenceTracker()
	for (let i = 0; i < lines.length; i++) {
		if (inFence(lines[i]) || !/^## /.test(lines[i])) continue
		if (start >= 0) {
			end = i
			break
		}
		if (lines[i].slice(3).trim() === label) start = i
	}
	if (start < 0) throw new Error(`選択肢がありません: ${label}（読み込み直してください）`)
	let metaEnd = start + 1
	while (metaEnd < end && ACTION_META.test(lines[metaEnd])) metaEnd++
	return { start, metaEnd, end }
}

/** 選択肢を 1 つ書き足した全文を返す（text が null ならファイルを作る）。 */
export function addAction(text, a) {
	const label = oneLine(a.label, 200)
	const problem = labelProblem(label)
	if (problem) throw new Error(problem)
	const { actions } = parseActions(text ?? '')
	if (actions.some((x) => x.label === label)) throw new Error(`「${label}」はもうあります`)
	if (actions.length >= MAX_ACTIONS) throw new Error(`選択肢は ${MAX_ACTIONS} 個までです（${ACTIONS_FILE} から消してください）`)
	if (a.decision && !DECISIONS.includes(a.decision)) throw new Error(`decision は ${DECISIONS.join(' / ')} のどれかです`)
	const head = text === null || text === undefined || !text.trim() ? ACTIONS_HEADER : splitLines(text.replace(/\n+$/, ''))
	return [...head, ...(head[head.length - 1] === '' ? [] : ['']), ...actionBlock(label, a), ''].join('\n')
}

/** 名前が from の選択肢を、その場で a に書き換えた全文を返す（並びと、メタの後ろの人のメモは変えない）。 */
export function updateAction(text, from, a) {
	const label = oneLine(a.label, 200)
	const problem = labelProblem(label)
	if (problem) throw new Error(problem)
	if (label !== from && parseActions(text).actions.some((x) => x.label === label)) throw new Error(`「${label}」はもうあります`)
	if (a.decision && !DECISIONS.includes(a.decision)) throw new Error(`decision は ${DECISIONS.join(' / ')} のどれかです`)
	const lines = splitLines(text)
	const { start, metaEnd } = findAction(lines, from)
	return [...lines.slice(0, start), ...actionBlock(label, a), ...lines.slice(metaEnd)].join('\n')
}

/** 名前が label の選択肢を消した全文を返す。 */
export function removeAction(text, label) {
	const lines = splitLines(text)
	const { start, end } = findAction(lines, label)
	return [...lines.slice(0, start), ...lines.slice(end)].join('\n').replace(/\n{3,}/g, '\n\n')
}

/** 選択肢で解決済みにするときの note（`後で` / `後で: ひとこと`）。 */
export const noteOf = (label, note) => (String(note ?? '').trim() ? `${label}: ${String(note).trim()}` : label)

const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * note がどの名前で始まるかと、その後ろの理由（無ければ null）。decisions.py の note_choice と同じ読み方。
 * popup は `名前` / `名前: 理由` と書く。手で付けた `名前（理由）`・`名前 (理由)`・`名前 理由` も受ける。名前は長いものから試す。
 */
export function matchNote(note, names) {
	const n = String(note ?? '').trim()
	for (const name of [...names].sort((a, b) => b.length - a.length)) {
		const m = new RegExp(`^${escapeRe(name)}(?:\\s*[:：]\\s*([\\s\\S]*)|\\s*[（(]([\\s\\S]*)[）)]|\\s+([\\s\\S]*))?$`).exec(n)
		if (m) return { name, reason: (m[1] ?? m[2] ?? m[3] ?? '').trim() }
	}
	return null
}

// ---- Feedback の記録との行き来（「直してもらう」） ----

const two = (n) => String(n).padStart(2, '0')
/** `yymmdd`（ローカルの日付）。記録の名前の規則は references/concepts.md「名前と採番」。 */
export const datePrefix = (now) => `${two(now.getFullYear() % 100)}${two(now.getMonth() + 1)}${two(now.getDate())}`

/** 記録の中の `- Review の指摘: <id>`。無ければ null。 */
export function linkedReviewId(recordText) {
	const m = new RegExp(`^\\s*-\\s*${escapeRe(LINK_LABEL)}[:：]\\s*` + '`?([^`\\s]+)`?\\s*$', 'm').exec(recordText)
	return m ? m[1] : null
}

/** 指摘 1 件（view）から、Feedback の記録（schema 1 の Markdown）を作る。loop はいつもの記録として読む。 */
export function reviewToRecord(v, note, { id, now }) {
	const where = v.line ? `${v.file}:${v.line.start === v.line.end ? v.line.start : `${v.line.start}-${v.line.end}`}` : null
	const comment = [`直してほしい: ${v.title}`, String(note ?? '').trim(), v.body.trim()].filter(Boolean).join('\n\n')
	const front = [
		'---',
		'schema: 1',
		`id: ${JSON.stringify(id)}`,
		`kind: ${JSON.stringify(where ? 'element' : 'page')}`,
		...(where ? [`source: ${JSON.stringify(where)}`] : []),
		`created: ${JSON.stringify(now.toISOString())}`,
		'omitted: ["shots","env","view","logs"]',
		'---',
	]
	const body = [
		'',
		'# Review の指摘から: 直してほしい',
		'',
		'案内: Review（agent が書いた指摘）を人が「直してもらう」で送った記録。場所は下の「選んだ位置」。直して archive したら、`review.mjs archived <この記録>` で元の指摘も消す。',
		'',
		'## フィードバックの文',
		'',
		...comment.split('\n').map((l) => (l ? `> ${l}` : '>')),
		'',
		'## 選んだ位置',
		'',
		`- ファイル: \`${v.file}\``,
		...(where ? [`- ソースの行: \`${where}\``] : ['- ソースの行: （ファイル全体への指摘）']),
		...(v.quote ? [`- quote: ${v.quote}`] : []),
		`- ${LINK_LABEL}: ${v.id}`,
		...(v.impact ? [`- ユーザ影響: ${v.impact.user ? 'あり' : 'なし'}${v.impact.reason ? `（${v.impact.reason}）` : ''}`] : []),
		...(v.place === 'lost' ? ['- 注意: quote が今のソースに見つかりません。もう直っているかもしれません'] : []),
		'',
	]
	return [...front, ...body].join('\n')
}

// ---- 置き場の読み書き（lock つき） ----

const LOCK_WAIT_MS = 3000
const LOCK_STALE_MS = 10_000
const readText = (abs) => readFile(abs, 'utf8').then((t) => t, () => null)

/** lock のファイルを排他で作る（O_EXCL）。取れたら外す関数を返す。落ちたプロセスが残した古い lock は消して取り直す。 */
async function lock(path) {
	const until = Date.now() + LOCK_WAIT_MS
	for (;;) {
		try {
			const h = await open(path, 'wx')
			await h.close()
			return () => rm(path, { force: true })
		} catch (e) {
			if (e?.code !== 'EEXIST') throw e
			const age = await stat(path).then((s) => Date.now() - s.mtimeMs, () => 0)
			if (age > LOCK_STALE_MS) {
				await rm(path, { force: true })
				continue
			}
			if (Date.now() > until) throw new Error(`指摘のファイルをほかが書いています（${path} が残っていれば消してください）`)
			await new Promise((r) => setTimeout(r, 15))
		}
	}
}

async function writeAtomic(dir, abs, text) {
	await mkdir(dir, { recursive: true })
	const tmp = `${abs}.${process.pid}.${randomBytes(4).toString('hex')}.tmp`
	try {
		await writeFile(tmp, text, 'utf8')
		await rename(tmp, abs)
	} catch (e) {
		await rm(tmp, { force: true })
		throw e
	}
}

function insideRepo(root, p) {
	if (!p || isAbsolute(p) || p.includes('\0')) return null
	const rel = relative(root, resolve(root, p))
	if (rel === '' || rel.startsWith('..') || isAbsolute(rel)) return null
	return rel.split(sep).join('/')
}

/**
 * 置き場（`<inbox>/review/`）を読み書きする。dev サーバー（popup の受け口）と cli が同じファイルを書くので、
 * 書くときは `<名前>.md.lock` を排他で作ってから読み → 書き換え → 書く。
 * @param {{ inbox: string, root: string, filePrefix?: string }} o
 */
export function createReviewStore({ inbox, root, filePrefix = '' }) {
	const dir = resolve(inbox, REVIEW_SUBDIR)
	const actionsPath = join(dir, ACTIONS_FILE)
	let chain = Promise.resolve()
	const serial = (job) => {
		const next = chain.then(job, job)
		chain = next.catch(() => {})
		return next
	}
	const setPath = (name) => {
		if (!SET_NAME.test(name)) throw new Error(`まとまりの名前が使えません: ${JSON.stringify(name)}（英数字・. _ - だけ）`)
		return join(dir, `${name}.md`)
	}
	async function setNames() {
		const names = await readdir(dir).catch(() => [])
		return names.filter((n) => n.endsWith('.md') && SET_NAME.test(n.slice(0, -3))).map((n) => n.slice(0, -3)).sort()
	}
	async function resolveFile(file, cache) {
		const candidates = [file, ...(filePrefix ? [`${filePrefix.replace(/\/?$/, '/')}${file}`] : [])].map((p) => insideRepo(root, p)).filter(Boolean)
		for (const path of candidates) {
			if (!cache.has(path)) {
				const text = await readText(join(root, path))
				cache.set(path, text === null ? null : splitLines(text))
			}
			const source = cache.get(path)
			if (source) return { path, source }
		}
		return { path: candidates[0] ?? file, source: null }
	}
	function view(item, path, source) {
		const { span, ...rest } = item
		const base = { ...rest, file: path, written: item.line }
		if (!source) return { ...base, place: 'no-file' }
		const r = relocate(item.line, item.quote, source)
		return { ...base, line: r.line, place: r.place }
	}

	/** 全部のまとまりを読む。{ dir, sets: [{ name, title, base?, file, errors }], items: [view], actions, actionErrors } */
	async function load() {
		const cache = new Map()
		const sets = []
		const items = []
		const at = await readText(actionsPath)
		const { actions, errors: actionErrors } = at === null ? { actions: [], errors: [] } : parseActions(at)
		const choices = new Set([...BUILTIN_CHOICES, ...actions.map((a) => a.label)])
		for (const name of await setNames()) {
			const file = relative(root, setPath(name)).split(sep).join('/')
			const text = await readText(setPath(name))
			if (text === null) {
				sets.push({ name, title: name, file, errors: ['読めませんでした'] })
				continue
			}
			const set = parseReview(name, text)
			const errors = [...set.errors]
			for (const item of set.items) {
				const { path, source } = await resolveFile(item.file, cache)
				if (!source) errors.push(`ファイルがありません: ${item.file}（${item.title}）`)
				for (const r of item.recommend ?? []) {
					if (!choices.has(r.name)) errors.push(`L${item.span.heading + 1}: 推奨度の「${r.name}」はボタンにありません（${[...choices].join(' / ')}）（${item.title}）`)
				}
				items.push(view(item, path, source))
			}
			sets.push({ name, title: set.title, ...(set.base ? { base: set.base } : {}), file, errors })
		}
		return { dir, sets, items, actions, actionErrors: actionErrors.map((e) => `${ACTIONS_FILE}: ${e}`) }
	}

	const setOf = (id) => {
		const name = String(id).split('/')[0] ?? ''
		if (!SET_NAME.test(name)) throw new Error(`id の形が違います: ${JSON.stringify(id)}（<まとまり>/<7 文字>）`)
		return name
	}
	/**
	 * lock を取って読み → 書き換え → 書く。then があれば、edit の戻り値を渡して lock の中で非同期の処理をし、
	 * then が返す関数で書き換える（send が記録を作ってから指摘を書き換えるため）。
	 */
	function rewrite(id, edit, then) {
		return serial(async () => {
			const name = setOf(id)
			const abs = setPath(name)
			if ((await readText(abs)) === null) throw new Error(`指摘のファイルがありません: ${abs}`)
			const release = await lock(`${abs}.lock`)
			try {
				const text = await readText(abs)
				if (text === null) throw new Error(`指摘のファイルがありません: ${abs}`)
				const first = edit(name, text)
				const finalEdit = then ? await then(first) : null
				await writeAtomic(dir, abs, finalEdit ? finalEdit(name, text) : first)
			} finally {
				await release()
			}
		})
	}
	function rewriteActions(edit) {
		return serial(async () => {
			await mkdir(dir, { recursive: true })
			const release = await lock(`${actionsPath}.lock`)
			try {
				await writeAtomic(dir, actionsPath, edit(await readText(actionsPath)))
			} finally {
				await release()
			}
		})
	}
	const patch = (id, p) => rewrite(id, (name, text) => applyStatus(name, text, id, p))

	return {
		dir,
		load,
		resolve: (id, note, now = new Date()) => patch(id, { status: 'resolved', note, at: now.toISOString() }),
		reopen: (id) => patch(id, { status: 'open' }),
		sendBack: (id, aiNote) => patch(id, { status: 'open', aiNote }),
		remove: (id) => rewrite(id, (name, text) => removeItem(name, text, id)),
		addAction: (a) => rewriteActions((text) => addAction(text, a)),
		updateAction: (from, a) => rewriteActions((text) => {
			if (text === null) throw new Error(`選択肢がありません: ${from}`)
			return updateAction(text, from, a)
		}),
		removeAction: (label) => rewriteActions((text) => {
			if (text === null) throw new Error(`選択肢がありません: ${label}`)
			return removeAction(text, label)
		}),
		/**
		 * 「直してもらう」: 記録を Inbox に採番して書き（上書きしない）、指摘を `Feedback に送った: <id>` で解決済みにする。記録の id を返す。
		 * UI と cli から同時に送っても記録が 2 つにならないよう、指摘のファイルの lock の中で最新の状態を確かめ直してから書く。
		 */
		send(id, note = '', now = new Date()) {
			let recId = null
			return rewrite(id, (name, text) => {
				const item = parseReview(name, text).items.find((x) => x.id === id)
				if (!item) throw new Error(`指摘が見つかりません: ${id}（list で id を確かめてください）`)
				if (item.status === 'resolved') throw new Error(`もう解決済みです: ${id}（${item.note ?? ''}。送り直すなら先に reopen）`)
				return { item, text }
			}, async ({ item }) => {
				const { path, source } = await resolveFile(item.file, new Map())
				const v = view(item, path, source)
				recId = await writeRecord(inbox, (rid) => reviewToRecord(v, note, { id: rid, now }), now)
				const head = `${SENT_NOTE}: ${recId}`
				return (name, text) => applyStatus(name, text, id, { status: 'resolved', note: String(note).trim() ? `${head} — ${String(note).trim()}` : head, at: now.toISOString() })
			}).then(() => recId)
		},
	}
}

/** Inbox に記録を採番して書く（Inbox と archived/ の同じ日付の最大 + 1。`wx` で上書きしない。競合したら数え直す）。 */
async function writeRecord(inbox, textOf, now) {
	const prefix = datePrefix(now)
	await mkdir(inbox, { recursive: true })
	for (let attempt = 0; attempt < 10; attempt++) {
		const names = [...(await readdir(inbox).catch(() => [])), ...(await readdir(join(inbox, 'archived')).catch(() => []))]
		// 連番の桁は置き場の記録に合わせる（無ければ core と同じ 2 桁。数えるときは 2 桁以上を全部読むので、桁が混ざっても重ならない）
		let max = 0
		let digits = 2
		for (const n of names) {
			const m = /^(\d{6})-(\d{2,})(?:\.[^/]*)?$/.exec(n)
			if (!m) continue
			digits = Math.max(digits, m[2].length)
			if (m[1] === prefix) max = Math.max(max, Number(m[2]))
		}
		const id = `${prefix}-${String(max + 1).padStart(digits, '0')}`
		const archived = join(inbox, 'archived', `${id}.md`)
		const exists = (abs) => stat(abs).then(() => true, () => false)
		// 数えてから書くまでに、ほかのプロセスが同じ番号を書いて archive することがある。書く前と後に archived/ を見て、あれば数え直す
		if (await exists(archived)) continue
		try {
			await writeFile(join(inbox, `${id}.md`), textOf(id), { encoding: 'utf8', flag: 'wx' })
		} catch (e) {
			if (e?.code !== 'EEXIST') throw e
			continue
		}
		if (await exists(archived)) {
			await rm(join(inbox, `${id}.md`), { force: true })
			continue
		}
		return id
	}
	throw new Error(`記録の番号が 10 回続けてぶつかりました: ${inbox}`)
}

// ---- cli ----

const USAGE = `使い方: node review.mjs <サブコマンド> [オプション]
  list [--status=open|resolved|all] [--action=<名前>] [--impact=あり|なし] [--json]
                                    指摘の一覧（既定は all）。id | 状態 | 優先度 | ユーザ影響 | 今の場所 | 見出し | 一言
  show <id>                         1 件の全部（本文・今のソースでの行・人が選んだボタン）
  check                             読めない指摘・無いファイル・quote が見つからない行・ボタンに無い推奨度。問題があれば 1 で終わる
                                    未解決の指摘の impact・recommend の書き漏れは「注意」に出す（終わりのコードは変えない）
  resolve <id> [--note "<一言>"]     解決済みにする（UI が無いとき・人がチャットで答えたとき。note の頭にボタンの名前を書くと、そのボタンを押したのと同じ）
  remove <id> [--force]             確かめた指摘を消す（解決済みのものだけ。未解決も消すなら --force）
  reopen <id> --note "<理由>"        解決済みを差し戻す（ai-note に理由を書く）
  send <id> [--note "<一言>"]        「直してもらう」: 指摘を Feedback の記録（<inbox>/yymmdd-xx.md）にして、指摘を解決済みにする
  archived <記録の id かパス>        記録を archive したあとに回す。記録の「- ${LINK_LABEL}: <id>」の指摘を消す
  actions [--json]                  人が足した選択肢（review/${ACTIONS_FILE}）と、選ばれた指摘の扱い
共通: --inbox <dir>（既定 $FEEDBACK_DIR か <root>/.local/feedback）/ --root <dir>（既定 カレント）/ --file-prefix <dir> / --link <テンプレート>`

function parseArgs(argv) {
	const positional = []
	const flags = {}
	for (let i = 0; i < argv.length; i++) {
		const a = argv[i]
		if (!a.startsWith('--')) positional.push(a)
		else if (a.includes('=')) flags[a.slice(2, a.indexOf('='))] = a.slice(a.indexOf('=') + 1)
		else if (['json', 'force', 'help'].includes(a.slice(2))) flags[a.slice(2)] = true
		else flags[a.slice(2)] = argv[++i] ?? ''
	}
	return { positional, flags }
}

const clip = (s, n) => ([...s].length > n ? `${[...s].slice(0, n - 1).join('')}…` : s)
const lineText = (v) => (v.line ? `:${formatLine(v.line).slice(1)}` : '')
const impactText = (v) => (v.impact ? (v.impact.user ? '影響あり' : '影響なし') : '影響?')
const PLACE_MARK = { ok: '', moved: '<moved>', lost: '<lost>', page: '', 'no-file': '<no-file>' }

export async function main(argv = process.argv.slice(2), env = process.env, log = console.log) {
	const { positional, flags } = parseArgs(argv)
	const [sub, id] = positional
	if (!sub || flags.help) return log(USAGE)
	const root = resolve(flags.root || '.')
	const inbox = resolve(root, flags.inbox || env.FEEDBACK_DIR || '.local/feedback')
	const filePrefix = flags['file-prefix'] ?? env.FEEDBACK_FILE_PREFIX ?? ''
	const linkTpl = flags.link ?? env.FEEDBACK_REVIEW_LINK ?? ''
	const store = createReviewStore({ inbox, root, filePrefix })

	if (sub === 'archived') {
		if (!id) throw new Error('記録の id かパスを指定してください: archived <記録>（archive したあとに回す）')
		const candidates = id.includes('/') || id.endsWith('.md') ? [resolve(root, id)] : [join(inbox, 'archived', `${id}.md`), join(inbox, `${id}.md`)]
		let text = null
		for (const c of candidates) if ((text = await readText(c)) !== null) break
		if (text === null) throw new Error(`記録が見つかりません: ${candidates.join(' / ')}`)
		const link = linkedReviewId(text)
		if (!link) return log(`指摘を指していない記録です（「- ${LINK_LABEL}:」の行がありません）: ${basename(candidates[0])}。何もしません`)
		if (!(await store.load()).items.some((v) => v.id === link)) return log(`指摘 ${link} はもうありません（消してあります）`)
		await store.remove(link)
		return log(`removed: ${link}（記録 ${basename(candidates[0], '.md')} から）`)
	}

	const data = await store.load()
	const choiceNames = [KEEP_NOTE, SENT_NOTE, ...data.actions.map((a) => a.label)]
	const choiceOf = (v) => (v.status === 'resolved' ? (matchNote(v.note, choiceNames)?.name ?? null) : null)
	const find = () => {
		if (!id) throw new Error(`id を指定してください: ${sub} <id>（list で確かめる）`)
		const v = data.items.find((x) => x.id === id)
		if (!v) throw new Error(`指摘が見つかりません: ${id}（list で id を確かめてください）`)
		return v
	}

	if (sub === 'list') {
		const status = flags.status ?? 'all'
		if (!['open', 'resolved', 'all'].includes(status)) throw new Error(`--status は open / resolved / all のどれか: ${status}`)
		const asked = typeof flags.action === 'string' ? flags.action.trim() : ''
		const action = asked === SEND_LABEL ? SENT_NOTE : asked
		if (action && !choiceNames.includes(action)) log(`（「${asked}」という選択肢はありません。actions で名前を確かめてください）`)
		const impactFlag = typeof flags.impact === 'string' ? flags.impact.trim() : ''
		const impactWant = { あり: true, yes: true, なし: false, no: false }[impactFlag]
		if (impactFlag && impactWant === undefined) throw new Error(`--impact は あり / なし のどちらか: ${impactFlag}`)
		const rows = data.items.filter((v) => (status === 'all' || v.status === status) && (!action || choiceOf(v) === action) && (impactWant === undefined || v.impact?.user === impactWant))
		if (flags.json) return log(JSON.stringify(rows, null, 2))
		if (!data.sets.length) return log(`（指摘のファイルがありません: ${store.dir}）`)
		for (const v of rows) {
			const note = [v.note && `人: ${v.note}`, v.aiNote && `agent: ${v.aiNote}`].filter(Boolean).join(' / ')
			log([v.id, v.status, v.priority === null ? '-' : `p${v.priority}`, impactText(v), `${v.file}${lineText(v)}`, clip(v.title, 70), PLACE_MARK[v.place], note && `（${clip(note, 80)}）`].filter(Boolean).join(' | '))
		}
		const count = (s) => data.items.filter((v) => v.status === s).length
		return log(`計 ${rows.length} 件（open ${count('open')} / resolved ${count('resolved')}）${asked ? `・「${asked}」で絞り込み` : ''}${impactFlag ? `・ユーザ影響 ${impactFlag} で絞り込み` : ''}`)
	}

	if (sub === 'actions') {
		if (flags.json) return log(JSON.stringify({ actions: data.actions, errors: data.actionErrors }, null, 2))
		const n = (label) => data.items.filter((v) => choiceOf(v) === label).length
		const plain = data.items.filter((v) => v.status === 'resolved' && choiceOf(v) === null).length
		log(`初めからのボタン: ${KEEP_NOTE}（${n(KEEP_NOTE)} 件。確かめずに remove）/ ${SEND_LABEL}（${n(SENT_NOTE)} 件。Feedback の消化で直して archive → archived）/ ${RESOLVE_LABEL}（${plain} 件。確かめて remove か reopen）`)
		if (!data.actions.length) log(`人が足した選択肢はありません（${join(store.dir, ACTIONS_FILE)}）`)
		for (const a of data.actions) log([`${a.label}（${n(a.label)} 件）`, a.decision && `判断: ${a.decision}`, `agent: ${a.agent ?? '（書かれていません。人に聞く）'}`].filter(Boolean).join(' | '))
		for (const e of data.actionErrors) log(`読めない行: ${e}`)
		return
	}

	if (sub === 'show') {
		const v = find()
		const set = data.sets.find((s) => s.name === v.set)
		const chosen = data.actions.find((x) => x.label === choiceOf(v))
		const link = linkTpl && linkTpl.replaceAll('{file}', v.file).replaceAll('{line}', v.line ? String(v.line.start) : '').replaceAll('{id}', encodeURIComponent(v.id))
		return log([
			`# ${v.title}`,
			'',
			`- id: ${v.id}`,
			`- まとまり: ${set?.title ?? v.set}（${set?.file ?? ''}）`,
			`- 状態: ${v.status}${v.resolvedAt ? `（${v.resolvedAt}）` : ''}`,
			`- 優先度: ${v.priority === null ? '-' : `p${v.priority}`}`,
			`- ユーザ影響: ${v.impact ? `${v.impact.user ? 'あり' : 'なし'}${v.impact.reason ? `（${v.impact.reason}）` : ''}` : '（書かれていません）'}`,
			...(v.recommend ? [`- 推奨度: ${formatRecommend(v.recommend)}（0〜10）`] : []),
			`- 場所: ${v.file}${lineText(v)}${v.place === 'moved' ? `（書かれた行 ${formatLine(v.written)} から動かした）` : ''}${v.place === 'lost' ? '（quote が今のソースに見つからない。もう直っているかも）' : ''}${v.place === 'no-file' ? '（ファイルが無い）' : ''}`,
			...(v.note ? [`- 人の一言: ${v.note}`] : []),
			...(chosen ? [`- 人が選んだ: ${chosen.label}（${chosen.agent ? `agent: ${chosen.agent}` : 'agent への一言が無い。人に聞く'}${chosen.decision ? `・判断: ${chosen.decision}` : ''}）`] : []),
			...(v.aiNote ? [`- agent の一言: ${v.aiNote}`] : []),
			...(link ? [`- 開く: ${link}`] : []),
			'',
			v.body,
		].join('\n'))
	}

	if (sub === 'check') {
		let problems = 0
		for (const s of data.sets) for (const e of s.errors) { problems++; log(`${s.file}: ${e}`) }
		for (const e of data.actionErrors) { problems++; log(e) }
		for (const v of data.items) if (v.place === 'lost') { problems++; log(`${v.id}: quote が ${v.file}${v.line ? `${lineText(v)} の近く` : ''}に見つかりません（${clip(v.title, 50)}）`) }
		const noImpact = data.items.filter((v) => v.status === 'open' && !v.impact)
		const noRec = data.items.filter((v) => v.status === 'open' && !v.recommend)
		const ids = (xs) => `${xs.slice(0, 5).map((v) => v.id).join(', ')}${xs.length > 5 ? ' …' : ''}`
		if (noImpact.length) log(`注意: ユーザ影響（- impact:）が無い未解決の指摘 ${noImpact.length} 件: ${ids(noImpact)}`)
		if (noRec.length) log(`注意: 推奨度（- recommend:）が無い未解決の指摘 ${noRec.length} 件: ${ids(noRec)}`)
		const moved = data.items.filter((v) => v.place === 'moved').length
		log(`${problems ? 'NG' : 'OK'} 指摘 ${data.items.length} 件・問題 ${problems} 件${moved ? `（quote で行を合わせたもの ${moved} 件）` : ''}: ${store.dir}`)
		if (problems) process.exitCode = 1
		return
	}

	if (sub === 'resolve') {
		const v = find()
		await store.resolve(v.id, typeof flags.note === 'string' ? flags.note : '')
		return log(`resolved: ${v.id}`)
	}

	if (sub === 'remove') {
		const v = find()
		if (v.status !== 'resolved' && !flags.force) throw new Error(`まだ解決済みではありません: ${id}（人が解決済みにしたものだけを消します。消すなら --force）`)
		await store.remove(v.id)
		return log(`removed: ${v.id}（${clip(v.title, 60)}）`)
	}

	if (sub === 'reopen') {
		const v = find()
		const note = typeof flags.note === 'string' ? flags.note.trim() : ''
		if (!note) throw new Error('--note "<理由>" が必要です（何が直っていないか。人が読みます）')
		await store.sendBack(v.id, note)
		return log(`reopened: ${v.id}`)
	}

	if (sub === 'send') {
		const v = find()
		if (v.status === 'resolved') throw new Error(`もう解決済みです: ${id}（送り直すなら先に reopen）`)
		const rec = await store.send(v.id, typeof flags.note === 'string' ? flags.note : '')
		return log(`sent: ${v.id} → ${join(inbox, `${rec}.md`)}`)
	}

	throw new Error(`知らないサブコマンド: ${sub}\n${USAGE}`)
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
	main().catch((e) => {
		console.error(`エラー: ${e instanceof Error ? e.message : e}`)
		process.exitCode = 1
	})
}
