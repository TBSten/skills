package com.example.snapshot.core

/**
 * デバッグ時のみメッセージを標準出力に出力する。
 *
 * CheckAllSnapshot / ShouldMatchSnapshot が警告ログ
 * (新規スナップショット検出・seed ファイル未作成など) に使用する。
 *
 * 有効化方法 (いずれか):
 * - システムプロパティ: `-Dsnapshot-test-debug=true`
 * - 環境変数: `SNAPSHOT_TEST_DEBUG=true`
 */
fun printOnlyDebug(message: String) {
    val enabled = System.getProperty("snapshot-test-debug")?.toBoolean() == true ||
        System.getenv("SNAPSHOT_TEST_DEBUG")?.toBoolean() == true
    if (enabled) {
        println(message)
    }
}
