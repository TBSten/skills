# ターゲットの選び方

`--targets` で生成物が変わる。迷ったら standard。

| 値 | ターゲット | 向いているライブラリ |
|---|---|---|
| `standard` (既定) | android / jvm / js / wasmJs / iosArm64 / iosSimulatorArm64 | 多くの KMP ライブラリ。Compose Multiplatform の利用者がいるターゲットをひと通り覆う |
| `full` | standard + macosArm64 / watchos / tvos / linuxX64・Arm64 / mingwX64 / androidNative / wasmWasi | 純粋なロジックだけで、依存ライブラリもすべてのターゲットに対応しているもの (コレクション・パーサ等) |
| `jvm` | `kotlin("jvm")` のみ | Gradle / IntelliJ / サーバ向け、JVM 専用 API に依存するもの |

選ぶ時に確認すること:

- **依存したいライブラリが対応しているターゲット**。1 つでも未対応なら、そのターゲットは落とす
- **利用者**。Android アプリ向けなら standard、JVM ツール向けなら jvm
- **CI 時間**。full は native のクロスコンパイルが増える。テストするのは linuxX64 と macosArm64 だけで、
  他はコンパイルのみ (ci.yml のコメント参照)

後からターゲットを足すなら `build-logic/src/main/kotlin/Targets.kt` を直し、ci.yml の matrix と
`./gradlew apiDump` (klib dump の対象が変わる) も合わせる。jvm → KMP の切り替えは scaffold し直した方が早い。

## Apple ターゲットの有効化

Xcode が無い環境 (CommandLineTools だけの macOS) では Apple ターゲットの link が `xcrun exit code 72` で
落ちる。そのため `Targets.kt` の `appleTargetsEnabled()` で次のように決める。

| 条件 | Apple ターゲット |
|---|---|
| `-PenableAppleTargets=true` | 常に有効 (publish.yml はこれを付けて Xcode 必須を明示する) |
| `-PdisableAppleTargets=true` | 常に無効 |
| macOS で `xcode-select -p` が `*.app` を指す | 有効 |
| macOS で CommandLineTools だけ | 無効 |
| Linux / Windows | 有効 (Kotlin Gradle Plugin がホスト非対応のタスクを自動で skip し、`kotlin.native.ignoreDisabledTargets` で警告も出さない) |

Apple 向けの artifact は macOS でしか作れないので、publish.yml は macOS runner で走る。

## 含めていないもの

- x86_64 系の Apple ターゲット (iosX64 / macosX64 / tvosX64 / watchosX64): Kotlin で非推奨化が進んでいる
- 旧 `androidTarget()` (`com.android.library`): AGP 9 の `com.android.kotlin.multiplatform.library` を使う。
  host test は `withHostTest {}` で有効にしてあり、タスクは `testAndroidHostTest`、AAR は `assembleAndroidMain`
