# 共通仕様: ディレクトリと保存データの形式

どの adapter・どの Sink でも同じになる「置き場の並び」と「記録・メモ・履歴の形」。パス名は例で、導入先で読み替えてよい（読み替えたら loop 側の置き換え表 `references/loop/loop.md` §0 にも書く）。

## 1. 用語

| 用語 | 意味 |
|---|---|
| **Inbox**（フィードバック置き場） | 未対応の記録が溜まる場所。1 件 = 1 記録（Markdown）+ 添付 |
| **Archive**（対応済み置き場） | 対応が終わった記録の移し先。状態は「Inbox と Archive のどちらにあるか」だけ |
| **添付置き場** | スクリーンショットなど。記録には参照（パス・URL）だけを書く |
| **Sink** | Inbox・Archive・添付置き場を差し替える層（`transport.md`） |
| **受け口** | Sink を持ち、送られた内容から記録を組み立てて保存する側 |
| **Transport** | 画面 → 受け口の送り方（IPC / HTTP / toolbar のメッセージ…） |
| **adapter** | 環境ごとに差し替える画面側の部分（`references/setup/adapter.md`） |
| **オーケストレータ** | 人の窓口の会話。受付・分類・起動・archive・報告だけをする |
| **履歴** | 受けた記録と対応結果の一覧 |
| **メモ** | ページと箇所ごとに書き溜める継続メモ（optional。§5） |
| **Review** | agent → 人の指摘。人は画面の印と popup で答える（`references/review/review.md`） |
| **判断の記録** | 人の「直さない・このまま・後で・方針」を溜め、同じことを二度聞かない（`references/review/decisions.md`） |

## 2. ディレクトリ

```
<Inbox>/                      例: feedback/ や .local/feedback/（git に入れるかは導入先で決める。既定は .gitignore）
├ yymmdd-xx.md                未対応の記録
├ archived/yymmdd-xx.md       対応済みの記録（番号は Inbox と通し）
├ attachments/<日時>/         添付（window.png・element.png）。Inbox の外に置いてもよい（例: .local/tmp/feedback/）
├ memo/<言語>/<ページ>.md      メモ（optional）。記録に数えない
├ memo/_fields.json           メモの field のひな形（全ページ共有）
├ review/<まとまり>.md         Review の指摘（agent が書く）
├ review/_actions.md          人が足した選択肢
└ HISTORY.md                  履歴（optional。§6）
.claude/feedback-decisions/   判断の記録（git に入れる。<領域>.md）
```

- 記録として数えるのは直下の `yymmdd-xx.md` だけ。`archived/`・`attachments/`・`memo/`・`review/`・`HISTORY.md`・`.gitkeep` は数えない。
- 添付・メモ・Review・判断の記録は、作っていなければ無くてよい。

## 3. 記録（1 件）

### 3.1 名前と採番

- id は `yymmdd`（ローカルの日付）+ `-` + その日の連番（2 桁以上、`01` から）。ファイルなら `<id>.md`。**人の入力を名前に含めない**。
- 連番 = Inbox と Archive にある同じ日付の記録（空ファイルも）の最大 + 1。**番号を再利用しない**。日付が変われば `01`。桁を増やしても前の記録と重ならないよう、数えるときは 2 桁以上を全部読む。
- 上書きしない（`wx` / `create_new`）。競合したら数え直して再試行（最大 10 回）。
- 数えてから書くまでに別のプロセスが同じ番号を保存して archive すると、`wx` は通るのに番号が重なる。書く前と書いた後に Archive に同じ名前が無いかを確かめ、あれば自分の分を消して数え直す。
- 保存先はルートの中だけ（`..`・絶対パス・外を指すシンボリックリンクは拒否）。

### 3.2 作られ方

(a) アプリのフィードバックモードから保存（4KB〜100KB）、(b) 人が手で書いた短いメモ（数行）、(c) 会話に貼られた文（記録にならない。履歴の番号は「（会話）」）。loop は 3 つとも扱う。

