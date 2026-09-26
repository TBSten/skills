# まだ入れていないもの (拡張の手がかり)

v1 の scaffold には含めていない。必要になったら生成後のプロジェクトに足す。

| 項目 | 状態 | 足す時の手がかり |
|---|---|---|
| Compose Multiplatform | 未対応 | Compose 用の convention (`<lib>.kmp.compose`) を別に作る。Compose UI は旧 `js` ターゲットに非対応なので wasmJs のみにする。Hot Reload 用の dev モジュールも一緒に置くと確認が速い (出典: koma-strict の `koma.strict.kmp.compose`、compose-preview-lab の CmpConventionPlugin) |
| kover (カバレッジ) | 未対応 | `org.jetbrains.kotlinx.kover` をルートと各モジュールに適用し、CI に `koverXmlReport` を足す |
| Konsist (アーキテクチャテスト) | 未対応 | JVM の test モジュールを 1 つ足し、パッケージ依存・命名・公開範囲をテストで固定する。モジュールが増えてから入れる価値が出る (出典: ksp-plugin-setup の ArchTest) |
| context7.json | 未対応 | context7 への登録に公開鍵が要るため scaffold では作らない。README で登録手順を案内する |
| 複数の Kotlin 版での互換テスト | 未対応 | 床より新しい版でも利用側がビルドできるかを CI matrix で確かめる。compiler plugin 向けの仕組みは kotlin-compiler-plugin-setup にある |

## scaffold.sh の既知の制限

- `--kotlin-version` を変えても、kotest・AGP と README の「Kotlin 2.4 以上」(JS / Wasm / Native の下限) は
  追従しない。README の下限は手で直す (KSP の扱いは SKILL.md の確認事項)
- full 構成のテスト全体 (watchOS / tvOS シミュレータ) と publish は、ローカル検証をしていない
