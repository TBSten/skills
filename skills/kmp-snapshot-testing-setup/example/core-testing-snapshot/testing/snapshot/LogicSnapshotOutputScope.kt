package com.example.snapshot.testing.snapshot

import kotlinx.coroutines.CoroutineScope

/**
 * [LogicSnapshotPbtSpec] 用の出力登録スコープ。
 *
 * [StateHolderSnapshotPbtSpec] が `state()` で状態を登録するのに対し、
 * ロジックテストでは `output()` で出力値を登録する。
 *
 * - デフォルト name は `"output"`
 * - 同じ name を複数回登録するとエラー
 */
class LogicSnapshotOutputScope(
    coroutineScope: CoroutineScope,
) {
    @PublishedApi
    internal val delegate = SnapshotSpecScope(coroutineScope)

    @PublishedApi
    internal val registeredNames = mutableSetOf<String>()

    /**
     * ロジックの出力値をスナップショットに登録する。
     *
     * @param name 出力名。デフォルトは `"output"`。複数出力がある場合は名前で区別する。
     * @param value 出力値を返すラムダ。
     */
    inline fun <reified S> output(name: String = "output", noinline value: () -> S) {
        require(registeredNames.add(name)) {
            "output name \"$name\" is already registered. Use a unique name for each output."
        }
        delegate.state(name, value)
    }
}