### 3.3 Markdown の構成

冒頭は `# <アプリ名>: 画面へのフィードバック` と **Agent への案内 1 行**（`pointer`。ソースの場所・ログの形）。オプションで外した節があれば「（オプションでオフ: …）」の 1 行。続いて次の節を、この順で並べる。

| 順 | 見出し | 中身 | オプション名（初期値） |
|---|---|---|---|
| 1 | フィードバックの文 | 引用ブロック。空なら「（書いていません）」 | 常に入る |
| 2 | 選んだ位置 | 画面・部品の道筋・要素の種類とラベル・場所の手がかり・矩形・クリック座標・ウィンドウ・スクロール・要素の状態 | `target`（オン） |
| 3 | スクリーンショット | `window.png`（選んだ部品に赤枠）と `element.png`（周り 24px）の参照。撮れなければ理由 | `shots`（オン。オフなら撮影もしない） |
| 4 | 環境 | 版・起動の種類・OS・ランタイム・パス | `env`（オフ） |
| 5 | 今の画面 | 画面名・ダイアログ・ウィンドウ・編集中か・モード | `screen`（オフ） |
| 6 | 画面に出ている情報 | **状態（モデル）から作る。表示の文字は拾わない**。個人的な入力は字数だけ | `view`（オフ） |
| 7 | ログ | 古い順。error/warn は先頭に要約（新しい 40 件まで）。200KB を超えたら古い方から省き、省いた件数を書く | `log`（オン） |

- 複数の要素を選んだ記録は、節の中を要素ごとに `### 1.`・`### 2.` と並べ、スクリーンショットの枠の番号とそろえる。
- 組み立ての最後に、文の全体へ redact を 1 回掛ける（Bearer・JWT・`Cookie:`・`token=`・`password=`・`secret=`・`api_key=`・`sk-`・`ghp_`・`github_pat_`）。コピーと保存で同じ文にする。
- 見出しは日本語固定。変えるなら loop の抜粋（`## フィードバックの文` を探す所）も変える。
- 実装: `assets/templates/core/feedback-core.mjs` の `buildFeedbackMarkdown`。例: `assets/templates/data/feedback-file.example.md`。

### 3.4 frontmatter（schema 1）

loop が全文を読まずに分類できるよう、記録の先頭に付ける。`buildFeedbackMarkdown` / `submitFeedback` に `meta` を渡したときだけ書く。定義: `assets/templates/data/feedback.schema.json` の `$defs.recordFrontmatter`、例: `assets/templates/data/feedback-file.schema1.example.md`。

| キー | 中身 | 書く条件 |
|---|---|---|
| `schema` | `1` | いつも |
| `kind` | `element` / `elements` / `text` / `page` / `site` | いつも（無ければ部品を選んだら element、選ばなければ page） |
| `screen`・`target`・`source` | 画面・部品の道筋・ソースの行（`path:12-14`）。複数の要素なら配列で、値の無い要素は `null`（番号をずらさない） | 分かるときだけ |
| `created` | 保存の時刻（ISO 8601） | いつも |
| `dirty` | 記録の時点でソースに未コミットの変更があった | 受け口が git で確かめられたときだけ |
| `stale` | 画面を開いた後にソースが変わった。true なら本文の冒頭にも「注意:」の 1 行 | 受け口が比べられたときだけ |
| `omitted` | 載せなかった節 | いつも（空なら `[]`） |
| `attachments` | 添付の参照 | 添付があるとき |
| `id` | `yymmdd-xx` | Sink の採番を文に渡せるときだけ |

- 値は JSON の書き方（`"…"`・`[…]`・`true`）にする。YAML としても読める。
- frontmatter の無い記録（旧形・手書き）は schema 0 として読む。キーが無いのは「情報が無い」で異常ではない。`dirty` / `stale` が無いのは「判定できなかった」で「新しい」ではない。両方の形が同じ Inbox に混ざってよい。

