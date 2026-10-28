// Sink の契約テスト（vitest）。file と memory に同じテストを掛ける。新しい実装を足したら makers に 1 つ足すだけでよい。【実績: Web 参照実装】
// 回し方: 導入先で `npx vitest run <このフォルダ>`（vitest を devDependencies に入れる）。
import { mkdir, mkdtemp, readdir, readFile, rm, stat, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterAll, describe, expect, it } from 'vitest'
import { createFileSink } from './file-sink'
import { createMemorySink } from './memory-sink'
import type { Sink } from './sink-types'

const dirs: string[] = []
const tmp = async () => {
	const d = await mkdtemp(join(tmpdir(), 'fb-sink-'))
	dirs.push(d)
	return d
}
afterAll(async () => {
	await Promise.all(dirs.map((d) => rm(d, { recursive: true, force: true })))
})

type Made = { sink: Sink; root?: string }
const makers: Record<string, () => Promise<Made>> = {
	file: async () => {
		const root = join(await tmp(), 'feedback')
		return { sink: createFileSink(root), root }
	},
	memory: async () => ({ sink: createMemorySink() }),
}

const day = new Date(2026, 9, 29, 10)
const ids = async (s: Sink, state?: 'open' | 'archived' | 'all') => (await s.list({ state })).map((e) => e.id)

describe.each(Object.keys(makers))('Sink 契約 [%s]', (kind) => {
	const make = makers[kind]!

	it('採番: 01 から。日付が変われば 01 に戻る', async () => {
		const { sink } = await make()
		expect((await sink.putEntry({ text: 'a', now: day })).id).toBe('261029-01')
		expect((await sink.putEntry({ text: 'b', now: day })).id).toBe('261029-02')
		expect((await sink.putEntry({ text: 'c', now: new Date(2026, 9, 30) })).id).toBe('261030-01')
	})

	it('archived の番号も数える（同じ日に番号が重ならない）', async () => {
		const { sink } = await make()
		await sink.putEntry({ text: 'a', now: day })
		await sink.putEntry({ text: 'b', now: day })
		await sink.archive('261029-02', { note: '対応した', now: day })
		expect((await sink.putEntry({ text: 'c', now: day })).id).toBe('261029-03')
		await sink.archive('261029-03')
		await sink.archive('261029-01')
		// その日の記録がすべて archived でも、次は 04
		expect((await sink.putEntry({ text: 'd', now: day })).id).toBe('261029-04')
	})

	it('text を関数で渡すと、採番した id で文を作る（frontmatter に id を書くため）', async () => {
		const { sink } = await make()
		const e = await sink.putEntry({ text: (id) => `id: ${id}`, now: day })
		expect((await sink.read(e.id)).text).toBe(`id: ${e.id}`)
	})

	it('上書きしない: 何度保存しても別の記録になり、前の本文が残る', async () => {
		const { sink } = await make()
		const a = await sink.putEntry({ text: 'first', now: day })
		const b = await sink.putEntry({ text: 'second', now: day })
		expect(a.id).not.toBe(b.id)
		expect((await sink.read(a.id)).text).toBe('first')
		expect((await sink.read(b.id)).text).toBe('second')
	})

	it('同時に保存しても番号が重ならず、全部残る', async () => {
		const { sink } = await make()
		const results = await Promise.all(Array.from({ length: 25 }, (_, i) => sink.putEntry({ text: `t${i}`, now: day })))
		const got = results.map((r) => r.id)
		expect(new Set(got).size).toBe(25)
		expect((await ids(sink)).length).toBe(25)
		const texts = await Promise.all(got.map(async (id) => (await sink.read(id)).text))
		expect(new Set(texts).size).toBe(25)
	})

	it('id の検査: 不正な id は read / archive で拒否（パスの組み立てに使わない）', async () => {
		const { sink } = await make()
		await sink.putEntry({ text: 'a', now: day })
		for (const bad of ['../x', '261029-01/../..', 'archived/261029-01', '261029-01.md', '', '261029-1', 'HISTORY']) {
			await expect(sink.read(bad)).rejects.toThrow()
			await expect(sink.archive(bad)).rejects.toThrow()
		}
		await expect(sink.read('261029-99')).rejects.toThrow('見つかりません')
	})

	it('writeDoc / readDoc: ../・絶対パス・空を拒否し、正規形にして同じ文書を指す', async () => {
		const { sink } = await make()
		for (const bad of ['../x.md', 'a/../../x.md', '/etc/passwd', '', '.', 'a\\b', 'x\0y']) {
			await expect(sink.writeDoc(bad, 'x')).rejects.toThrow()
			await expect(sink.readDoc(bad)).rejects.toThrow()
		}
		await sink.writeDoc('memo/ja/a.md', 'ok')
		expect(await sink.readDoc('memo/ja/a.md')).toBe('ok')
		expect(await sink.readDoc('./memo//ja/a.md')).toBe('ok')
	})

	it('writeDoc は上書きできる。無い文書は null', async () => {
		const { sink } = await make()
		expect(await sink.readDoc('memo/none.md')).toBeNull()
		await sink.writeDoc('memo/a.md', 'v1')
		await sink.writeDoc('memo/a.md', 'v2')
		expect(await sink.readDoc('memo/a.md')).toBe('v2')
	})

	it('list は直下の記録だけ。archived / memo / HISTORY.md / ほかのフォルダは数えない', async () => {
		const { sink, root } = await make()
		await sink.putEntry({ text: 'a', now: day })
		await sink.writeDoc('memo/ja/page.md', 'memo')
		await sink.writeDoc('HISTORY.md', 'history')
		await sink.putAttachments([{ name: 'window.png', data: new Uint8Array([1]) }], { now: day })
		if (root) {
			await mkdir(join(root, 'old-batch'), { recursive: true })
			await writeFile(join(root, 'old-batch', '260925-01.md'), 'x')
			await mkdir(join(root, '261029-09.md'), { recursive: true }) // 名前だけ記録に見えるフォルダ
			await writeFile(join(root, 'notes.md'), 'x')
		}
		expect(await ids(sink)).toEqual(['261029-01'])
		await sink.archive('261029-01')
		expect(await ids(sink)).toEqual([])
		expect(await ids(sink, 'archived')).toEqual(['261029-01'])
		expect(await ids(sink, 'all')).toEqual(['261029-01'])
	})

	it('archive: 移して末尾に対応メモを 1 行足す。もう一度呼んでも二重にしない', async () => {
		const { sink } = await make()
		await sink.putEntry({ text: '# a\n', now: day })
		const a = await sink.archive('261029-01', { note: '直した\n改行は 1 行にする', now: day })
		expect(a.state).toBe('archived')
		const text = (await sink.read('261029-01')).text
		expect(text.startsWith('# a\n')).toBe(true)
		expect(text.endsWith('\n---\n対応メモ: 直した 改行は 1 行にする（2026-10-29）\n')).toBe(true)
		await sink.archive('261029-01', { note: 'もう一度' })
		expect((await sink.read('261029-01')).text).toBe(text)
	})

	it('putAttachments: 参照を返し、不正な名前は投げずに error', async () => {
		const { sink } = await make()
		const ok = await sink.putAttachments([{ name: 'window.png', data: new Uint8Array([1, 2]) }, { name: 'element.png', data: new Uint8Array([3]) }], { now: day })
		expect(ok.error).toBeNull()
		expect(Object.keys(ok.refs).sort()).toEqual(['element.png', 'window.png'])
		for (const bad of ['../x.png', 'a/b.png', '.hidden', '']) {
			const r = await sink.putAttachments([{ name: bad, data: new Uint8Array([1]) }], { now: day })
			expect(r.error).not.toBeNull()
			expect(r.refs).toEqual({})
		}
	})
})

