// ファイルの Sink（TypeScript 版）。置き場のルートの下に、記録・archived・添付・上書きする文書（メモ）を置く。【実績: Web 参照実装】
//   <root>/yymmdd-xx.md            記録（Inbox）
//   <root>/archived/yymmdd-xx.md   対応済み（Archive）
//   <root>/attachments/<日時>/     添付（スクリーンショット）
//   <root>/<relPath>               writeDoc / readDoc の文書（例: memo/<言語>/<ページ>.md）
import { randomBytes } from 'node:crypto'
import { appendFile, lstat, mkdir, open, readdir, readFile, realpath, rename, rm, stat, writeFile } from 'node:fs/promises'
import { dirname, isAbsolute, join, relative, resolve, sep } from 'node:path'
import { ARCHIVE_DIR, datePrefix, formatId, idOfFileName, isEntryId, MAX_PER_DAY, nextNumber } from './naming'
import { safeRelPath } from './paths'
import { ATTACH_NAME, archiveNoteText, type Attachment, type EntryInfo, type EntryState, type PutAttachmentsResult, type Sink } from './sink-types'

const ATTACHMENTS_DIR = 'attachments'

const two = (n: number) => String(n).padStart(2, '0')
const stamp = (d: Date) => `${d.getFullYear()}${two(d.getMonth() + 1)}${two(d.getDate())}-${two(d.getHours())}${two(d.getMinutes())}${two(d.getSeconds())}`

/** ディレクトリ内の名前。無ければ空。 */
async function namesIn(dir: string): Promise<string[]> {
	try {
		return await readdir(dir)
	} catch (e) {
		if ((e as NodeJS.ErrnoException).code === 'ENOENT') return []
		throw e
	}
}

/** 名前があるか（シンボリックリンクはたどらない。壊れたリンクもあるとみなす）。 */
const exists = (p: string) => lstat(p).then(() => true, () => false)
/** 実体のファイルか（シンボリックリンクは false。記録の名前をしたリンクで外のファイルを読ませない）。 */
const isPlainFile = (p: string) => lstat(p).then((s) => s.isFile(), () => false)
/** inode。同じファイルか（rename しても変わらない）を見分ける。無ければ null。 */
const inodeOf = (p: string) => lstat(p, { bigint: true }).then((s) => s.ino, () => null)

/** `root` の中に収まる絶対パス。文字列の検査に加え、既にある一番近い親の実体（シンボリックリンクの先）が root の外なら投げる。 */
async function resolveInside(root: string, relPath: string): Promise<string> {
	const abs = resolve(root, safeRelPath(relPath))
	const rel = relative(root, abs)
	if (rel === '' || rel.startsWith('..') || isAbsolute(rel)) throw new Error(`置き場の外には出せません: ${relPath}`)
	let probe = abs
	for (;;) {
		try {
			await lstat(probe)
			break
		} catch {
			if (probe === root) break
			probe = dirname(probe)
		}
	}
	const realRoot = await realpath(root).catch(() => root)
	// 名前はあるのに実体をたどれない（壊れたシンボリックリンク）なら、行き先を確かめられないので使わない。
	const realProbe = await realpath(probe).catch(() => (probe === root ? root : null))
	const back = realProbe === null ? '..' : relative(realRoot, realProbe)
	if (back.startsWith('..') || isAbsolute(back)) throw new Error(`置き場の外を指しています: ${relPath}`)
	return abs
}

export type FileSinkOptions = {
	/** 渡すと ref・添付の参照を repoRoot からの相対にする。渡さなければ root からの相対。 */
	repoRoot?: string
	/** テスト用: 番号を決めてから書くまで / 書いた直後に呼ぶ（競合を差し込む）。 */
	hooks?: { beforeWrite?(file: string, attempt: number): Promise<void> | void; afterWrite?(file: string, attempt: number): Promise<void> | void }
}

