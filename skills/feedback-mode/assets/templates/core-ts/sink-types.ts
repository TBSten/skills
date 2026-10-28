// Sink（フィードバック置き場）の型の TypeScript 版。【実績: Web 参照実装】
// 契約は references/concepts.md「Sink の契約」と同じ。JS 版（../core/feedback-core.mjs の JSDoc）に、メモ機能が使う writeDoc / readDoc を足したもの。
// ファイル実装（file-sink.ts）とメモリ実装（memory-sink.ts）が同じ契約を満たすことを、sink-contract.test.ts（vitest）で確かめる。

export type EntryState = 'open' | 'archived'

export type EntryInfo = {
	/** `yymmdd-xx`。置き場の中で一意。名前にユーザーの入力を入れない。 */
	id: string
	/** 人・Agent が開ける参照。ファイル実装では repoRoot からの相対（repoRoot が無ければ置き場からの相対）。 */
	ref: string
	state: EntryState
	bytes: number
	createdAt: string
}

/** 添付の名前として使える形（パスの区切りや先頭の `.` を含めない）。 */
export const ATTACH_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]*$/

export type Attachment = { name: string; data: Uint8Array; mime?: string }

export type PutAttachmentsResult = {
	/** name → 参照。記録に書かれ、Agent が開く。 */
	refs: Record<string, string>
	/** まとめ先。無ければ null。 */
	dir: string | null
	/** 失敗の理由。投げずに返す（記録に「撮れませんでした」と書くため）。 */
	error: string | null
}

export type Sink = {
	kind: string
	capabilities: { put: boolean; list: boolean; read: boolean; archive: boolean; doc: boolean }
	/**
	 * 新しい記録を作る。上書きしない。番号は Inbox と archived の両方を数える。
	 * text が関数なら、採番した id を渡して文を作る（frontmatter に id を書くため）。
	 */
	putEntry(o: { text: string | ((id: string) => string); now?: Date }): Promise<EntryInfo & { attempts: number }>
	/** 既定は Inbox（置き場の直下）の記録だけ。`state` で archived や両方も選べる。id の昇順。 */
	list(o?: { state?: EntryState | 'all' }): Promise<EntryInfo[]>
	read(id: string): Promise<EntryInfo & { text: string }>
	/** archived に移し、note があれば末尾に対応メモを 1 行足す。2 回目は何もしない。 */
	archive(id: string, o?: { note?: string; now?: Date }): Promise<EntryInfo>
	/** 添付（スクリーンショットなど）を置く。失敗しても投げない。 */
	putAttachments(files: Attachment[], o?: { now?: Date }): Promise<PutAttachmentsResult>
	/** 置き場の中の文書を上書きする（メモ機能）。採番しない・記録に数えない。 */
	writeDoc(relPath: string, text: string): Promise<void>
	/** 無ければ null。 */
	readDoc(relPath: string): Promise<string | null>
}

/** 対応メモの追記。frontmatter は書き換えず、末尾に区切りと 1 行を足す。 */
export function archiveNoteText(note: string, now: Date): string {
	const day = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
	return `\n---\n対応メモ: ${note.replace(/\s+/g, ' ').trim()}（${day}）\n`
}
