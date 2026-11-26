// =============================================================================
// annotations モジュール: src/commonMain/kotlin/{package}/Collectable.kt
// =============================================================================
//
// テンプレート変数:
//   {{PACKAGE}}          - ユーザー指定のパッケージ名 (例: com.example.collector)
//   {{COLLECTABLE_NAME}} - マーカーアノテーション名 (デフォルト: Collectable)
//   {{COLLECT_ALL_NAME}} - 収集結果アノテーション名 (デフォルト: CollectAll)

package {{PACKAGE}}

import kotlin.reflect.KClass

/**
 * このアノテーションが付与されたクラスは、コンパイル時にモジュール横断で収集される。
 *
 * @param tag グルーピング用のタグ。同じ tag のクラスがまとめて収集される。
 *            未指定の場合は "default" として扱われる。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class {{COLLECTABLE_NAME}}(
    val tag: String = "",
)

/**
 * この関数またはプロパティに付与すると、コンパイラが body を生成し、
 * [{{COLLECTABLE_NAME}}] が付与された全クラスの [KClass] リストを返す。
 *
 * 使用例:
 * ```kotlin
 * @{{COLLECT_ALL_NAME}}(tag = "handler")
 * fun allHandlers(): List<KClass<*>>
 * ```
 *
 * コンパイラが以下のように body を生成する:
 * ```kotlin
 * fun allHandlers(): List<KClass<*>> = listOf(HandlerA::class, HandlerB::class)
 * ```
 *
 * @param tag 収集対象の tag。[{{COLLECTABLE_NAME}}] の tag と一致するクラスのみ収集する。
 *            未指定の場合は "default" として扱われる。
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class {{COLLECT_ALL_NAME}}(
    val tag: String = "",
)
