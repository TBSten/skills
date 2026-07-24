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
- **日本語ラベルは実測で文字化けしうる** → 図/生成物のラベルは英語を既定にする。
- **plain Swing プレビュー / 自前 Graphics2D canvas / JB* を test で描画は supersede** → Jewel standalone +
  `renderComposeScene` に一本化 (テーマ忠実 & headless。詳細は SKILL.md「中心思想」の旧案節)。

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
