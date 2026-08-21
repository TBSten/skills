// 配置先: UI 層 (例: ui/core/error/HandleErrorDefault.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

import androidx.compose.runtime.mutableStateListOf

/**
 * [HandleError] のデフォルト実装。
 * エラーを [mutableStateListOf] で保持し、Compose UI (エラーダイアログ・Snackbar 等) から
 * 観測できるようにする。
 */
class HandleErrorDefault : HandleError {
    private val mutableErrors = mutableStateListOf<Throwable>()

    /** UI が観測する未処理エラーの一覧。 */
    val errors: List<Throwable> get() = mutableErrors

    override fun handle(exception: Throwable) {
        // TODO: 必要ならここでログ出力等を追加する
        mutableErrors.add(exception)
    }

    /** ユーザーへの表示が終わったエラーを取り除く。 */
    fun consume(error: Throwable) {
        mutableErrors.remove(error)
    }
}