/** @param root 置き場のルート（絶対パス。無ければ作る） */
export function createFileSink(root: string, o: FileSinkOptions = {}): Sink {
	const base = resolve(root)
	const refBase = o.repoRoot ? resolve(o.repoRoot) : base
	const ref = (abs: string) => relative(refBase, abs).split(sep).join('/')
	/** 記録の置き場。archived/ は置き場の中に収まるかを確かめる（シンボリックリンクで外を指していたら投げる）。 */
	const dirOf = async (state: EntryState) => (state === 'open' ? base : resolveInside(base, ARCHIVE_DIR))

	const info = async (id: string, state: EntryState, file: string): Promise<EntryInfo> => {
		const st = await stat(file)
		return { id, ref: ref(file), state, bytes: st.size, createdAt: st.birthtime.toISOString() }
	}
	const locate = async (id: string): Promise<{ state: EntryState; file: string }> => {
		if (!isEntryId(id)) throw new Error(`id が不正です: ${JSON.stringify(id)}`)
		for (const state of ['open', 'archived'] as const) {
			const file = join(await dirOf(state), `${id}.md`)
			if (await isPlainFile(file)) return { state, file }
		}
		throw new Error(`見つかりません: ${id}`)
	}
	const scan = async (state: EntryState): Promise<EntryInfo[]> => {
		const dir = await dirOf(state)
		const out: EntryInfo[] = []
		for (const name of await namesIn(dir)) {
			const id = idOfFileName(name)
			if (!id) continue
			const file = join(dir, name)
			if (await isPlainFile(file)) out.push(await info(id, state, file))
		}
		return out
	}

	return {
		kind: 'file',
		capabilities: { put: true, list: true, read: true, archive: true, doc: true },

		/**
		 * `<root>/yymmdd-xx.md` に排他的（wx）に書く。番号は一覧から決めるが、同時の保存と取り合うので、作れた番号を採用する。
		 * 数えてから書くまでに、別のプロセスが同じ番号を保存して archived/ へ移すと wx は通ってしまうので、書く前と書いた後に archived/ を確かめる。
		 * 書いた後の archived/ の同じ名前が自分の書いたファイル（inode が同じ）なら、消化する側がもう対応済みにしただけなので、保存できたとして返す。
		 * 文（text の関数）はファイルを作る前に組み立てる。書けなかったら作ったファイルを消す（空の記録を残さない）。
		 */
		async putEntry({ text, now = new Date() }) {
			await mkdir(base, { recursive: true })
			const prefix = datePrefix(now)
			const archivedHas = async (id: string) => (await namesIn(join(base, ARCHIVE_DIR))).includes(`${id}.md`)
			let attempts = 0
			for (let tries = 0; tries < 10; tries++) {
				const start = nextNumber([...(await namesIn(base)), ...(await namesIn(join(base, ARCHIVE_DIR)))], prefix)
				if (start === null) break
				for (let n = start; n <= MAX_PER_DAY; n++) {
					attempts++
					const id = formatId(prefix, n)
					const file = join(base, `${id}.md`)
					const body = typeof text === 'function' ? text(id) : text
					await o.hooks?.beforeWrite?.(file, attempts)
					if (await archivedHas(id)) continue
					let handle
					try {
						handle = await open(file, 'wx')
					} catch (e) {
						if ((e as NodeJS.ErrnoException).code === 'EEXIST') continue
						throw e
					}
					let mine: bigint
					try {
						await handle.writeFile(body, 'utf8')
						mine = (await handle.stat({ bigint: true })).ino
					} catch (e) {
						await handle.close()
						await rm(file, { force: true })
						throw e
					}
					await handle.close()
					await o.hooks?.afterWrite?.(file, attempts)
					// Inbox の同じ名前はもう自分のもの。ここを通れば以後この番号は重ならない。
					const archivedFile = join(base, ARCHIVE_DIR, `${id}.md`)
					const archivedIno = await inodeOf(archivedFile)
					// 自分の記録が、もう対応済みへ移された（消化する側が見つけてすぐ archive した）。二重に保存しない。
					if (archivedIno === mine) return { ...(await info(id, 'archived', archivedFile)), attempts }
					// 別の記録が同じ番号で archived にある。自分の分を消して数え直す。
					if (archivedIno !== null) {
						if ((await inodeOf(file)) === mine) await rm(file, { force: true })
						break
					}
					return { ...(await info(id, 'open', file)), attempts }
				}
			}
			throw new Error(`その日の連番（${MAX_PER_DAY}）を使い切ったか、空いている番号を取れませんでした`)
		},

		async list({ state = 'open' } = {}) {
			const out = [...(state !== 'archived' ? await scan('open') : []), ...(state !== 'open' ? await scan('archived') : [])]
			return out.sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
		},

		async read(id) {
			const { state, file } = await locate(id)
			return { ...(await info(id, state, file)), text: await readFile(file, 'utf8') }
		},

		async archive(id, { note, now = new Date() } = {}) {
			const { state, file } = await locate(id)
			if (state === 'archived') return info(id, state, file)
			const dest = await resolveInside(base, `${ARCHIVE_DIR}/${id}.md`)
			await mkdir(dirname(dest), { recursive: true })
			if (await exists(dest)) throw new Error(`既にあります: ${ref(dest)}`)
			await rename(file, dest)
			if (note) await appendFile(dest, archiveNoteText(note, now), 'utf8')
			return info(id, 'archived', dest)
		},

		/** `<root>/attachments/<YYYYMMDD-HHMMSS>/<name>`。同じ秒は -2, -3 …。失敗しても投げず error に入れる。 */
		async putAttachments(files: Attachment[], { now = new Date() } = {}): Promise<PutAttachmentsResult> {
			try {
				if (files.length === 0) return { refs: {}, dir: null, error: '添付がありません' }
				for (const f of files) if (!ATTACH_NAME.test(f.name)) throw new Error(`添付の名前が使えません: ${f.name}`)
				const root0 = await resolveInside(base, ATTACHMENTS_DIR)
				await mkdir(root0, { recursive: true })
				let dir = ''
				// mkdir を再帰なしで試し、取り合いに強くする。
				for (let i = 1; ; i++) {
					const candidate = join(root0, i === 1 ? stamp(now) : `${stamp(now)}-${i}`)
					try {
						await mkdir(candidate)
						dir = candidate
						break
					} catch (e) {
						if ((e as NodeJS.ErrnoException).code !== 'EEXIST') throw e
					}
				}
				const refs: Record<string, string> = {}
				for (const f of files) {
					await writeFile(join(dir, f.name), f.data)
					refs[f.name] = ref(join(dir, f.name))
				}
				return { refs, dir: ref(dir), error: null }
			} catch (e) {
				return { refs: {}, dir: null, error: e instanceof Error ? e.message : String(e) }
			}
		},

		/** 同じディレクトリの tmp に書いてから rename する。読み手に書きかけが見えない。 */
		async writeDoc(relPath, text) {
			const abs = await resolveInside(base, relPath)
			await mkdir(dirname(abs), { recursive: true })
			const tmp = `${abs}.${process.pid}.${randomBytes(4).toString('hex')}.tmp`
			try {
				await writeFile(tmp, text, 'utf8')
				await rename(tmp, abs)
			} catch (e) {
				await rm(tmp, { force: true })
				throw e
			}
		},

		async readDoc(relPath) {
			const abs = await resolveInside(base, relPath)
			try {
				return await readFile(abs, 'utf8')
			} catch (e) {
				if ((e as NodeJS.ErrnoException).code === 'ENOENT') return null
				throw e
			}
		},
	}
}
