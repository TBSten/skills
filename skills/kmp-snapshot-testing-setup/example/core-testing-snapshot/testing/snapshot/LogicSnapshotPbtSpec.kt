package com.example.snapshot.testing.snapshot

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Gen
import io.kotest.property.PropTestConfig
import com.example.snapshot.testing.snapshot.assertion.shouldMatchSnapshot
import com.example.snapshot.testing.snapshot.internal.RawSnapshotText

/**
 * PBT mode for logic/data that is neither StateHolder nor Compose.
 * ランダムな入力を受け取り、ロジック・関数の出力をスナップショットする。
 *
 * [StateHolderSnapshotPbtSpec] と異なり、アクション列は使用しない。
 * UseCase・Cache・ユーティリティ関数などの純粋なロジックのテストに使う。
 *
 * [doSnapshot] は `suspend` なので、suspend 関数を直接呼び出せる。
 * 出力は [LogicSnapshotOutputScope.output] で登録する。
 *
 * ```kotlin
 * class MyUseCasePbtSnapshotTest : LogicSnapshotPbtSpec1<String>(
 *     { Arb.basicString().withLabel { it } },
 *     doSnapshot = { input ->
 *         val result = runCatching { MyUseCaseImpl()(input) }
 *         output("result") { result }
 *     },
 * )
 * ```
 */

// region 1 Arb

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec1<A>(
    genA: () -> Gen<Pair<String, A>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, config) { pairA ->
            val (labelA, a) = pairA
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 2 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec2<A, B>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, config) { pairA, pairB ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 3 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec3<A, B, C>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, config) { pairA, pairB, pairC ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 4 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec4<A, B, C, D>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, config) { pairA, pairB, pairC, pairD ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 5 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec5<A, B, C, D, E>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, config) { pairA, pairB, pairC, pairD, pairE ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 6 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec6<A, B, C, D, E, F>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, config) { pairA, pairB, pairC, pairD, pairE, pairF ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 7 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec7<A, B, C, D, E, F, G>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G) -> Unit,
) : FreeSpec({
    tags(SnapshotPBT)

    val gA = genA()
    val gB = genB()
    val gC = genC()
    val gD = genD()
    val gE = genE()
    val gF = genF()
    val gG = genG()

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 8 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec8<A, B, C, D, E, F, G, H>(
    genA: () -> Gen<Pair<String, A>>,
    genB: () -> Gen<Pair<String, B>>,
    genC: () -> Gen<Pair<String, C>>,
    genD: () -> Gen<Pair<String, D>>,
    genE: () -> Gen<Pair<String, E>>,
    genF: () -> Gen<Pair<String, F>>,
    genG: () -> Gen<Pair<String, G>>,
    genH: () -> Gen<Pair<String, H>>,
    config: PropTestConfig = PropTestConfig(),
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 9 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec9<A, B, C, D, E, F, G, H, I>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI ->
            val (labelA, a) = pairA
            val (labelB, b) = pairB
            val (labelC, c) = pairC
            val (labelD, d) = pairD
            val (labelE, e) = pairE
            val (labelF, f) = pairF
            val (labelG, g) = pairG
            val (labelH, h) = pairH
            val (labelI, i) = pairI
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 10 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec10<A, B, C, D, E, F, G, H, I, J>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 11 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec11<A, B, C, D, E, F, G, H, I, J, K>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 12 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec12<A, B, C, D, E, F, G, H, I, J, K, L>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 13 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec13<A, B, C, D, E, F, G, H, I, J, K, L, M>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 14 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec14<A, B, C, D, E, F, G, H, I, J, K, L, M, N>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 15 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, gO, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 16 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, gO, gP, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 17 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, gO, gP, gQ, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 18 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, gO, gP, gQ, gR, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, pairR ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ, labelR),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 19 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, gO, gP, gQ, gR, gS, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, pairR, pairS ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ, labelR, labelS),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion

// region 20 Arbs

@OptIn(io.kotest.common.ExperimentalKotest::class)
abstract class LogicSnapshotPbtSpec20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T>(
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
    doSnapshot: suspend LogicSnapshotOutputScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) -> Unit,
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

    "on inputs snapshot PBT" {
        checkAllSnapshot(gA, gB, gC, gD, gE, gF, gG, gH, gI, gJ, gK, gL, gM, gN, gO, gP, gQ, gR, gS, gT, config) { pairA, pairB, pairC, pairD, pairE, pairF, pairG, pairH, pairI, pairJ, pairK, pairL, pairM, pairN, pairO, pairP, pairQ, pairR, pairS, pairT ->
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
            val scope = LogicSnapshotOutputScope(this)
            scope.doSnapshot(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
            @OptIn(RawSnapshotText::class)
            shouldMatchSnapshot(
                fileName = "$fileBaseName/_inputs",
                text = PbtSnapshotInputs(
                    inputs = listOf(labelA, labelB, labelC, labelD, labelE, labelF, labelG, labelH, labelI, labelJ, labelK, labelL, labelM, labelN, labelO, labelP, labelQ, labelR, labelS, labelT),
                    actions = emptyList(),
                ).toSnapshotText(),
            )
            scope.delegate.executeSnapshots(this, doAction = {}, fileBaseName)
        }
    }
})

// endregion
