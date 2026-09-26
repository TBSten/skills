package com.example.kotlinlibrarybootup

/**
 * ライブラリのモジュール間でだけ使う API を示すマーカー。利用者が使うことは想定しない。
 *
 * 自モジュールは build-logic の convention で opt-in 済み。
 * binary-compatibility-validator の対象外 (root build.gradle.kts の nonPublicMarkers)。
 */
@RequiresOptIn(
    message = "This is an internal ExampleLib API. It may be changed or removed without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class InternalExampleLibApi
