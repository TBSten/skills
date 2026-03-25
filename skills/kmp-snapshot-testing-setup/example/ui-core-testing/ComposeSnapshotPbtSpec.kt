@file:OptIn(RawSnapshotText::class)

package com.example.snapshot.ui.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.TestScope
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import com.example.snapshot.testing.snapshot.PbtActionScope
import com.example.snapshot.testing.snapshot.PbtSnapshotInputs
import com.example.snapshot.testing.snapshot.SnapshotPBT
import com.example.snapshot.testing.snapshot.assertion.shouldMatchSnapshot
import com.example.snapshot.testing.snapshot.checkAllSnapshot
import com.example.snapshot.testing.snapshot.internal.RawSnapshotText

private data class ComposeSnapshotPbtEnvironment(
    val density: Density,
    val screenSize: DpSize,
    val theme: SystemTheme,
)

private val composeSnapshotPbtInternalArb: Arb<ComposeSnapshotPbtEnvironment> = Arb.bind(
    Arb.bind(
        Arb.element(0.1f, 0.8f, 1.0f, 1.5f, 2.5f),
        Arb.element(0.1f, 0.8f, 1.0f, 1.5f, 2.5f),
        ::Density,
    ),
    Arb.bind(
        Arb.element(250, 300, 500, 800, 1600),
        Arb.element(250, 300, 500, 800, 1600, 2000),
    ) { width, height -> DpSize(width.dp, height.dp) },
    Arb.enum<SystemTheme>(),
    ::ComposeSnapshotPbtEnvironment,
)
    .filter { (_, screenSize) -> screenSize.isSpecified }

private fun composeSnapshotPbtCombinedArb(
    actionCount: Int,
): Arb<Pair<ComposeSnapshotPbtEnvironment, List<Int>>> {
    val actionListArb = if (actionCount > 0) {
        Arb.list(Arb.int(0 until actionCount), 0..maxOf(1, actionCount))
    } else {
        Arb.constant(emptyList())
    }
    return Arb.bind(composeSnapshotPbtInternalArb, actionListArb) { env, actions ->
        Pair(env, actions)
    }
}

private fun composeSnapshotPbtFileNamePrefix(
    fileBaseName: String,
    environment: ComposeSnapshotPbtEnvironment,
): String =
    fileBaseName +
            "__density=${environment.density.density}" +
            "__fontScale=${environment.density.fontScale}" +
            "__size=${environment.screenSize.width}x${environment.screenSize.height}" +
            "__theme=${environment.theme}" +
            "/"

@OptIn(ExperimentalTestApi::class)
private suspend fun <Subject> executeActions(
    subject: Subject,
    actionScope: PbtActionScope<Subject>,
    actionIndices: List<Int>,
    composeUiTestAction: suspend () -> Unit,
) {
    for (idx in actionIndices) {
        actionScope.actions[idx].action.invoke(subject)
        composeUiTestAction()
    }
}

