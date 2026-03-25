package com.example.snapshot.testing.snapshot

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import com.example.snapshot.testing.snapshot.assertion.shouldMatchSnapshot
import com.example.snapshot.testing.snapshot.internal.RawSnapshotText

/**
 * PBT mode: ランダム入力 x アクション列を統合テスト。
 *
 * gen パラメータは `Gen<Pair<String, A>>` を受け取る。
 * String はスナップショットに記録される入力ラベル。
 *
 * ```kotlin
 * class MyTest : StateHolderSnapshotPbtSpec1<MySubject, suspend () -> String>(
 *     { Arb.suspendFunction(Arb.unit()).withLabel() },
 *     actions = {
 *         "doSomething" { doSomething() }
 *     },
 *     doSnapshot = { suspendFun ->
 *         val vm = createViewModel(suspendFun)
 *         stateFlow("uiState") { vm.uiState }
 *         vm // return Subject
 *     },
 * )
 * ```
 */

object SnapshotPBT : io.kotest.core.Tag()

// region 0 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec0<Subject>(
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.() -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(actionListArb, config) { actionIndices ->
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot()
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                emptyList()
            )
        }
    }
})

// endregion

// region 1 Arb

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec1<Subject, A>(
    genA: () -> Gen<Pair<String, A>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(gA, actionListArb, config) { pairA, actionIndices ->
            val (labelA, a) = pairA
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA)
            )
        }
    }
})

// endregion

// region 2 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec2<Subject, A, B>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(gA, gB, actionListArb, config) { pairA, pairB, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB)
            )
        }
    }
})

// endregion

// region 3 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec3<Subject, A, B, C>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, actionListArb, config) { pairA, pairB, pairC, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC)
            )
        }
    }
})

// endregion

// region 4 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec4<Subject, A, B, C, D>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC, labelD)
            )
        }
    }
})

// endregion

// region 5 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec5<Subject, A, B, C, D, E>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC, labelD, labelE)
            )
        }
    }
})

// endregion

// region 6 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec6<Subject, A, B, C, D, E, F>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC, labelD, labelE, labelF)
            )
        }
    }
})

// endregion

// region 7 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec7<Subject, A, B, C, D, E, F, G>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG)
            )
        }
    }
})

// endregion

// region 8 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec8<Subject, A, B, C, D, E, F, G, H>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH)
            )
        }
    }
})

// endregion

// region 9 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec9<Subject, A, B, C, D, E, F, G, H, I>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    genI: () -> Gen<Pair<String, I>>,
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI)
            )
        }
    }
})

// endregion

// region 10 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec10<Subject, A, B, C, D, E, F, G, H, I, J>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ
                )
            )
        }
    }
})

// endregion

// region 11 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec11<Subject, A, B, C, D, E, F, G, H, I, J, K>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK
                )
            )
        }
    }
})

// endregion

// region 12 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec12<Subject, A, B, C, D, E, F, G, H, I, J, K, L>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL
                )
            )
        }
    }
})

// endregion

// region 13 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec13<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM
                )
            )
        }
    }
})

// endregion

// region 14 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec14<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN
                )
            )
        }
    }
})

// endregion

// region 15 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec15<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val gO = genO()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            gO,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val (labelO, o) = pairO
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN,
                    labelO
                )
            )
        }
    }
})

// endregion

// region 16 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec16<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val gO = genO()
    val gP = genP()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            gO,
            gP,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val (labelO, o) = pairO
            val (labelP, p) = pairP
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN,
                    labelO,
                    labelP
                )
            )
        }
    }
})

// endregion

// region 17 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec17<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val gO = genO()
    val gP = genP()
    val gQ = genQ()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            gO,
            gP,
            gQ,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val (labelO, o) = pairO
            val (labelP, p) = pairP
            val (labelQ, q) = pairQ
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN,
                    labelO,
                    labelP,
                    labelQ
                )
            )
        }
    }
})

// endregion

// region 18 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec18<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val gO = genO()
    val gP = genP()
    val gQ = genQ()
    val gR = genR()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            gO,
            gP,
            gQ,
            gR,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, pairR, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val (labelO, o) = pairO
            val (labelP, p) = pairP
            val (labelQ, q) = pairQ
            val (labelR, r) = pairR
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN,
                    labelO,
                    labelP,
                    labelQ,
                    labelR
                )
            )
        }
    }
})

// endregion

// region 19 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec19<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val gO = genO()
    val gP = genP()
    val gQ = genQ()
    val gR = genR()
    val gS = genS()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            gO,
            gP,
            gQ,
            gR,
            gS,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, pairR, pairS, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val (labelO, o) = pairO
            val (labelP, p) = pairP
            val (labelQ, q) = pairQ
            val (labelR, r) = pairR
            val (labelS, s) = pairS
            val scope = SnapshotSpecScope(this)
            val subject = scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN,
                    labelO,
                    labelP,
                    labelQ,
                    labelR,
                    labelS
                )
            )
        }
    }
})