### 3.5 状態

```
（作成）→ Inbox［未対応］→（対応・報告）→ オーケストレータだけが archive → Archive［対応済み］
```

ステータス欄は持たない。archive のときは 1 行の対応メモを付ける（ファイルなら末尾に「対応メモ: …」を足す。Sink の `archive(id, { note })`）。保留・判断待ちは Inbox に残すか、「判断待ち」と明記して archive する（人に聞く）。

## 4. 送信 body（画面 → 受け口）

画面が受け口に送るのは記録の文ではなく、組み立て前の材料（`FeedbackSubmission`）。定義は `assets/templates/data/feedback.schema.json`。

```
{ payload: { appName, pointer, kind, pageOpenedAt, comment, target{screen,path,element,selector,xpath,fragment,source,rect,click,state}, viewport, env, screen, view, logs[] },
  options: { target, log, shots, env, screen, view },
  windowPng, elementPng,   // base64。撮れなければ null と shotError
  shotError }
```

- 受け口が `submitFeedback` で「添付を保存 → 文を組み立て → redact → 記録を保存」する。画面が JS で文を組み立てられる環境は、画面の中で `submitFeedback` を呼んで HTTP の Sink に送ってもよい（`transport.md`）。
- 場所の手がかり（`selector`・`xpath`）は、保存の前に引き直して同じ要素に戻ったものだけを入れる（違う場所を指す手がかりは、無いより悪い）。

## 5. メモ（optional）

| | フィードバック | メモ |
|---|---|---|
| 性質 | 一度送って閉じる。1 件 = 1 記録 | 書き溜めて何度でも直す。ページごとに 1 ファイル |
| 置き場 | `<Inbox>/yymmdd-xx.md`（Sink の `putEntry`。採番する） | `<Inbox>/memo/<言語>/<ページ>.md`（Sink の `writeDoc`。上書き） |
| 状態 | Inbox → Archive | 無い。archive しない |
| loop の扱い | 対応して閉じる | **読むだけ**（意図の手がかり）。書き換えない・消さない・移さない |

ファイルの形:

- 見える部分は Agent 向け: frontmatter・`## ページのメモ`・`## 箇所のメモ`（各箇所の場所の手がかりと本文・field）。各メモに「最終更新」の時刻（中身が変わったときだけ進める）。箇所には作成時刻も。
- 末尾の HTML コメントに画面の読み直し用の JSON。JSON の中の `<` `>` は Unicode エスケープ `\u003c` `\u003e` にする（`JSON.stringify(data).replace(/</g, '\\u003c').replace(/>/g, '\\u003e')`。`JSON.parse` で元に戻る）。中身の `-->` でコメントが閉じないようにするため。読み直せないファイルは上書きせず、保存を止めて知らせる。
- パスはページの URL から作るので検査する: 言語は `^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$`、各段は文字・数字・`._-` だけ、`..` は拒否。Sink 側でも `safeDocPath` で外に出さない。
- `memo/_fields.json` は全ページ共有の field 名の一覧（例: 「読みやすさ」「優先度」）。ひな形から外しても値は消さない。壊れていたら既定に戻す。
- 例: `assets/templates/data/memo-file.example.md`。

## 6. 履歴（optional）

archive のたびに 1 行足す表。正本 docs に「<日付> フィードバック <id>「…」」と残す場合は、その索引になる。

| 番号 | 要旨（文の冒頭） | 位置 | 種類 | 結果 |
|---|---|---|---|---|
| 261023-15 | 情報が多すぎる、一部を抜きたい | 吹き出し | 実 | 吹き出しにオプションを足した |
| （会話） | 標準の select でよかった | 一覧/メニュー | 実 | 標準の `<select>` に置き換え |

種類は 実（実装）・調（調査）・設（設計）。複数なら「実・設」。結果の末尾に「判断待ち」「のち N で変更」を付けてよい。
