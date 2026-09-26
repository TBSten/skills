package com.example.kotlinlibrarybootup

/**
 * 実験的な API を示すマーカー。今後のリリースで互換性なく変更・削除され得る。
 *
 * 利用側は `@OptIn(ExperimentalExampleLibApi::class)` で opt-in する。
 * binary-compatibility-validator の対象外 (root build.gradle.kts の nonPublicMarkers)。
 */
@RequiresOptIn(
    message = "This API is experimental. It may be changed or removed in the future.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class ExperimentalExampleLibApi
