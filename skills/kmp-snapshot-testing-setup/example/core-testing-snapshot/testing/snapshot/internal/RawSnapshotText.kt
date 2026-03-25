package com.example.snapshot.testing.snapshot.internal

/**
 * [shouldMatchSnapshot] の `text: String` オーバーロードの使用を制限するアノテーション。
 *
 * `.toString()` や `"$value"` による安易なスナップショット化を防ぐために、
 * `text: String` を直接渡すオーバーロードは OptIn が必要。
 * 代わりに serializer オーバーロードや、明示的なフォーマット関数の使用を推奨する。
 */
@RequiresOptIn(
    message = "shouldMatchSnapshot(text: String) は差分が見づらくなる可能性があります。" +
        "serializer オーバーロードの使用を検討してください。",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class RawSnapshotText
