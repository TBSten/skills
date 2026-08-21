// 配置先: テストコード (例: core/testing/error/HandleErrorForTest.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

/**
 * テスト用の [HandleError] Fake 実装。
 * handle されたエラーを記録し、テストから検証できるようにする。
 */
class HandleErrorForTest : HandleError {
    val errors = mutableListOf<Throwable>()

    override fun handle(exception: Throwable) {
        errors.add(exception)
    }
}

/**
 * テスト用の [HandleWarning] Fake 実装。
 * 通知された警告を記録し、テストから検証できるようにする。
 */
class HandleWarningForTest : HandleWarning {
    val warnings = mutableListOf<List<String>>()

    override fun invoke(vararg warnings: String) {
        this@HandleWarningForTest.warnings.add(warnings.toList())
    }
}
