// メモリの Sink（TypeScript 版）。契約テストと、ほかの部品のテスト用。file-sink.ts と同じ規則（採番・archive・パス）にそろえる。【実績: Web 参照実装】
import { datePrefix, formatId, isEntryId, nextNumber } from './naming'
import { safeRelPath } from './paths'
import { ATTACH_NAME, archiveNoteText, type Attachment, type EntryInfo, type EntryState, type PutAttachmentsResult, type Sink } from './sink-types'

export type MemorySink = Sink & {
	entries: Map<string, { text: string; state: EntryState; createdAt: string }>
	docs: Map<string, string>
	attachments: Map<string, Uint8Array>
}

export function createMemorySink(): MemorySink {
	const entries: MemorySink['entries'] = new Map()
	const docs = new Map<string, string>()
	const attachments = new Map<string, Uint8Array>()
	let attachSeq = 0
	const enc = new TextEncoder()
	const info = (id: string): EntryInfo => {
		const e = entries.get(id)!
		return { id, ref: `memory:${id}`, state: e.state, bytes: enc.encode(e.text).length, createdAt: e.createdAt }
	}
	const need = (id: string) => {
		if (!isEntryId(id) || !entries.has(id)) throw new Error(`見つかりません: ${id}`)
	}
	return {
		kind: 'memory',
		capabilities: { put: true, list: true, read: true, archive: true, doc: true },
		entries,
		docs,
		attachments,
		async putEntry({ text, now = new Date() }) {
			const prefix = datePrefix(now)
			// archived も entries に残っている（state が変わるだけ）ので、ここで数えれば重ならない。
			const n = nextNumber(entries.keys(), prefix)
			if (n === null) throw new Error('その日の連番を使い切りました')
			const id = formatId(prefix, n)
			entries.set(id, { text: typeof text === 'function' ? text(id) : text, state: 'open', createdAt: now.toISOString() })
			return { ...info(id), attempts: 1 }
		},
		async list({ state = 'open' } = {}) {
			return [...entries.keys()].filter((id) => state === 'all' || entries.get(id)!.state === state).sort().map(info)
		},
		async read(id) {
			need(id)
			return { ...info(id), text: entries.get(id)!.text }
		},
		async archive(id, { note, now = new Date() } = {}) {
			need(id)
			const e = entries.get(id)!
			if (e.state === 'archived') return info(id)
			e.state = 'archived'
			if (note) e.text += archiveNoteText(note, now)
			return info(id)
		},
		async putAttachments(files: Attachment[]): Promise<PutAttachmentsResult> {
			try {
				if (files.length === 0) return { refs: {}, dir: null, error: '添付がありません' }
				for (const f of files) if (!ATTACH_NAME.test(f.name)) throw new Error(`添付の名前が使えません: ${f.name}`)
				const dir = `memory:attach/${++attachSeq}`
				const refs: Record<string, string> = {}
				for (const f of files) {
					refs[f.name] = `${dir}/${f.name}`
					attachments.set(refs[f.name]!, f.data)
				}
				return { refs, dir, error: null }
			} catch (e) {
				return { refs: {}, dir: null, error: e instanceof Error ? e.message : String(e) }
			}
		},
		async writeDoc(relPath, text) {
			docs.set(safeRelPath(relPath), text)
		},
		async readDoc(relPath) {
			return docs.get(safeRelPath(relPath)) ?? null
		},
	}
}
