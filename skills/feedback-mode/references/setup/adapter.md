# adapter: 環境ごとに差し替える部分

フィードバックモードのうち、環境（Web・Electron・モバイル…）で変わるのは下の **差し込み口** だけ。受け口・Sink・記録の形（`references/spec/`）は共通のまま使う。adapter のファイル（`setup/<分類>/<環境>.md`）は、この表の各行をその環境でどう埋めるかだけを書く。

## 1. 差し込み口

| 差し込み口 | 何を埋めるか | 例 |
|---|---|---|
| `entry` 入口 | モードに入る手段。既存の debug menu・dev toolbar があればそこに 1 項目足す（新しく作らない） | ホットキー Alt+Shift+F・メニュー項目・FAB・シェイク・Dev Menu の項目・Astro の toolbar アプリ |
| `pick` 部品の選択 | ポインタ・タップの位置から部品を決める | `document.elementsFromPoint`・名前を付けた部品の registry から位置を含む最小の矩形・`UIView.hitTest`・ビューの再帰 |
| `name` 部品の名前 | 道筋に使う名前の付け方（`references/spec/features.md` R5） | `data-fb`・`Modifier.fbTarget("画面/部品")`・`.fbTarget("部品")`・`accessibilityIdentifier`・`testTag` |
| `block` 操作を止める | モード中にアプリへ入力が届かないようにする（R3） | 全面の透明オーバーレイ + window の capture で click / key / focus を止める |
| `collect` 文脈 | `getScreen`（画面 ID）・`getView`（画面の状態。モデルから）・`stateOf`（要素ごとのアプリ固有の状態）・`getEnv`（版・OS） | ルーターの現在地・store の状態 |
| `logs` ログ | `getRecentLogs()` → `[{ ts, level, text }]`（古い順）と、任意の `logEvent(level, text)`。**既存のログ基盤を読むだけ**（作るのは別タスク。`setup.md` §4） | アプリの logger のバッファ・Sentry の breadcrumb |
| `capture` スクショ | モードの UI を隠し、枠を描いてから撮る。撮れなければ `{ error }`（optional O1） | `capturePage`・html2canvas・view-shot・`RepaintBoundary.toImage`・`drawHierarchy`・`PixelCopy` |
| `transport` 送り方 | 画面 → 受け口（`references/spec/transport.md` §3）。結果は `{ ok, id, ref, error }` | `ipcTransport`・`httpTransport(url)`・toolbar の `server.send` |
| `exclude` 本番から外す | ビルド時に消す方法・実行時の保険・成果物の検査パターン（R1） | 動的 import + `import.meta.env.DEV`・`app.isPackaged`・`__DEV__`・別 entrypoint・debug source set・`#if DEBUG` |
| `pointer` Agent への案内 | 記録の冒頭に書く 1 行（ソースの場所・ログの形・正本 docs） | 「ソースは `src/`、ログの形は README の『ログ』」 |

Web 系の DOM 版（`assets/templates/adapters/web/dom/feedback-mode.js`）は、この表がそのまま `installFeedbackMode(opts)` の引数になっている（`hotkey`・`getScreen`・`getView`・`stateOf`・`getEnv`・`getRecentLogs`・`logEvent`・`capture`・`transport`・`pointer`・`enabled`）。DOM が無い環境のひな形（Compose の `FeedbackConfig`・SwiftUI の `FeedbackMode.shared.config`）も、同じ名前（`appName`・`pointer`・`getScreen`・`getView`・`getEnv`・`getRecentLogs`・`transport`）で受ける。足りない差し込み口は、同じ名前で props / 引数を足す。

## 2. adapter の一覧

| 分類 | adapter | 読むファイル | ひな形 | 検証状況 |
|---|---|---|---|---|
| web | dom（素の DOM・Vite・webpack） | `web/dom.md` | `adapters/web/dom/` | ヘッドレスの Chromium で通しを確認 |
| web | react（+ Vite） | `web/react.md` | dom を使う | React 19 + Vite 7 で通し・スクショ・本番の検査を確認 |
| web | astro | `web/astro.md` | `adapters/web/astro/` | Astro 7 の実サイトと最小プロジェクトで確認 |
| web | single-html（agent が作る 1 枚の HTML） | `web/single-html.md` | `adapters/web/single-html/`（+ `scripts/single-html.mjs`） | PC・スマホ幅・保存できない状態で e2e を確認 |
| desktop | electron | `desktop/electron.md` | `adapters/desktop/electron/` | Electron 38 で通し・capturePage・asar の検査を確認 |
| desktop | tauri | `desktop/tauri.md` | dom を使う | Tauri 2 + Vite 7（Linux）で通し・release の検査を確認 |
| mobile | android（Jetpack Compose） | `mobile/android.md` | `adapters/mobile/compose/` + `adapters/mobile/android/` | エミュレータ（API 36.1）で通しと release の APK の検査を確認。View（XML）の画面は未確認 |
| mobile | kmp（Compose Multiplatform: Android・iOS・Desktop・Web） | `mobile/kmp.md` | `adapters/mobile/compose/` + `adapters/mobile/kmp/` | Android エミュレータ・iOS シミュレータ・wasmJs（Chrome）で通しを、Desktop は UI テストで 1 件送るまでを確認。4 つとも本番の成果物の検査を確認 |
| mobile | ios（SwiftUI） | `mobile/ios.md` | `adapters/mobile/ios/` | iOS 26.2 シミュレータで通しと Release の `.app` の検査を確認。実機は未確認 |

モバイル共通の前提（DOM が無い・ホバーが無い・端末から開発機へ届かせる）は `mobile/common.md`。Web 共通の操作の作り方は `web/interaction.md`、場所の手がかりは `web/locate.md`、メモは `web/memo.md`。

## 3. 一覧に無い環境の adapter を作る

1. いちばん近い adapter を土台にする（DOM がある → dom、WebView + 別言語の本体 → tauri（受け口は HTTP の別プロセス）、DOM が無いがタッチ → mobile/common.md と android（Compose）/ ios（SwiftUI））。
2. §1 の表を 1 行ずつ埋める。分からない行は「無し」でよい（`capture`・`logs` は省略できる。記録に「撮っていません」「ログ: 0 件」と出る）。
3. 受け口は、組み立てを JS で書けるなら `submitFeedback` + 好きな Sink、書けないなら HTTP の受け口（`core/feedback-dev-server.mjs`）に submission を送る。Node が無い受け口（Rust など）は `references/spec/data.md` §3.1 と `transport.md` §1 の規則を移植し、契約テストと同じ内容のテストを書く。
4. **実際に動かして確かめる**（`setup.md` 手順 8 と本番の成果物の検査）。skill に取り込むのは確かめたものだけ。
5. `setup/<分類>/<環境>.md` を、下の形で足す（skill に取り込むなら contribute-skill の手順で）。

```markdown
# <環境>
検証状況: <何の版で・どう動かして・何を確かめたか を 1〜2 行>
ひな形: `assets/templates/adapters/<分類>/<環境>/`（無ければ「無い」）

## 差し込み口
| 差し込み口 | この環境での埋め方 |
（adapter.md §1 の行のうち、変わるものだけ）

## 本番から外す
## この環境の注意
```