// region 0 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec0<Subject>(
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.() -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
        ) { (env, actionIndices) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = emptyList<Any?>(),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content()
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 1 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec1<Subject, A>(
    genA: () -> Gen<Pair<String, A>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
        ) { (env, actionIndices), (labelA, a) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 2 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec2<Subject, A, B>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
        ) { (env, actionIndices), (labelA, a), (labelB, b) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 3 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec3<Subject, A, B, C>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 4 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec4<Subject, A, B, C, D>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 5 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec5<Subject, A, B, C, D, E>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 6 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec6<Subject, A, B, C, D, E, F>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 7 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec7<Subject, A, B, C, D, E, F, G>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 8 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec8<Subject, A, B, C, D, E, F, G, H>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 9 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec9<Subject, A, B, C, D, E, F, G, H, I>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 10 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec10<Subject, A, B, C, D, E, F, G, H, I, J>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 11 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec11<Subject, A, B, C, D, E, F, G, H, I, J, K>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 12 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec12<Subject, A, B, C, D, E, F, G, H, I, J, K, L>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 13 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec13<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 14 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec14<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 15 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec15<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    genO: () -> Gen<Pair<String, O>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
            genO(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n), (labelO, o) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 16 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec16<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    genO: () -> Gen<Pair<String, O>>,
    genP: () -> Gen<Pair<String, P>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
            genO(),
            genP(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n), (labelO, o), (labelP, p) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 17 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec17<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    genO: () -> Gen<Pair<String, O>>,
    genP: () -> Gen<Pair<String, P>>,
    genQ: () -> Gen<Pair<String, Q>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
            genO(),
            genP(),
            genQ(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n), (labelO, o), (labelP, p), (labelQ, q) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 18 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec18<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    genO: () -> Gen<Pair<String, O>>,
    genP: () -> Gen<Pair<String, P>>,
    genQ: () -> Gen<Pair<String, Q>>,
    genR: () -> Gen<Pair<String, R>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
            genO(),
            genP(),
            genQ(),
            genR(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n), (labelO, o), (labelP, p), (labelQ, q), (labelR, r) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ, labelR),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 19 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec19<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    genO: () -> Gen<Pair<String, O>>,
    genP: () -> Gen<Pair<String, P>>,
    genQ: () -> Gen<Pair<String, Q>>,
    genR: () -> Gen<Pair<String, R>>,
    genS: () -> Gen<Pair<String, S>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
            genO(),
            genP(),
            genQ(),
            genR(),
            genS(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n), (labelO, o), (labelP, p), (labelQ, q), (labelR, r), (labelS, s) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ, labelR, labelS),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
                        }
                    }
                },
            )
        }
    }
})

// endregion
// region 20 Arbs

@OptIn(ExperimentalKotest::class, ExperimentalTestApi::class)
abstract class ComposeSnapshotPbtSpec20<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    genJ: () -> Gen<Pair<String, J>>,
    genK: () -> Gen<Pair<String, K>>,
    genL: () -> Gen<Pair<String, L>>,
    genM: () -> Gen<Pair<String, M>>,
    genN: () -> Gen<Pair<String, N>>,
    genO: () -> Gen<Pair<String, O>>,
    genP: () -> Gen<Pair<String, P>>,
    genQ: () -> Gen<Pair<String, Q>>,
    genR: () -> Gen<Pair<String, R>>,
    genS: () -> Gen<Pair<String, S>>,
    genT: () -> Gen<Pair<String, T>>,
    actions: PbtActionScope<Subject>.() -> Unit = {},
    content: @Composable TestScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }

    "compose snapshot PBT" {
        checkAllSnapshot(
            composeSnapshotPbtCombinedArb(actionScope.actions.size),
            genA(),
            genB(),
            genC(),
            genD(),
            genE(),
            genF(),
            genG(),
            genH(),
            genI(),
            genJ(),
            genK(),
            genL(),
            genM(),
            genN(),
            genO(),
            genP(),
            genQ(),
            genR(),
            genS(),
            genT(),
        ) { (env, actionIndices), (labelA, a), (labelB, b), (labelC, c), (labelD, d), (labelE, e), (labelF, f), (labelG, g), (labelH, h), (labelI, i), (labelJ, j), (labelK, k), (labelL, l), (labelM, m), (labelN, n), (labelO, o), (labelP, p), (labelQ, q), (labelR, r), (labelS, s), (labelT, t) ->
            val actionSequence = actionIndices.map { actionNames[it] }
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ, labelR, labelS, labelT),
                    actions = actionSequence,
                ).toSnapshotText(),
            )
            var subject: Subject? = null
            runComposableSnapshotTest(
                fileNamePrefix = composeSnapshotPbtFileNamePrefix(
                    fileBaseName,
                    env,
                ),
                action = {
                    executeActions(subject!!, actionScope, actionIndices) { waitForIdle() }
                },
                content = {
                    Box(Modifier.size(env.screenSize)) {
                        CompositionLocalProvider(LocalDensity provides env.density) {
                            subject = content(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
                        }
                    }
                },
            )
        }
    }
})

// endregion