describe('Sink [file] だけの契約', () => {
	it('採番と書き込みの間に、別のプロセスが同じ番号を保存して archived/ へ移しても重ならない', async () => {
		const root = join(await tmp(), 'feedback')
		const arch = join(root, 'archived')
		const other = async (file: string) => {
			await mkdir(arch, { recursive: true })
			await writeFile(join(arch, file.split('/').pop()!), 'other')
		}
		let firstBefore = true
		const s1 = createFileSink(root, { hooks: { beforeWrite: async (f) => { if (firstBefore) { firstBefore = false; await other(f) } } } })
		expect((await s1.putEntry({ text: 'a', now: day })).id).toBe('261029-02') // 01 は書く前に取られた
		let firstAfter = true
		const s2 = createFileSink(root, { hooks: { afterWrite: async (f) => { if (firstAfter) { firstAfter = false; await other(f) } } } })
		expect((await s2.putEntry({ text: 'b', now: day })).id).toBe('261029-04') // 03 は書いた直後に取られた → 消して数え直す
		const all = [...(await readdir(root)), ...(await readdir(arch))].filter((n) => n.endsWith('.md'))
		expect(new Set(all).size).toBe(all.length)
	})

	it('writeDoc は rename で書く: 上書きで inode が変わり、tmp が残らない', async () => {
		const root = join(await tmp(), 'feedback')
		const sink = createFileSink(root)
		await sink.writeDoc('memo/a.md', 'v1')
		const before = (await stat(join(root, 'memo/a.md'))).ino
		await sink.writeDoc('memo/a.md', 'v2')
		const after = (await stat(join(root, 'memo/a.md'))).ino
		expect(after).not.toBe(before)
		expect(await readdir(join(root, 'memo'))).toEqual(['a.md'])
	})

	it('シンボリックリンクで外へ出る書き込みを拒否', async () => {
		const base = await tmp()
		const outside = join(base, 'outside')
		const root = join(base, 'feedback')
		await mkdir(outside)
		await mkdir(root)
		await symlink(outside, join(root, 'link'))
		const sink = createFileSink(root)
		await expect(sink.writeDoc('link/x.md', 'x')).rejects.toThrow('外')
		await expect(readdir(outside)).resolves.toEqual([])
	})

	it('repoRoot を渡すと ref が repoRoot からの相対になる', async () => {
		const repo = await tmp()
		const sink = createFileSink(join(repo, '.local/feedback'), { repoRoot: repo })
		const e = await sink.putEntry({ text: 'a', now: day })
		expect(e.ref).toBe('.local/feedback/261029-01.md')
		expect(await readFile(join(repo, e.ref), 'utf8')).toBe('a')
		const at = await sink.putAttachments([{ name: 'window.png', data: new Uint8Array([1]) }], { now: day })
		expect(at.refs['window.png']).toBe('.local/feedback/attachments/20261029-100000/window.png')
	})

	it('桁の違う古い記録（3 桁など）も読めて、番号の数えにも入る', async () => {
		const root = join(await tmp(), 'feedback')
		await mkdir(root, { recursive: true })
		await writeFile(join(root, '261029-105.md'), 'old')
		const sink = createFileSink(root)
		expect(await ids(sink)).toEqual(['261029-105'])
		expect((await sink.putEntry({ text: 'new', now: day })).id).toBe('261029-106')
	})

	it('同じ秒の添付は別のフォルダになる', async () => {
		const sink = createFileSink(join(await tmp(), 'fb'))
		const f = [{ name: 'window.png', data: new Uint8Array([1]) }]
		const a = await sink.putAttachments(f, { now: day })
		const b = await sink.putAttachments(f, { now: day })
		expect(a.dir).not.toBe(b.dir)
	})
})
