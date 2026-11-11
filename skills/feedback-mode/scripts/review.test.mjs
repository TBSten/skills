// review.mjs のテスト。node --test scripts/review.test.mjs（Node 18 以上、依存なし）
import assert from 'node:assert/strict'
import { mkdtemp, mkdir, readFile, readdir, rename, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import {
	addAction, applyStatus, createReviewStore, linkedReviewId, main, matchNote, parseActions, parseImpact, parseLine,
	parseRecommend, parseReview, relocate, removeAction, removeItem, updateAction,
} from './review.mjs'

const SET = `---
title: リリース前チェック
base: 1a7a072
---

## [p3] 個別 import している
- file: docs/setup.md
- line: 3
- quote: import foo.bar
- impact: あり: 写すとビルドが失敗する
- recommend: 直してもらう 8, 直さない 2

本文。
- 該当: これはメタではない

\`\`\`md
## これは見出しではない
\`\`\`

## 言い回しが硬い
- file: docs/setup.md
- line: 1

本文 2
`

test('見出し・メタ・本文・フェンスの中の ## を読み分ける', () => {
	const s = parseReview('r1', SET)
	assert.equal(s.title, 'リリース前チェック')
	assert.equal(s.base, '1a7a072')
	assert.equal(s.items.length, 2)
	const [a, b] = s.items
	assert.equal(a.priority, 3)
	assert.equal(a.title, '個別 import している')
	assert.deepEqual(a.line, { start: 3, end: 3 })
	assert.deepEqual(a.impact, { user: true, reason: '写すとビルドが失敗する' })
	assert.deepEqual(a.recommend, [{ name: '直してもらう', score: 8 }, { name: '直さない', score: 2 }])
	assert.match(a.body, /- 該当: これはメタではない/)
	assert.match(a.body, /## これは見出しではない/)
	assert.equal(b.priority, null)
	assert.match(a.id, /^r1\/[0-9a-z]{7}$/)
	assert.deepEqual(s.errors, [])
})

test('line・impact・recommend の書き方の揺れ', () => {
	assert.deepEqual(parseLine('L160〜L186'), { start: 160, end: 186 })
	assert.deepEqual(parseLine('168'), { start: 168, end: 168 })
	assert.equal(parseLine('x'), null)
	assert.deepEqual(parseImpact('なし（内部だけ）'), { user: false, reason: '内部だけ' })
	assert.deepEqual(parseImpact('yes'), { user: true })
	assert.equal(parseImpact('たぶん'), null)
	const r = parseRecommend('直さない: 3、後で 7/10, 変 11')
	assert.deepEqual(r.items, [{ name: '直さない', score: 3 }, { name: '後で', score: 7 }])
	assert.equal(r.errors.length, 1)
})

test('同じ file・見出しは -2 になり、解決済みにすると id を書いて固定する', () => {
	const text = '## 同じ\n- file: a.md\n\nx\n\n## 同じ\n- file: a.md\n\ny\n'
	const s = parseReview('r', text)
	assert.equal(s.items[1].id, `${s.items[0].id}-2`)
	const resolved = applyStatus('r', text, s.items[1].id, { status: 'resolved', note: '直さない: 誤検知', at: '2026-11-11T00:00:00Z' })
	assert.match(resolved, /- id: [0-9a-z]{7}-2\n- status: resolved\n- resolved-at: 2026-11-11T00:00:00Z\n- note: 直さない: 誤検知/)
	// 1 件目を消しても、2 件目の id は変わらない
	const removed = removeItem('r', resolved, s.items[0].id)
	assert.equal(parseReview('r', removed).items[0].id, s.items[1].id)
})

test('remove は残りの同じ見出しの指摘の id を固定する（後ろの指摘が詰まらない）', () => {
	const text = '## 同じ\n- file: a.md\n\nx\n\n## 同じ\n- file: a.md\n\ny\n\n## 同じ\n- file: a.md\n\nz\n'
	const ids = parseReview('r', text).items.map((i) => i.id)
	const after = parseReview('r', removeItem('r', text, ids[0])).items.map((i) => i.id)
	assert.deepEqual(after, ids.slice(1))
})

test('見出しが 1 つだけの指摘は id を書かない（ファイルを汚さない）', () => {
	const text = '## ひとつ\n- file: a.md\n\nx\n'
	const id = parseReview('r', text).items[0].id
	assert.doesNotMatch(applyStatus('r', text, id, { status: 'resolved', at: 't' }), /- id:/)
})

test('差し戻しは解決済みの印を消して ai-note を書く', () => {
	const text = '## a\n- file: a.md\n- status: resolved\n- resolved-at: t\n- note: 直した\n\nx\n'
	const id = parseReview('r', text).items[0].id
	const out = applyStatus('r', text, id, { status: 'open', aiNote: 'まだ L3 に残っています' })
	assert.equal(out, '## a\n- file: a.md\n- ai-note: まだ L3 に残っています\n\nx\n')
})

test('quote で行を探し直す（moved）・見つからない（lost）・… は省略', () => {
	const src = ['a', 'b', 'import foo.bar // x', 'c']
	assert.deepEqual(relocate({ start: 1, end: 2 }, 'import foo.bar', src), { line: { start: 3, end: 4 }, place: 'moved' })
	assert.equal(relocate({ start: 1, end: 1 }, 'zzz', src).place, 'lost')
	assert.equal(relocate({ start: 3, end: 3 }, 'import … // x', src).place, 'ok')
	assert.deepEqual(relocate(null, 'c', src), { line: { start: 4, end: 4 }, place: 'ok' })
	assert.equal(relocate(null, undefined, src).place, 'page')
})

test('選択肢: 読む・足す・直す・消す・使えない名前', () => {
	let text = addAction(null, { label: '後で', agent: 'チケットに積んでから remove', decision: '後で' })
	text = addAction(text, { label: 'PR 待ち', agent: 'マージ後に見直す' })
	assert.deepEqual(parseActions(text).actions, [
		{ label: '後で', agent: 'チケットに積んでから remove', decision: '後で' },
		{ label: 'PR 待ち', agent: 'マージ後に見直す' },
	])
	assert.throws(() => addAction(text, { label: '直さない' }), /初めから/)
	assert.throws(() => addAction(text, { label: 'a: b' }), /「:」/)
	text = updateAction(text, 'PR 待ち', { label: 'PR #65 待ち', agent: 'マージ後に見直す' })
	assert.deepEqual(parseActions(text).actions.map((a) => a.label), ['後で', 'PR #65 待ち'])
	text = removeAction(text, '後で')
	assert.deepEqual(parseActions(text).actions.map((a) => a.label), ['PR #65 待ち'])
	assert.equal(parseActions('## x\n- decision: 謎\n').errors.length, 1)
})

test('note の頭の読み方（名前: 理由 / 名前（理由） / 名前 理由 / 長い名前を優先）', () => {
	const names = ['直さない', 'PR', 'PR #65 待ち']
	assert.deepEqual(matchNote('直さない: 誤検知', names), { name: '直さない', reason: '誤検知' })
	assert.deepEqual(matchNote('直さない（意図どおり）', names), { name: '直さない', reason: '意図どおり' })
	assert.deepEqual(matchNote('PR #65 待ち', names), { name: 'PR #65 待ち', reason: '' })
	assert.equal(matchNote('直した', names), null)
})

async function sandbox() {
	const root = await mkdtemp(join(tmpdir(), 'review-'))
	await mkdir(join(root, 'docs'), { recursive: true })
	await writeFile(join(root, 'docs/setup.md'), '言い回し\n空き\nimport foo.bar\n')
	await mkdir(join(root, '.local/feedback/review'), { recursive: true })
	await writeFile(join(root, '.local/feedback/review/r1.md'), SET)
	const out = []
	const run = async (...argv) => {
		out.length = 0
		process.exitCode = 0
		await main([...argv, '--root', root], {}, (s) => out.push(s))
		return out.join('\n')
	}
	return { root, run }
}

test('cli: list / check / resolve / remove / reopen', async () => {
	const { root, run } = await sandbox()
	const list = await run('list')
	assert.match(list, /p3 \| 影響あり \| docs\/setup\.md:3 \| 個別 import している/)
	assert.match(list, /計 2 件（open 2 \/ resolved 0）/)
	const check = await run('check')
	assert.match(check, /^OK 指摘 2 件・問題 0 件/m)
	assert.match(check, /注意: ユーザ影響（- impact:）が無い未解決の指摘 1 件/)
	const id = parseReview('r1', SET).items[1].id
	await assert.rejects(run('remove', id), /まだ解決済みではありません/)
	await run('resolve', id, '--note', '直した')
	assert.match(await run('list', '--status=resolved'), /人: 直した/)
	await run('reopen', id, '--note', 'まだ残っています')
	assert.match(await readFile(join(root, '.local/feedback/review/r1.md'), 'utf8'), /- ai-note: まだ残っています/)
	await run('resolve', id)
	await run('remove', id)
	assert.match(await run('list'), /計 1 件/)
})

test('cli: check は無いファイル・quote の外れ・ボタンに無い推奨度で 1 を返す', async () => {
	const { root, run } = await sandbox()
	await writeFile(join(root, '.local/feedback/review/bad.md'), '## a\n- file: nope.md\n\n## b\n- file: docs/setup.md\n- line: 1\n- quote: 無い文字\n- recommend: 謎のボタン 5\n')
	const out = await run('check')
	assert.match(out, /ファイルがありません: nope\.md/)
	assert.match(out, /quote が docs\/setup\.md:1 の近くに見つかりません/)
	assert.match(out, /推奨度の「謎のボタン」はボタンにありません/)
	assert.match(out, /^NG /m)
	assert.equal(process.exitCode, 1)
	process.exitCode = 0
})

test('cli: send で記録を作り、archive のあとの archived で指摘も消える', async () => {
	const { root, run } = await sandbox()
	const id = parseReview('r1', SET).items[0].id
	const sent = await run('send', id, '--note', '急ぎ')
	const inbox = join(root, '.local/feedback')
	const rec = (await readdir(inbox)).find((n) => /^\d{6}-\d{2}\.md$/.test(n))
	assert.ok(rec, sent)
	const text = await readFile(join(inbox, rec), 'utf8')
	assert.match(text, /^---\nschema: 1\n/)
	assert.match(text, /> 直してほしい: 個別 import している\n>\n> 急ぎ/)
	assert.equal(linkedReviewId(text), id)
	assert.match(await run('list', '--action=直してもらう'), new RegExp(`人: Feedback に送った: ${rec.slice(0, -3)} — 急ぎ`))
	await mkdir(join(inbox, 'archived'))
	await rename(join(inbox, rec), join(inbox, 'archived', rec))
	assert.match(await run('archived', rec.slice(0, -3)), /removed: /)
	assert.match(await run('list'), /計 1 件/)
	assert.match(await run('archived', rec.slice(0, -3)), /もうありません/)
})

test('cli: actions は選ばれた件数と agent の扱いを出す', async () => {
	const { root, run } = await sandbox()
	await writeFile(join(root, '.local/feedback/review/_actions.md'), addAction(null, { label: '後で', agent: 'チケットに積む', decision: '後で' }))
	const id = parseReview('r1', SET).items[0].id
	await run('resolve', id, '--note', '後で: v2 で')
	const out = await run('actions')
	assert.match(out, /後で（1 件） \| 判断: 後で \| agent: チケットに積む/)
	assert.match(await run('list', '--action=後で'), /計 1 件/)
	assert.match(await run('show', id), /人が選んだ: 後で（agent: チケットに積む・判断: 後で）/)
})

test('store: 同時に書いても消し合わない（lock）', async () => {
	const { root } = await sandbox()
	const a = createReviewStore({ inbox: join(root, '.local/feedback'), root })
	const b = createReviewStore({ inbox: join(root, '.local/feedback'), root })
	const [x, y] = parseReview('r1', SET).items.map((i) => i.id)
	await Promise.all([a.resolve(x, 'A'), b.resolve(y, 'B')])
	const s = parseReview('r1', await readFile(join(root, '.local/feedback/review/r1.md'), 'utf8'))
	assert.deepEqual(s.items.map((i) => i.note), ['A', 'B'])
})

test('--file-prefix: file がそのままで無ければ前に付けて探す', async () => {
	const { root, run } = await sandbox()
	await writeFile(join(root, '.local/feedback/review/p.md'), '## a\n- file: setup.md\n- line: 1\n')
	assert.match(await run('check'), /ファイルがありません: setup\.md/)
	process.exitCode = 0
	assert.match(await run('check', '--file-prefix', 'docs'), /^OK /m)
})
