// 配置先: jvmSnapshotTest ソースセット
// TODO: package をプロジェクトに合わせて置換し、__Target__ をテスト対象名に置換する
package com.example.snapshottest

// TODO: import を追加する (StateHolderSnapshotPbtSpec*, Arb 拡張, テスト対象, Fake 等)

/**
 * __Target__ (StateHolder / ViewModel) の PBT スナップショットテスト。
 * ランダムな入力 x アクション列の組み合わせで状態遷移を網羅テストする。
 *
 * docs/test/snapshot-test.md「StateHolder/ViewModel PBT スナップショットテスト」参照。
 * - Arb が N 個なら StateHolderSnapshotPbtSpecN (0〜20) を使う
 * - 各 Arb は `Gen<Pair<String, A>>` 形式 (ラベル + 値) で渡す
 */
class __Target__PbtSnapshotTest :
    StateHolderSnapshotPbtSpec1<__Target__ViewModel, suspend () -> List<__Target__Data>>(
        // 1. 入力値の Arb を作成 (ラベル付き)
        {
            // TODO: テスト対象の入力に合わせて Arb を差し替える
            Arb.suspendFunction(returns = Arb.list(Arb.__target__Data()))
                .withSuspendFunctionLabel()
        },
        // 2. テスト対象の公開メソッドを action として登録
        actions = {
            // TODO: テスト対象の公開メソッドを列挙する
            "reloadInitial" { reloadInitial() }
            "refresh" { refresh() }
        },
        // 3. doSnapshot でテスト対象を構築し、状態を登録
        doSnapshot = { loadFn ->
            // TODO: テスト対象を Fake / .forTest() ラッパーと組み合わせて構築する
            val loader = __Target__LoaderImpl(
                __target__UseCase = { loadFn() },
                simpleLoaderFactory = SimpleLoaderFactory.Default,
            ).forTest() // テスト用ラッパー (ログ記録・不正遷移検出)
            val handleError = HandleErrorForTest()

            __Target__ViewModel(
                __target__Loader = loader,
                handleError = handleError,
            ).also {
                // StateFlow は stateFlow で登録 (Turbine 経由で全遷移を記録)
                stateFlow("loadState") { it.loadState }
                // 非 Flow の値は state で登録 (アクション実行後のスナップショット)
                state("logEntries") { loader.logEntries.toList() }
                state("illegalStateTransitions") { loader.illegalStateTransitions.toList() }
                state("handleError.errors") { handleError.errors }
            }
        },
    )
