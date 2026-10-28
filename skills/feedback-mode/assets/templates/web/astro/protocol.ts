// toolbar アプリ（ブラウザ）と統合（サーバー）が共有する名前。両側から import するので、node: やブラウザ専用の API を書かない。
// 本番の dist に出ていないことの検査（check-no-dev-tools-in-dist.sh）も、ここの文字列で grep する。変えたら script も直す。

/** toolbar アプリの id。イベント名の接頭辞にも使う。 */
export const APP_ID = 'feedback-mode'
/** ブラウザ → サーバー: 保存の依頼。 */
export const EVENT_SAVE = 'feedback-mode:save'
/** サーバー → ブラウザ: 保存の結果。requestId で依頼と対応づける。 */
export const EVENT_RESULT = 'feedback-mode:result'
/** head-inline のログのバッファを置く window のプロパティ名。 */
export const LOG_GLOBAL = '__feedbackLog'

/** 受け口が受け取る大きさの上限（`astro dev --host` で LAN に出したときの守りにもなる）。 */
export const LIMITS = { commentChars: 20_000, payloadChars: 2_000_000, logEntries: 500, logChars: 1_000 } as const

export type SaveRequest = {
	requestId: string
	/** assets/templates/feedback.schema.json の payload（appName・pointer はサーバーが足す）。 */
	payload: Record<string, unknown> & { comment: string }
	options?: Record<string, boolean>
}
export type SaveResult = { requestId: string; ok: true; ref: string } | { requestId: string; ok: false; error: string }
