# 見た目の headless 確認 (renderComposeScene + Jewel standalone + VRT golden)

Jewel/Compose の UI を **IDE を起動せず** PNG に焼き、エージェントが画像として目視する。同一マシンで
描画がバイト決定的なので、golden 比較 (VRT: Visual Regression Test) のゲートも掛けられる。

参照実装例: `<plugin-module>/src/preview/.../PreviewMain.kt` (+ `PreviewVrt` / `PreviewChecks` / gradle の
`updatePreview` / `verifyPreview`)。

## 中核レシピ

```kotlin
@file:OptIn(InternalComposeUiApi::class)   // renderComposeScene
System.setProperty("java.awt.headless", "true")
System.setProperty("skiko.renderApi", "SOFTWARE")   // IDE 不要・SW ラスタライズ

val image = renderComposeScene(width = w, height = h) {   // androidx.compose.ui.renderComposeScene
    IntUiTheme(isDark = dark) {                            // standalone Jewel Int UI → テーマ忠実
        // render root を theme の panel background で全面塗装する (下記 alpha 検査のため)。
        Box(Modifier.fillMaxSize().background(JewelTheme.globalColors.panelBackground)) {
            PluginToolWindowContent(model)            // 図(Compose Canvas) + Jewel chrome を丸ごと
        }
    }
}
File(out).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)   // org.jetbrains.skia.Image
```

- **plugin 本体は同じ Composable を `addComposeTab` でホストする** (`ide-integration.md`)。preview と
  plugin でテーマ wrapper だけが違うので、PNG は出荷物に忠実。
- gradle: `previewImplementation(compose.desktop.currentOs)` + standalone Jewel。`uiTestJUnit4` は不要。
  依存・タスク登録の配線は `setup/preview.md`、golden 回帰の配線は `setup/snapshot.md`。

## gradle タスクの型 (update / verify)

`JavaExec` で `PreviewMainKt` を回し、引数で mode を切る。

| タスク | mode | 動作 |
|---|---|---|
| `./gradlew updatePreview` | `update` | 全 PNG を焼く → gallery `build/preview/index.html` を書く → golden (`snapshots/preview`) を強制同期 |
| `./gradlew verifyPreview` | `verify` | 全 PNG を焼いて golden と比較、差分 (changed/new/missing) があれば非ゼロ終了。report は `build/preview/report/index.html` |

## 推奨ワークフロー (baseline 確認 → 変更 → 差分検出 → 目視 → 人間確認)

`snapshots/preview/*.png` は **コミット済み golden** が回帰基準。VRT の目的はこの golden に対する差分検出
なので、**検証前に golden を上書きしない** — `updatePreview` を先に回すと、チェックアウト時点で既にあった
回帰や未コミット変更を検出前に受け入れてしまい、以後の verify は「上書き後からの差分」しか見なくなる。
順番は「まず verify、golden の更新は人間承認の後だけ」。

1. **着手前に `./gradlew verifyPreview`** — コミット済み golden に対して現状を検証し、**clean な baseline
   から始める**ことを確認する。
   - ここで差分が出たら着手前に切り分ける: 自分の未コミット変更なら退避、他者由来の回帰なら先に対処。
     **環境/JBR 差の drift** と判断できる場合も golden をいきなり上書きせず、「golden は canonical な
     マシン/CI で再生成する」方針にする (描画はマシン間で決定的でないので、ローカル正規化を常態化すると
     回帰基準が壊れる)。差分を退避・レビューしてからでないと正規化しない。
2. 実装作業。
3. `./gradlew verifyPreview` — golden との差分を検出する。**差分が出たら `build/preview/report/index.html`
   を開いて before/after を目視確認**する (数値の一致ではなく、はみ出し/重なり/色/ラベル衝突/配置を見る)。
   広範な描画変更なら report の各 PNG を 1 枚ずつ見る (1〜2 枚だけの確認は不可)。
4. 差分が意図と違う / 見た目が悪ければ **修正 → 3 に戻る** (`verifyPreview` を繰り返す)。golden はまだ触らない。
5. 目視で問題なしと判断したら **人間に確認を依頼**する。このとき **verify report の場所
   (`build/preview/report/index.html`) を必ず伝える** — 人間が同じ before/after を見られるようにする。
6. **人間が承認したら初めて** `./gradlew updatePreview` で意図した差分を golden に焼き直し、
   `snapshots/preview` の差分を commit する。`updatePreview` は「承認済みの意図変更を受け入れる」操作に
   限定し、検証の代わりに使わない。

- 新規シナリオを足すと verify では `new` (golden 未登録) として出る。意図通りなら updatePreview で golden 化。
- CI は `verifyPreview` をゲートにする (golden と不一致なら fail)。
- 多数の PNG 目視/採点/修正を subagent に振るときは **1 agent = 10〜15 図のまとまり** (読み込み重複を避ける)。

## 自動ゲート (目視の自己弁護を排す)

- **透明角の自動検査**: standalone preview の render root が theme surface を塗らないと透明背景 PNG に
  なり、暗い viewer で黒 marker / 薄線 / table header が消える。**四隅 pixel を含め alpha=255 を検査**し、
  透明角があれば fail (`PreviewChecks.transparentCornerPngs`)。透明版が要るときだけ別 suffix。
- **managed 出力の事前 clean**: 生成前に管理下の生成物 (`preview-*.png` / `index.html`) を消す。rename/削除
  した旧 scenario の PNG が gallery に残り続けるのを防ぐ。expected filename set の完全一致も検査する。
- **制約は機械判定に落とす**: 「線を曲げない」「重ねない」等の制約は、折れ線の頂点数 == 2 のような
  テストに固定して目視の主観を排除する。参照画像があるなら**自出力と並べて差分を列挙**してから報告。

## 限界 (これは headless では見えない)

- **`HorizontalSplitLayout` など `layoutCoordinates` 依存の分割レイアウトは、単発 `renderComposeScene`
  では描画されない** → 実 IDE の live ComposePanel でのみ確認できる。preview scene は非分割の各パネルを
  個別に焼いて回避する。
- interactive / timed な状態 (focus 強調・flow 再生・パルスアニメ) は 1 フレーム static render に写らない
  → **固定値を注入して決定的な中間フレームに焼く** (例: パルス intensity を固定、focus selection を
  引数で与える)。真の対話・アニメは実 IDE / Driver で確認する。
- **standalone Jewel version と実 plugin の IDE bundled Jewel は不一致**うる (例 `0.37.0-261.26222.65`)。
  preview を実機同等と扱わず、定期的に `runIde` / AS smoke で差を確認する。
- **standalone preview で `AllIconsKeys` を使うなら `com.jetbrains.intellij.platform:icons` を
  previewImplementation に足す** (無いとマゼンタのプレースホルダになる)。実 plugin は platform から
  解決するので不要。

## preview matrix に含めると良いシナリオ

正常系だけでなく **degraded/edge を網羅**する: narrow 幅 (例 300〜340px)・degraded バナー・render error・
dropdown open・zoom 50%/250%・dark の setup/indexing 空状態・長い名前 (折返し/autosize)・到達不能 state。
静止 PNG で見えない interaction は semantics/UI テストで別途担保する。
