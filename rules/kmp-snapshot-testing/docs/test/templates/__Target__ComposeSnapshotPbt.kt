// 配置先: jvmSnapshotTest ソースセット
// TODO: package をプロジェクトに合わせて置換し、__Target__ をテスト対象名に置換する
package com.example.snapshottest

// TODO: import を追加する (ComposeSnapshotPbtSpec*, Arb 拡張, テスト対象 Composable, Fake 等)

/**
 * __Target__ (Compose UI) の PBT スナップショットテスト。
 * Density (0.1x〜2.5x)・ScreenSize (250dp〜1600dp)・SystemTheme (Light/Dark) の組み合わせと
 * PBT 入力で網羅テストし、スクリーンショット PNG とセマンティクス木を記録する。
 *
 * docs/test/compose-snapshot-test.md「Compose PBT スナップショットテスト」参照。
 * - Arb が N 個なら ComposeSnapshotPbtSpecN (0〜20) を使う
 * - 各 Arb は `Gen<Pair<String, A>>` 形式 (ラベル + 値) で渡す
 * - 必要なら actions { } でアクション列も定義できる
 */
class __Target__ComposeSnapshotPbt : ComposeSnapshotPbtSpec1<Unit, suspend () -> __Target__Data>(
    // TODO: テスト対象の入力に合わせて Arb を差し替える
    genA = { Arb.suspendFunction(returns = Arb.__target__Data()).withSuspendFunctionLabel() },
    content = { get__Target__Data ->
        // TODO: テスト対象の Composable を Fake 依存と組み合わせて構築する
        __Target__Screen(
            viewModel = remember {
                __Target__ViewModel(
                    __target__Loader = object :
                        __Target__Loader by SimpleLoaderFactory.Default.create(
                            this,
                            load = { get__Target__Data() },
                        ) {},
                )
            },
        )
    },
)
