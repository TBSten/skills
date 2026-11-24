# web / chrome-extension（どのページでも選んでコピーする Chrome 拡張）

検証状況: Playwright 1.63 の Chromium（画面あり・xvfb）に、ひな形から作った拡張を読み込み、OS のキー（xdotool の Alt+Shift+F）とマウスで「入る → 部品を選んでも押されない → 書く → Ctrl+Enter → クリップボードに記録（schema 1・文・部品の道筋・セレクタ・URL）→ 出る・リンクを選んでも移動しない・Esc / もう一度のキーで出る・`content.js` の CONFIG の書き換えが記録に入る・ページのエラー 0」を確認（`scripts/chrome-extension.e2e.cjs`）。CSP が厳しいページ（`script-src 'self'`）でも同じ。ページの許可なしでコピーできるのは `clipboardWrite` の権限があるときだけだった（無いと「コピーしました」と出るのに空になる）。Chrome の正式版（Google Chrome）で読み込む手順と、ツールバーのボタンを人の手で押すことは確かめていない（ボタンとショートカットは同じ `chrome.action.onClicked` に届く）。
ひな形: `assets/templates/adapters/web/chrome-extension/`（`manifest.json`・`background.js`・`content.js`）。画面側は dom の `feedback-mode.js`、記録の文はコアの `buildFeedbackMarkdown` を `vendor/` に写して使う。

アプリに組み込まず、**人のブラウザに入れる開発用の拡張**。どのサイト（自分のアプリの開発サーバ・ステージング・他社のページ）でも、ボタンを押したタブで部品を選び、記録の文をクリップボードにコピーする。人はそれをチャットに貼る（`references/spec/data.md` §3.2 の「会話に貼る」）。受け口・置き場は無い。

| | アプリに入れる adapter（dom など） | Chrome 拡張（ここ） |
|---|---|---|
| 入れる先 | アプリのコード（debug ビルドだけ） | 人のブラウザ（パッケージ化せずに読み込む） |
| 送り先 | 受け口 → 置き場 | クリップボード |
| 本番から外す | 要る | 要らない（アプリに何も足さない） |
| 部品の名前 | アプリに `data-fb` を付ける | あれば使う。無ければ要素の説明と CSS セレクタ |

## 1. 作る（agent の手順）

1. **置き場所を人に聞く**（決めずに進めない）。推奨を添えて聞く:
   - ★ リポジトリの中の開発用ツールの場所（例: `tools/feedback-extension/`）。チームで同じ拡張を使い、CONFIG を git で共有できる
   - リポジトリの外（例: `~/feedback-extension/`）。自分だけで使う・どのリポジトリにも属さない
   - ほかのパス
2. 作る。拡張の名前も人に聞いてよい（`chrome://extensions` の一覧に出る）:

   ```bash
   E="${CLAUDE_SKILL_DIR}/scripts/chrome-extension.mjs"
   node "$E" new <置き場所> --name "<拡張の名前>"   # 空でなければ中断（--force で上書き）
   node "$E" check <置き場所>                      # 直したあとも毎回
   ```

3. `content.js` の CONFIG を書く（下の §2）。`pointer` には、読む Agent がソースにたどり着ける案内（リポジトリ・ソースの場所・正本 docs）を書く。
4. 人に読み込んでもらう: `chrome://extensions` →「デベロッパー モード」をオン →「パッケージ化されていない拡張機能を読み込む」→ 置き場所を選ぶ。直したら同じ画面で拡張の「再読み込み」。
5. ショートカットが他の拡張と重なったら `chrome://extensions/shortcuts` で変える。

script は読解・書き換え・再実装せず、そのまま実行する。

## 2. 人が書き換えてよいところ

| ファイル | 書き換えてよいこと |
|---|---|
| `content.js` の CONFIG | `appName`（見出しの名前）・`pointer`（Agent への案内）・`getScreen`（画面の名前。既定は URL）・`defaultOptions`（節の初期値） |
| `content.js` の残り | 送り方（`copyTransport`）。例: 自分の受け口に送る、文の先頭に決まった前置きを足す |
| `manifest.json` | 名前・ショートカット・権限 |
| `vendor/` | **書き換えない**。`sync` が skill の版で上書きする。直したいことは `content.js` 側で `installFeedbackMode` の引数として渡す |

skill を更新したら `node "$E" sync <置き場所>` で `vendor/` だけを新しい版にする（`content.js`・`manifest.json` はそのまま）。`check` は `vendor/` が skill の版と違えば警告する。

## 3. 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | ツールバーのボタンと Alt+Shift+F（`manifest.json` の `commands._execute_action`）。押すと `background.js` が今のタブに `content.js` を入れ、2 回目からは入る / 出るを切り替える。ページ側のホットキーは切る（`hotkey: () => false`） |
| `pick` / `block` | dom と同じ（全面オーバーレイ。下の部品にクリックが届かない・リンクで移動しない） |
| `name` | ページに `data-fb` があれば使う。無ければ要素の説明と CSS セレクタ（O6） |
| `collect` | `getScreen`（既定: URL とタイトル）。ページのアプリの状態（store など）は拡張から読めない |
| `logs` | 無い（拡張はページの console を読めない）。節は初期値でオフ |
| `capture` | 無い（クリップボードは文だけ）。節は初期値でオフ |
| `transport` | `content.js` の `copyTransport`: `buildFeedbackMarkdown`（schema 1）→ `redact` → `navigator.clipboard.writeText`。`{ ok, message }` を返し、「クリップボードにコピーしました」と出る |
| `pointer` | CONFIG の `pointer` |

## 4. 権限

- `activeTab` + `scripting`: ボタンを押したタブにだけ、その時だけ入る。どのサイトにも常に入る権限（`host_permissions`・`content_scripts` の `<all_urls>`）は持たない。`check` は広い権限があれば警告する。
- `clipboardWrite`: ページの許可なしでコピーするため。外すと「コピーしました」と出るのにクリップボードが空になった。
- `web_accessible_resources` の `vendor/*`（`use_dynamic_url`）: `content.js` が `import(chrome.runtime.getURL(...))` で読むため。
- `chrome://`・Chrome ウェブストアのページには入れない（`background.js` が警告を出すだけ）。

## 5. この環境の注意

- 節のオン/オフはページの `localStorage`（`feedback-mode:options`）に覚える。拡張はページと同じ場所に書く。
- 選べるのは同じ文書の中だけ。iframe の中・閉じた shadow root の中は選べない（dom と同じ）。
- 画面の確かめは `node "${CLAUDE_SKILL_DIR}/scripts/chrome-extension.e2e.cjs" [<置き場所>]`（Playwright の Chromium・xdotool が要る。画面が無ければ xvfb-run の中で起動し直す。無ければ skip）。拡張は画面ありのときだけ読み込め、ショートカットは OS のキーでしか動かないので、Playwright のキー入力ではなく xdotool で押す。
