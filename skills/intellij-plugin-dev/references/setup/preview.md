# preview (headless PNG) を焼くための build 配線

`renderComposeScene` で UI を PNG に焼くための gradle 配線。**焼き方・見た目の回し方 (ワークフロー) は
`headless-preview.md`**、golden との回帰は `setup/snapshot.md`。ここは「PNG が出るところまで」の配線。

実ファイル (SSoT): `example/build.gradle.kts` (`sourceSets {}` / `previewImplementation` 依存 /
`registerPreviewTask`)。snippet を転記せず `scripts/scaffold.sh` で生成する (SKILL.md)。

## source set 共有 (plugin 本体 ⇄ preview で Composable を共通化)

図/UI の Composable は plugin 本体 (bundled Jewel) と preview (standalone Jewel) で **同じ Jewel/Compose
API**。`src/shared/kotlin` を両 source set の srcDir に足し、それぞれの Compose 依存で二重コンパイルする
(共有 Composable の例: `example/src/shared/kotlin/com/example/plugin/ui/ExampleToolWindowContent.kt`)。

- `sourceSets`: main と `create("preview")` の両方に `kotlin.srcDir("src/shared/kotlin")`。
- `previewImplementation` 依存: `compose.desktop.currentOs` (renderComposeScene はここ・Skiko 同梱。
  `uiTestJUnit4` は不要) + `org.jetbrains.jewel:jewel-int-ui-standalone:<jewel>-<jewelForIde>` +
  `com.jetbrains.intellij.platform:icons:<jewelForIde>` (AllIconsKeys 用。無いとマゼンタの
  プレースホルダになる)。バージョンは `example/gradle/libs.versions.toml`。
- plugins block には `org.jetbrains.compose` (preview の standalone Compose Desktop 用) と
  `org.jetbrains.kotlin.plugin.compose` が要る (`setup/basics.md` のバージョンと揃える)。
- **二重コンパイルの副作用に注意**: `src/shared` は main と preview の両方でコンパイルされるので、
  test で使わない sample が「main 側コピー未使用」判定になることがある。未使用判定は推測せず
  jetbrains MCP `get_file_problems` で事実確認する。

## preview タスクの登録 (JavaExec)

preview の `main()` (`PreviewMainKt`) を standalone 依存で回す。第 1 引数で mode を切る (`update` /
`verify`)。headless + Skiko SOFTWARE で IDE 起動不要。

実体は `example/build.gradle.kts` の `registerPreviewTask` (SSoT): `JavaExec` で
`mainClass = <pkg>.preview.PreviewMainKt` / `classpath = sourceSets["preview"].runtimeClasspath` /
`jvmArgs("-Djava.awt.headless=true", "-Dskiko.renderApi=SOFTWARE")` / `args(mode)` を配線し、
`updatePreview` (mode=update) と `verifyPreview` (mode=verify) の 2 タスクを登録する。

- 出力は `build/preview/preview-<scenario>-<theme>.png` + gallery `build/preview/index.html`。
- `update` / `verify` の golden 側の意味と自動ゲートは `setup/snapshot.md`。
