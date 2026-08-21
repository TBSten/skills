# snapshot (VRT golden) の配線

`setup/preview.md` で焼いた PNG を **golden として回帰テスト**するレイヤ (VRT: Visual Regression Test)。
同一マシンで描画がバイト決定的なことを利用する。**日々の回し方 (baseline → verify → 目視 → 人間確認)
は `headless-preview.md` の「推奨ワークフロー」**。ここは golden の置き場と自動ゲートの配線。

## golden の置き場と update / verify の意味

| タスク | mode | golden への作用 |
|---|---|---|
| `./gradlew updatePreview` | `update` | 焼いた PNG を golden ディレクトリ `snapshots/preview` に **強制同期** (baseline を更新)。gallery も書く |
| `./gradlew verifyPreview` | `verify` | 焼いた PNG を `snapshots/preview` と比較。差分 (changed / new / missing) があれば **非ゼロ終了**。golden は変更しない。report は `build/preview/report/index.html` |

- `snapshots/preview/*.png` は **コミット対象** (golden)。意図した見た目変更のときだけ `updatePreview`
  で焼き直して差分を commit する。
- CI は `verifyPreview` をゲートにする (golden と不一致なら fail)。**独立ビルドなので root の通常
  test/check では走らない** → root/CI から独立ビルドを明示呼び出しする quality gate を足す。

## 自動ゲート (目視の自己弁護を排す)

`PreviewMain` / `PreviewChecks` が update・verify 双方で走らせる不変条件。golden 比較の手前で
silent な劣化を止める。実装 (SSoT): `example/src/preview/kotlin/com/example/plugin/preview/PreviewMain.kt`
+ `PreviewChecks.kt`。

- **透明角の自動検査**: render root が theme surface を塗らないと透明背景 PNG になり、暗い viewer で
  黒 marker / 薄線 / table header が消える。**四隅 pixel を含め alpha=255 を検査**し、透明角があれば
  fail (`PreviewChecks.transparentCornerPngs`)。透明版が要るときだけ別 suffix。塗り方は `headless-preview.md`。
- **managed 出力の事前 clean**: 生成前に管理下の生成物 (`preview-*.png` / `index.html`) を消す。rename/削除
  した旧 scenario の PNG が gallery / golden に残り続けるのを防ぐ。**expected filename set の完全一致**も
  検査する。
- **制約は機械判定に落とす**: 「線を曲げない」「重ねない」等は、折れ線の頂点数 == 2 のようなテストに
  固定して目視の主観を排除する (VRT とは別に unit test 側で)。

## test から純出力ゲートを叩く

preview の純粋な出力チェック (alpha / stale 削除など、Compose に触れない部分) を test から回せるよう、
**preview の output (compiled class) だけ** を testImplementation に足す (standalone Compose 依存は載せ
ない → bundled Compose との二重ロードを避ける)。

実体は `example/build.gradle.kts` の `testImplementation(sourceSets["preview"].output)` (SSoT)。
test 側の例: `example/src/test/kotlin/com/example/plugin/PreviewOutputGateTest.kt`
(透明角検出 / expected filename set / changed・new・missing 分類を PreviewChecks 単体で検証)。
