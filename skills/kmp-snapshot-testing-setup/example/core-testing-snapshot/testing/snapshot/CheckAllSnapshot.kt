package com.example.snapshot.testing.snapshot

import io.kotest.common.ExperimentalKotest
import io.kotest.core.test.TestScope
import io.kotest.property.Gen
import io.kotest.property.PropTestConfig
import io.kotest.property.checkAll
import com.example.snapshot.core.printOnlyDebug
import com.example.snapshot.testing.snapshot.internal.SnapshotRegistry
import kotlin.random.Random

class CheckAllSnapshotScope(
    private val testScope: TestScope,
    private val iterationIndex: Int,
) : TestScope by testScope {
    val fileBaseName: String = "case_${iterationIndex.toString().padStart(4, '0')}"
}

// region 1 arb

@OptIn(ExperimentalKotest::class)
suspend fun <A> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA) { a ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 2 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA, genB) { a, b ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 3 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA, genB, genC) { a, b, c ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 4 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA, genB, genC, genD) { a, b, c, d ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c, d)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 5 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA, genB, genC, genD, genE) { a, b, c, d, e ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c, d, e)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 6 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA, genB, genC, genD, genE, genF) { a, b, c, d, e, f ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c, d, e, f)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 7 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(resolvedConfig, genA, genB, genC, genD, genE, genF, genG) { a, b, c, d, e, f, g ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c, d, e, f, g)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 8 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH
        ) { a, b, c, d, e, f, g, h ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c, d, e, f, g, h)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 9 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI
        ) { a, b, c, d, e, f, g, h, i ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(a, b, c, d, e, f, g, h, i)
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 10 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ
        ) { a, b, c, d, e, f, g, h, i, j ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 11 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK
        ) { a, b, c, d, e, f, g, h, i, j, k ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 12 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL
        ) { a, b, c, d, e, f, g, h, i, j, k, l ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 13 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 14 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 15 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 16 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    genP: Gen<P>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO,
            genP
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o,
                p
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 17 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    genP: Gen<P>,
    genQ: Gen<Q>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO,
            genP,
            genQ
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o,
                p,
                q
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 18 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    genP: Gen<P>,
    genQ: Gen<Q>,
    genR: Gen<R>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO,
            genP,
            genQ,
            genR
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o,
                p,
                q,
                r
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 19 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    genP: Gen<P>,
    genQ: Gen<Q>,
    genR: Gen<R>,
    genS: Gen<S>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO,
            genP,
            genQ,
            genR,
            genS
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o,
                p,
                q,
                r,
                s
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 20 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    genP: Gen<P>,
    genQ: Gen<Q>,
    genR: Gen<R>,
    genS: Gen<S>,
    genT: Gen<T>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO,
            genP,
            genQ,
            genR,
            genS,
            genT
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o,
                p,
                q,
                r,
                s,
                t
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
// region 21 arbs

@OptIn(ExperimentalKotest::class)
suspend fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U> TestScope.checkAllSnapshot(
    genA: Gen<A>,
    genB: Gen<B>,
    genC: Gen<C>,
    genD: Gen<D>,
    genE: Gen<E>,
    genF: Gen<F>,
    genG: Gen<G>,
    genH: Gen<H>,
    genI: Gen<I>,
    genJ: Gen<J>,
    genK: Gen<K>,
    genL: Gen<L>,
    genM: Gen<M>,
    genN: Gen<N>,
    genO: Gen<O>,
    genP: Gen<P>,
    genQ: Gen<Q>,
    genR: Gen<R>,
    genS: Gen<S>,
    genT: Gen<T>,
    genU: Gen<U>,
    config: PropTestConfig = PropTestConfig(),
    block: suspend CheckAllSnapshotScope.(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U) -> Unit,
) {
    val resolvedConfig = resolveSnapshotConfig(config)
    var index = 0
    try {
        checkAll(
            resolvedConfig,
            genA,
            genB,
            genC,
            genD,
            genE,
            genF,
            genG,
            genH,
            genI,
            genJ,
            genK,
            genL,
            genM,
            genN,
            genO,
            genP,
            genQ,
            genR,
            genS,
            genT,
            genU
        ) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u ->
            CheckAllSnapshotScope(this@checkAllSnapshot, index++).block(
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                j,
                k,
                l,
                m,
                n,
                o,
                p,
                q,
                r,
                s,
                t,
                u
            )
        }
    } finally {
        generatePbtSnapshotReport(index)
    }
}

// endregion
@OptIn(ExperimentalKotest::class)
private fun TestScope.resolveSnapshotConfig(config: PropTestConfig): PropTestConfig {
    val snapshotPath = buildSnapshotPath()
    val seed = when (snapshotTestFlavor()) {
        SnapshotTestFlavor.Record -> Random.nextLong().also { saveSeed(snapshotPath, it) }
        SnapshotTestFlavor.Verify -> loadSeed(snapshotPath)
    }
    return config.copy(seed = seed)
}

private const val seedFileName = "_snapshot_pbt_seed.txt"
private fun saveSeed(snapshotPath: String, seed: Long) {
    val file = snapshotDir(snapshotPath).resolve(seedFileName)
    file.parentFile.mkdirs()
    file.writeText(seed.toString())
}

private fun loadSeed(snapshotPath: String): Long {
    val file = snapshotDir(snapshotPath).resolve(seedFileName)
    SnapshotRegistry.markUsed(file)
    if (!file.exists()) {
        // seed 未作成 = 新規テスト。ランダム seed で続行し、PBT が全 case 走るようにする
        val fallbackSeed = Random.nextLong()
        printOnlyDebug("[Snapshot] WARNING: seed ファイル未作成のためランダム seed を使用: ${file.absolutePath} (seed=$fallbackSeed)")
        return fallbackSeed
    }
    return file.readText().trim().toLong()
}

