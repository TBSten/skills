// 配置先: Domain 層 (例: domain/core/error/HandleWarning.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

/**
 * 警告 (エラーにはしないが検知したい不正な状態など) を文字列ベースで通知する interface。
 * エラー ([HandleError]) とは別に管理する。
 */
interface HandleWarning {
    /** 警告を通知する。 */
    operator fun invoke(vararg warnings: String)

    /** 複数のハンドラを合成する。合成後は両方に警告が通知される。 */
    operator fun plus(other: HandleWarning): HandleWarning = object : HandleWarning {
        override fun invoke(vararg warnings: String) {
            this@HandleWarning(*warnings)
            other(*warnings)
        }
    }
}
