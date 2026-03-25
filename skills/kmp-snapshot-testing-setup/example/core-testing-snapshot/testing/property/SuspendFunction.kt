package com.example.snapshot.testing.property

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.duration
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.serialization.KSerializer
import com.example.snapshot.testing.snapshot.code.KotlinCodeFormat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * ランダムな suspend 関数を生成する Arb。
 * 生成された関数は yield / delay した後、成功時は [returns] の値を返し、
 * 失敗時は [throwable] の例外を throw する。
 *
 * - `yieldBefore`: scheduler 依存バグの検出
 * - `delay`: race condition の検出
 * - `Outcome`: success / failure を型安全に表現
 */
fun <Result> Arb.Companion.suspendFunction(
    returns: Arb<Result>,
    delayRange: ClosedRange<Duration> = Duration.ZERO..100.milliseconds,
    throwable: Arb<Throwable> = Arb.throwable(),
): Arb<FakeSuspendFunction<Result>> {
    val delayArb = Arb.duration(delayRange)

    val successArb = arbitrary<FakeSuspendFunction<Result>> {
        FakeSuspendFunction.success(
            delay = delayArb.bind(),
            yieldBefore = Arb.boolean().bind(),
            result = returns.bind(),
        )
    }

    val failureArb = arbitrary<FakeSuspendFunction<Result>> {
        FakeSuspendFunction.failure(
            delay = delayArb.bind(),
            yieldBefore = Arb.boolean().bind(),
            throwable = throwable.bind(),
        )
    }

    return Arb.choice(successArb, failureArb)
}

sealed interface FakeSuspendFunctionOutcome<out Result> {
    data class Success<Result>(val value: Result) : FakeSuspendFunctionOutcome<Result>
    data class Failure(val error: Throwable) : FakeSuspendFunctionOutcome<Nothing>
}

class FakeSuspendFunction<Result>(
    val delay: Duration,
    val yieldBefore: Boolean,
    val outcome: FakeSuspendFunctionOutcome<Result>,
) : suspend () -> Result {

    companion object {
        fun <Result> success(delay: Duration, yieldBefore: Boolean, result: Result) =
            FakeSuspendFunction(delay, yieldBefore, FakeSuspendFunctionOutcome.Success(result))

        fun <Result> failure(delay: Duration, yieldBefore: Boolean, throwable: Throwable) =
            FakeSuspendFunction<Result>(
                delay,
                yieldBefore,
                FakeSuspendFunctionOutcome.Failure(throwable)
            )
    }

    override suspend fun invoke(): Result {
        if (yieldBefore) yield()
        if (delay != Duration.ZERO) delay(delay)
        return when (outcome) {
            is FakeSuspendFunctionOutcome.Success -> outcome.value
            is FakeSuspendFunctionOutcome.Failure -> throw outcome.error
        }
    }

    fun toLabel(resultSerializer: KSerializer<Result>): String {
        val bodyPart = when (outcome) {
            is FakeSuspendFunctionOutcome.Failure ->
                "throw ${outcome.error::class.simpleName}(\"...\")"

            is FakeSuspendFunctionOutcome.Success ->
                KotlinCodeFormat.encodeToString(resultSerializer, outcome.value)
        }
        return buildString {
            appendLine("suspend {")
            if (yieldBefore) {
                appendLine("  yield()")
            }
            if (delay != Duration.ZERO) {
                appendLine("  delay($delay)")
            }
            bodyPart.lines().forEachIndexed { i, line ->
                if (i == 0) append("  $line") else append("\n  $line")
            }
            appendLine()
            append("}")
        }
    }
}

/**
 * [FakeSuspendFunction] を `suspend { yield(); delay(...); "RESULT" }` 形式のラベルに変換する。
 *
 * ```kotlin
 * Arb.suspendFunction(returns = Arb.list(Arb.string()))
 *     .withSuspendFunctionLabel { serializer() }
 * ```
 */
inline fun <reified Result> Arb<FakeSuspendFunction<Result>>.withSuspendFunctionLabel(): Arb<Pair<String, FakeSuspendFunction<Result>>> {
    val s = kotlinx.serialization.serializer<Result>()
    return map { value -> value.toLabel(s) to value }
}

/**
 * ランダムな [Throwable] を生成する Arb。
 * [RuntimeException], [IllegalStateException], [IllegalArgumentException] のいずれかを生成する。
 * stacktrace は無効化されており、PBT で大量生成しても GC 負荷が低い。
 */
fun Arb.Companion.throwable(
    message: Arb<String> = Arb.basicString(),
) = arbitrary<Throwable> {
    val message = message.map { "From Arb.throwable: $it" }.bind()
    val exceptionType = Arb.element(
        { msg: String -> RuntimeException(msg) },
        { msg: String -> IllegalStateException(msg) },
        { msg: String -> IllegalArgumentException(msg) },
    ).bind()
    exceptionType(message).apply { stackTrace = emptyArray() }
}

fun Arb.Companion.unit() = arbitrary<Unit> {}
