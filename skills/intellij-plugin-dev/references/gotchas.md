# ハマりどころ (Compose Desktop / IntelliJ 固有)

主要な落とし穴は各 reference に散っているので、ここは「どこにも属さない小さな罠」と索引を置く。

## Compose Desktop (bundled Jewel) の入力

- **トラックパッドのピンチは `detectTransformGestures` に来ない** (macOS 実測、runIde で確認)。Compose
  Desktop はピンチ magnify を transform gesture として配信しない。**ズームは `Ctrl` / `Cmd` + マウス
  ホイールで実装する**:
  ```kotlin
  Modifier.pointerInput(Unit) {
      awaitPointerEventScope {
          while (true) {
              val e = awaitPointerEvent()
              if (e.type == PointerEventType.Scroll &&
                  (e.keyboardModifiers.isCtrlPressed || e.keyboardModifiers.isMetaPressed)) {
                  val dy = e.changes.first().scrollDelta.y   // これで拾える
              }
          }
      }
  }
  ```
  `detectTransformGestures` はタッチ環境用に残置しても無害 (マウスドラッグは zoomChange==1 で no-op)。
- 修飾キーの取得は `LocalWindowInfo.keyboardModifiers.isShiftPressed` (tap 時に読む)。

## Android Studio vs IntelliJ (build 番号スキュー)

- `intellijIdea("2026.1")` は base `IU-261.22158.x` に解決。実機 AS Quail は `AI-261.23567.x`。`sinceBuild=261`
  で吸収する想定だが、**AS 実機での `runIde` 追従は対話確認を推奨** (build 番号スキュー・AS 同梱プラグインとの
  相互作用は headless では出ない)。実機基準 = Android Studio Quail 2026.1.1 / JBR 21。

## 描画/レンダリングの方針決定 (却下記録)

- **JCEF + Mermaid で図を描く案は却下**: JCEF 非搭載環境に依存する。
- **PlantUML jar 同梱は却下**: 重い。
- **`mermaid-cli` でビルド内ラスタライズは却下**: Node + Chromium 必須。→ 図は Compose Canvas を自前描画。
- **日本語ラベル** → 下記「headless preview の日本語表示」。図/生成物のラベルを英語にしておくのが一番安全。
- **plain Swing プレビュー / 自前 Graphics2D canvas / JB* を test で描画は supersede** → Jewel standalone +
  `renderComposeScene` に一本化 (テーマ忠実 & headless。詳細は SKILL.md「中心思想」の旧案節)。

## headless preview の日本語表示

- **macOS では表示される (確認済み)**: `renderComposeScene` + standalone Jewel (`IntUiTheme`) +
  `-Dskiko.renderApi=SOFTWARE` で、フォントを指定しない `Text("日本語")` が漢字・かな・カナとも正しく描かれた。
  既定フォントに無い字形は OS のフォントへフォールバックしていると考えられる。
- **Linux (CI) は未検証**: 同じ仕組みなら CJK フォントが入っていない環境 (素の CI イメージ・コンテナ) では
  豆腐 (□) になる見込み。回避策の候補 (いずれも未検証): CI に CJK フォントを入れる
  (例: Debian/Ubuntu の `fonts-noto-cjk`) / フォントファイルを preview の resources に同梱して
  `FontFamily` を明示する。どちらにしても golden は OS 間で一致しない (次節)。
- 豆腐になっても preview の自動ゲート (透明角・ファイル集合) は通ってしまう。日本語 UI なら日本語の
  scenario を 1 つ入れて PNG を目で見る。

## golden と OS (CI で `verifyPreview` を回すとき)

描画は **同一マシンでだけ** バイト決定的。フォント・アンチエイリアス・Skiko のバイナリが OS ごとに違うので、
macOS で作った golden は Linux の CI ではほぼ確実にバイト一致しない (未検証だが前提にしておく)。選択肢:

| 方針 | やり方 | 向く場面 |
|---|---|---|
| golden を CI の OS で作る | CI (または同じ OS のコンテナ) で `updatePreview` を回し、出た PNG を artifact から取って commit する。ローカルの macOS では `verifyPreview` が落ちるので、ローカルは gallery の目視だけにする | VRT を CI の門番にしたい |
| CI では描けることだけ見る | CI は `updatePreview` を回して成否 (描画が例外なく終わる・自動ゲートが通る) だけを見る。golden 比較はローカル (golden を作った OS) でだけ `verifyPreview` | 開発者が 1 つの OS にそろっている |
| OS ごとに golden を持つ | `snapshots/preview/<os>/` のように分け、`verifyPreview` が実行中の OS の golden と比べる (example は未対応。`PreviewMain.kt` の golden パスを OS で切り替える改修が要る) | 複数 OS で開発し、どこでも VRT を回したい |

どれにするかは最初の golden を commit する前に決める。決めずに macOS の golden を commit して CI で
`verifyPreview` を回すと、毎回落ちる門番になる。

## build / test classpath

- **shared のクラスが test の classpath で二重になる**: `testImplementation(sourceSets["preview"].output)`
  のため、`src/shared` のクラスが main と preview の両方から test の classpath に載る。test が shared の
  クラスを直接使わない限り実害は無い (雛形の test は使っていない)。shared のクラスを test から使うなら、
  `PreviewChecks` だけを別の source set (例: `previewChecks`) に切り出し、test はそれだけに依存させる。
  そもそも UI に依存しない純ロジックは shared ではなく `src/main` に置く (`setup/preview.md`)。
- **消したクラスがサンドボックスに残る**: `.intellijPlatform/sandbox/.../plugins-test/.../lib/` には前回の
  test / runIde で使った plugin の jar が残る。クラスを消した後にリポジトリ全体を grep すると、そこに
  引っかかって「取り残しがある」ように見える。取り残しを探すときは `.intellijPlatform/` と `build/` を除いて
  grep する (`git grep`、または `.gitignore` を読む `rg` なら自動で除かれる)。
- **`.intellijPlatform/` はコミットしない**: 初回ビルドでプラグインモジュール直下に大量の未追跡ファイルが
  出る。scaffold が生成する `.gitignore` に `build/` `.gradle/` `.intellijPlatform/` `.kotlin/` が入っている
  (既存の `.gitignore` があった場合は生成しないので、自分で足す)。

## 索引 (各罠の一次記載)

| 罠 | 参照 |
|---|---|
| `intellijIdeaCommunity` が解決不可 (統合ディストリ) / plugin version 衝突 | `setup/basics.md` |
| 二重コンパイルで sample が未使用判定 → `get_file_problems` で事実確認 | `setup/preview.md` |
| AA が壊れコードに error type を返す (例外でなく) / shortName 依存は脆い | `analysis-api-testing.md` |
| 無関係 logged error で test 失敗 / 非 hermetic なので複数回 clean 実行 | `analysis-api-testing.md` |
| `Stubs index ... stale file ids` (`addFileToProject` 過多) | `analysis-api-testing.md` |
| 透明角 PNG で暗色 viewer で線が消える / managed 出力の事前 clean | `headless-preview.md` |
| `HorizontalSplitLayout` が単発 renderComposeScene で描画されない | `headless-preview.md` |
| standalone preview のアイコンがマゼンタ (`:icons` 不足) | `headless-preview.md` |
| `Read access is allowed from inside read-action only` (PSI 挿入) | `ide-integration.md` |
| `runCatching` が `ProcessCanceledException`/`OOM` を畳む | `ide-integration.md` |
| live 更新の stale 固着 (state 名で `remember`) | `ide-integration.md` |
| dumb mode / 外部 invalidation で図が更新されない | `ide-integration.md` |
| Compose UI が Driver XPath から中を覗けない (1 ComposePanel) | `driver-smoke.md` |
| 独立ビルドは root の check で走らない → CI ゲート追加 | `driver-smoke.md` |
