// 配置先: jvmSnapshotTest ソースセット
// TODO: package をプロジェクトに合わせて置換し、__Target__ をテスト対象名に置換する
package com.example.snapshottest

// TODO: import を追加する (LogicSnapshotPbtSpec*, Arb 拡張, テスト対象)

/**
 * __Target__ (StateHolder でも Compose でもないロジック・関数) の PBT スナップショットテスト。
 * UseCase・Cache・ユーティリティ関数などの純粋なロジックの出力をランダム入力でスナップショットする。
 *
 * docs/test/snapshot-test.md「ロジック PBT スナップショットテスト」参照。
 * - Arb が N 個なら LogicSnapshotPbtSpecN (1〜20) を使う
 * - 各 Arb は `Gen<Pair<String, A>>` 形式 (ラベル + 値) で渡す
 * - doSnapshot は suspend — suspend 関数を直接呼び出せる
 * - アクション列 (actions) はなし — stateless なロジック向け
 */
class __Target__LogicPbtSnapshotTest : LogicSnapshotPbtSpec1<String>(
    // TODO: テスト対象の入力に合わせて Arb を差し替える (ラベル付き)
    { Arb.basicString().withLabel { it } },
    doSnapshot = { input ->
        // TODO: テスト対象のロジックを呼び出し、出力を output で登録する
        val result = runCatching { __Target__Impl()(input) }
        output("result") { result }
    },
)
