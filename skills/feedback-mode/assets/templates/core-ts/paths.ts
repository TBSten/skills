// 置き場のルートの中だけを指す相対パスの検査。純粋な文字列の処理だけ（シンボリックリンクの確認は file-sink.ts がする）。

/**
 * `relPath` を `/` 区切りの正規形にして返す。ルートの外に出るもの（絶対パス・`..`・空・NUL・`\`）は投げる。
 * `./a//b` → `a/b`
 */
export function safeRelPath(relPath: unknown): string {
	if (typeof relPath !== 'string' || relPath === '' || relPath.includes('\0') || relPath.includes('\\')) {
		throw new Error(`パスが使えません: ${JSON.stringify(relPath)}`)
	}
	if (relPath.startsWith('/') || /^[A-Za-z]:/.test(relPath)) throw new Error(`絶対パスは使えません: ${relPath}`)
	const parts = relPath.split('/').filter((p) => p !== '' && p !== '.')
	if (parts.length === 0) throw new Error(`パスが空です: ${relPath}`)
	if (parts.includes('..')) throw new Error(`.. で置き場の外には出せません: ${relPath}`)
	return parts.join('/')
}
