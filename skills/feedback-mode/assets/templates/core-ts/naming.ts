// 記録の名前（id）と採番の規則。DOM も node: も使わない純粋な関数だけ。【実績】
// 名前は `yymmdd-xx`（連番は SEQ_DIGITS 桁にそろえる）。数えるときは 2 桁以上を全部読むので、後から桁を増やしても前の記録と重ならない。

/** 連番の桁数。1 日に 100 件を超えうるなら 3 にする（Web 参照実装は 3）。 */
export const SEQ_DIGITS = 2

/** 1 日に採番できる上限。 */
export const MAX_PER_DAY = 999

/** 置き場（Inbox）の下の、対応済みの記録の置き場。 */
export const ARCHIVE_DIR = 'archived'

/** id の形。2 桁以上の連番を許す。 */
export const ID_PATTERN = /^(\d{6})-(\d{2,})$/

/** 記録のファイル名の形。`archived/` や `memo/`・`HISTORY.md` などは当てはまらない。 */
export const ENTRY_FILE_PATTERN = /^(\d{6}-\d{2,})\.md$/

/** `yymmdd`。日付は受け口のローカル時刻で決める。 */
export function datePrefix(now: Date): string {
	const two = (n: number) => String(n).padStart(2, '0')
	return `${two(now.getFullYear() % 100)}${two(now.getMonth() + 1)}${two(now.getDate())}`
}

/** id として使える形か。パスの区切りや `..` を含められないので、読み書きの前の検査に使う。 */
export function isEntryId(id: unknown): id is string {
	return typeof id === 'string' && ID_PATTERN.test(id)
}

export function formatId(prefix: string, n: number): string {
	return `${prefix}-${String(n).padStart(SEQ_DIGITS, '0')}`
}

/** ファイル名が記録なら id を返す。そうでなければ null。 */
export function idOfFileName(name: string): string | null {
	return ENTRY_FILE_PATTERN.exec(name)?.[1] ?? null
}

/**
 * その日の番号のうち最大のもの。無ければ 0。
 * names は Inbox と `archived/` の両方の名前（拡張子は問わない。空ファイルも数える）。
 * `archived/` に移した番号を数えないと、次の保存が同じ番号を再利用してしまう。
 */
export function lastNumber(names: Iterable<string>, prefix: string): number {
	let max = 0
	for (const name of names) {
		const m = /^(\d{6})-(\d{2,})(?:\.[^/]*)?$/.exec(name)
		if (m && m[1] === prefix) max = Math.max(max, Number(m[2]))
	}
	return max
}

/** 次の番号。上限を超えるなら null。 */
export function nextNumber(names: Iterable<string>, prefix: string): number | null {
	const n = lastNumber(names, prefix) + 1
	return n > MAX_PER_DAY ? null : n
}