// endregion

// region 20 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class StateHolderSnapshotPbtSpec20<Subject, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T>(
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
    config: PropTestConfig = PropTestConfig(),
    actions: PbtActionScope<Subject>.() -> Unit,
    doSnapshot: SnapshotSpecScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) -> Subject,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()
    val gH = genH()
    val gI = genI()
    val gJ = genJ()
    val gK = genK()
    val gL = genL()
    val gM = genM()
    val gN = genN()
    val gO = genO()
    val gP = genP()
    val gQ = genQ()
    val gR = genR()
    val gS = genS()
    val gT = genT()
    val actionScope = PbtActionScope<Subject>().apply(actions)
    val actionNames = actionScope.actions.map { it.name }
    val actionListArb = actionListArb(actionNames.size)

    "on actions snapshot PBT" {
        checkAllSnapshot(
            gA,
            gB,
            gC,
            gD,
            gE,
            gF,
            gG,
            gH,
            gI,
            gJ,
            gK,
            gL,
            gM,
            gN,
            gO,
            gP,
            gQ,
            gR,
            gS,
            gT,
            actionListArb,
            config
        ) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, pairR, pairS, pairT, actionIndices ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val (labelJ, j) = pairJ
            val (labelK, k) = pairK
            val (labelL, l) = pairL
            val (labelM, m) = pairM
            val (labelN, n) = pairN
            val (labelO, o) = pairO
            val (labelP, p) = pairP
            val (labelQ, q) = pairQ
            val (labelR, r) = pairR
            val (labelS, s) = pairS
            val (labelT, t) = pairT
            val scope = SnapshotSpecScope(this)
            val subject =
                scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
            scope.executePbtIteration(
                this,
                subject,
                actionScope.actions,
                actionNames,
                actionIndices,
                fileBaseName,
                listOf(
                    labelA,
                    labelB,
                    labelC,
                    labelD,
                    labelE,
                    labelF,
                    labelG,
                    labelH,
                    labelI,
                    labelJ,
                    labelK,
                    labelL,
                    labelM,
                    labelN,
                    labelO,
                    labelP,
                    labelQ,
                    labelR,
                    labelS,
                    labelT
                )
            )
        }
    }
})

// endregion

// region Internal helpers

private fun actionListArb(actionCount: Int): Arb<List<Int>> =
    Arb.list(Arb.int(0 until actionCount), 0..maxOf(1, actionCount))

@Suppress("UNCHECKED_CAST")
private suspend fun <Subject> SnapshotSpecScope.executePbtIteration(
    testScope: CheckAllSnapshotScope,
    subject: Subject,
    pbtActions: List<PbtActionScope.NamedAction<Subject>>,
    actionNames: List<String>,
    actionIndices: List<Int>,
    fileNamePrefix: String,
    inputLabels: List<String>,
) {
    val wrappedActions = actionIndices.map { idx ->
        val pbtAction = pbtActions[idx]
        SnapshotSpecScope.Action(pbtAction.name) { pbtAction.action.invoke(subject) }
    }
    val doAction = when {
        wrappedActions.isEmpty() -> suspend {}
        wrappedActions.size == 1 -> onceAction(wrappedActions[0])
        else -> sequenceAction(wrappedActions)
    }
    val actionSequence = actionIndices.map { actionNames[it] }
    @OptIn(RawSnapshotText::class)
    testScope.shouldMatchSnapshot(
        fileName = "$fileNamePrefix/_inputs",
        text = PbtSnapshotInputs(
            inputs = inputLabels,
            actions = actionSequence,
        ).toSnapshotText(),
    )
    executeSnapshots(testScope, doAction, fileNamePrefix)
}

data class PbtSnapshotInputs<A>(
    val inputs: List<A>,
    val actions: List<String>,
) {
    fun toSnapshotText(): String = buildString {
        appendLine("PbtSnapshotInputs(")
        appendLine("  inputs = ${formatListOf(inputs, quoted = false)},")
        appendLine("  actions = ${formatListOf(actions, quoted = true)},")
        append(")")
    }
}

fun formatListOf(items: List<Any?>, quoted: Boolean, baseIndent: String = "  "): String {
    if (items.isEmpty()) return "emptyList()"
    val itemIndent = "$baseIndent  "
    return buildString {
        appendLine("listOf(")
        for (item in items) {
            val text = if (quoted) "\"$item\"" else "$item"
            val indented = text.lines().joinToString("\n") { line ->
                if (line.isEmpty()) line else "$itemIndent$line"
            }
            appendLine("$indented,")
        }
        append("$baseIndent)")
    }
}

// endregion
