# 共通: 凡例・用語・記録の形・Sink の契約

構築（`references/build/`）と実践（`references/loop/`）の両方が前提にする概念だけを置く。必要になったときだけ読む（片方の作業で全部は要らない）。

## 凡例

- **【実績】** 参照実装（社内向けの Electron ツール）と、その運用 skill で確認した事実。**【実績: Web 参照実装】** と書いたものは、2 つ目の参照実装（Astro 7 + Starlight の OSS の docs サイトに、この skill の build を取り込んだ版と、その消化用 skill）で確認した事実。
- **【追加】** 汎用化のために足した設計・コード・手順。参照実装には無い。
- **【未検証】** 推測・一般知識・未検証。

## 具体パスは「参照実装では」の例

この skill の具体パス（`feedback/`・`feedback/archived/`・`.local/tmp/feedback/`・`yymmdd-xx.md`・`npm run check` など）は、すべて**参照実装の例**。読む Agent は自分の環境（アプリの種類・リポジトリ・ユーザの運用）に置き換える。

| 抽象語（仮称） | 意味 | 参照実装での具体例【実績】 |
|---|---|---|
| **フィードバック置き場**（Inbox） | 未対応のフィードバックが溜まる場所。1 件 = 1 記録（Markdown）+ 添付 | `<リポジトリ>/feedback/`（`config.yaml` の `feedback.dir`）。1 件 = `yymmdd-xx.md` |
| **対応済み置き場**（Archive） | 対応が終わった記録の移し先。**参照実装の状態は、記録が Inbox と Archive のどちらにあるかだけ** | `feedback/archived/` |
| **添付置き場** | スクリーンショットなど。記録には「参照」（パス・URL）だけを書く | `<リポジトリ>/.local/tmp/feedback/<YYYYMMDD-HHMMSS>/window.png, element.png`（Agent が自由に使ってよい一時ディレクトリ） |
| **Sink** | 上の 3 つを差し替えるインターフェース（採番・保存・添付・一覧・読み・アーカイブ）【追加】 | 参照実装は「ローカルのファイル」だけ（`src/main/feedbackFile.ts`・`feedbackShots.ts`）。これを `file-sink.mjs` にした |
| **Transport** | 画面 → 受け口 の送り方【追加】 | IPC（`app:feedback-save`） |
| **受け口** | Sink を持ち、文を組み立てて保存する側（置き場に書ける側）【追加】 | Electron の main（`buildFeedback`） |
| **履歴**（History） | 受けたフィードバックと対応結果の一覧（番号・要旨・位置・種類・結果） | 事後にまとめた表（自動では作られない） |
| **オーケストレータ** | ユーザの窓口の会話。受付・分類・起動・クローズ・報告だけ | メインの Claude Code の会話（`CLAUDE.md` の方針） |

「フィードバック置き場」より良い語があれば差し替えてよい（候補: 受信箱 / feedback inbox）。この skill では **置き場 = Inbox** で統一。

## 記録（1 件）の形

### 名前と採番【実績】

- id: `yymmdd`（今日のローカル日付。例: 2026-10-23 → `261023`）+ `-` + `xx`（その日の連番 2 桁、`01` から）。ファイルなら `<id>.md`。**ユーザの入力を名前に含めない**。
- 連番 = Inbox と Archive にある**同じ日付**の記録（空ファイルも）の最大 + 1。**番号を再利用しない**（archive 後も重ならない）。日付が変われば `01`。
- 書き込みは上書きしない（`wx`）。競合したら数え直して再試行（最大 10 回）。保存先はルート内（`..`・絶対パス・外を指すシンボリックリンクは拒否）。
- **Archive の番号を数え忘れると、同じ日に番号が重なる**【実績: Web 参照実装】（Inbox だけを数える実装で、archive した後の保存が同じ番号を使った）。さらに、数えてから書くまでの間に別のプロセスが同じ番号を保存して archive すると、`wx` は通るのに番号が重なる。書く前と書いた後に Archive に同じ名前が無いかを確かめ、あれば自分の分を消して数え直す（`file-sink.mjs`・`core-ts/file-sink.ts`。テストは `file-sink.test.mjs`・`sink-contract.test.ts`）【追加】。
- 連番の桁は導入先で決めてよい（Web 参照実装は 3 桁）。数えるときは 2 桁以上を全部読むので、後から桁を増やしても前の記録と重ならない。
- `.gitkeep`（空。フォルダを残すため）は対象外。0 バイトの記録は作成直後・エディタの未保存のことがある（実際に 3 回）。

### 作り方 3 通り

(a) アプリの「ファイルに追加」（GUI 形式。4KB〜100KB。以前はコピーをユーザーが手でファイルに貼っていた）/ (b) ユーザが手で書いた短いメモ（数行。GUI 形式ではない）/ (c) 会話にコピーを貼る（記録にならない。履歴には「（会話）」）。

### Markdown の構成【実績】

冒頭: `# <アプリ名>: 画面へのフィードバック` + **Agent への案内 1 行**（ソースの場所・ログの形。受け取る Agent が自力で調べ始められる）。オプションでオフにした節があれば「（オプションでオフ: …）」の 1 行（無いのは意図して省いたものと分かる）。続いて:

| 順 | 見出し | 中身 | オプション名（初期値） |
|---|---|---|---|
| 1 | フィードバックの文 | 引用ブロック。空なら「（書いていません）」 | 常に入る |
| 2 | 選んだ位置 | 画面・部品の道筋（`data-fb`）・要素の種類とラベル・セレクタ・矩形・クリック座標・ウィンドウ・スクロール・要素の状態 | 選択位置（オン） |
| 3 | スクリーンショット | `window.png`（選んだ部品を赤枠）と `element.png`（周り 24px）の参照 | スクリーンショット（オン。オフなら撮影もしない） |
| 4 | 環境 | 版・起動の種類・OS・ランタイム・パス | 環境（オフ） |
| 5 | 今の画面 | 画面名・日付・ダイアログ・ウィンドウ・編集中か・モード | 今の画面（オフ） |
| 6 | 画面に出ている情報 | **状態（モデル）から作る。DOM の文字は拾わない**。個人的な入力は字数だけ（本文は書かない） | 画面の状態（オフ） |
| 7 | ログ | 起動からのバッファ（古い順）。error/warn は先頭に要約。200KB 超は古い方から省く。debug の繰り返しは 1 行にまとめる | 操作ログ（オン） |

- 記録を schema 1 にする場合は、上の前に frontmatter が付く（次の節）。
- 秘密情報を伏せる redact を、組み立ての最後に文の全体へ 1 回（コピーと保存で同じ文）。範囲は `token=`・`password=`・`secret=`・`api_key=`・`sk-`・`ghp_`・`github_pat_` まで。
- 例: `assets/templates/feedback-file.example.md`。

### 記録の形 schema 1（frontmatter）【実績: Web 参照実装】

loop が全文を読まずに分類・グループ化できるよう、記録の先頭に frontmatter を付ける形。`buildFeedbackMarkdown` / `submitFeedback` に `meta` を渡したときだけ書く（渡さなければ上の旧形と同じ文）。定義: `assets/templates/feedback.schema.json` の `$defs.recordFrontmatter`、例: `assets/templates/feedback-file.schema1.example.md`。

| キー | 中身 | 書く条件 |
|---|---|---|
| `schema` | `1` | いつも |
| `kind` | 何を選んだか: `element`（要素 1 つ）/ `elements`（複数）/ `text`（テキストの選択）/ `page`（画面・ページ全体）/ `site`（アプリ・サイト全体） | いつも（送られなければ、部品を選んだら element、選ばなければ page） |
| `screen`・`target`・`source` | 画面・部品の道筋・ソースの行（`path:12-14`） | 分かるときだけ |
| `created` | 保存の時刻（ISO 8601） | いつも |
| `dirty` | 記録の時点で、ソースに未コミットの変更があった（行番号は作業ツリーのもの） | 受け口が git で確かめられたときだけ |
| `stale` | 画面を開いた後にソースが変わった（位置・文言がずれているかもしれない）。true なら本文の冒頭にも「注意:」の 1 行 | 受け口がソースの更新時刻と画面を開いた時刻を比べられたときだけ |
| `omitted` | 載せなかった節（オプションで外したもの） | いつも（空なら `[]`）。本文の「（オプションでオフ: …）」の 1 行と同じ中身 |
| `attachments` | 添付の参照 | 添付があるとき |
| `id` | `yymmdd-xx` | Sink が採番した id を文に渡せるときだけ（`core-ts` の `putEntry` は `text` に関数を渡せる） |

- 冒頭の **Agent への案内 1 行**（`pointer`）と**載せなかった節の一覧**は旧形と同じ位置に残す（frontmatter を読まない人・Agent にも分かるように）。
- 値は JSON の書き方（`"…"`・`[…]`・`true`）にする。YAML としても JSON としても読め、改行やコロンを含む値でも壊れない。
- **後方互換**: frontmatter の無い記録（旧形・手書きのメモ）は schema 0 として読む（`parseRecordFrontmatter`）。キーが無いのは「情報が無い」で、異常ではない。`dirty` / `stale` が無いのは「判定できなかった」で、「新しい」ではない。両方の形が同じ置き場に混ざってよく、loop は書き換えない。

### 状態遷移【実績】

ステータス欄は**無い**。`（作成）→ Inbox  [未対応]  →（対応・報告）→ オーケストレータだけが archive →  Archive  [対応済み]`。保留・判断待ちは Inbox に残す、または「判断待ち」と明記して archive する（ユーザに聞く）。判断待ちの一覧は報告に累積する。

## Sink の契約【追加】

置き場の実装を差し替えるためのインターフェース（コード: `assets/templates/core/feedback-core.mjs` の JSDoc。構築側の詳細は `references/build/architecture.md` §2.2）。

```
Sink { kind, capabilities{put,list,read,archive}
  putAttachments(files[{name,data,mime}], {now}) → { refs{name→参照}, dir, error }   先に呼ぶ。参照（パス・URL）が文に書かれる
  putEntry({text, now})        → { id, ref, state:'open', bytes, attempts }          採番して保存
  list({state:'open'|'archived'|'all'}) → EntryInfo[]                                 取り込み
  read(id)                     → { text, ...EntryInfo }                              取り込み
  archive(id, {note?})         → EntryInfo{state:'archived'}                         クローズ（参照実装の mv）。2 回目は何もしない
  writeDoc(relPath, text)      → void          任意（capabilities.doc）。置き場の中の文書を上書き（メモ）。採番しない・記録に数えない
  readDoc(relPath)             → text | null   任意（capabilities.doc）
}
```

規則（参照実装 `feedbackFile.ts` 由来【実績】）: id は日付 + 連番で archived の番号も数える（競合も含めて。上の「名前と採番」）／上書きしない／id・名前にユーザ入力を入れない／スクショ失敗は投げず理由を返す／保存の失敗は理由つきで投げる（画面が吹き出しに出してモードを続ける）。実装: ローカルのファイル（参照実装）・メモリ・HTTP【追加】。Issue トラッカー・クラウドストレージは概要のみ【未検証】（`references/build/architecture.md` §2.2）。

文書（`writeDoc` / `readDoc`）の規則【実績: Web 参照実装】: パスは置き場からの相対の正規形（`safeDocPath`。絶対パス・`..`・空・`\`・NUL は拒否）、ファイルではシンボリックリンクで外に出るものも拒否、tmp に書いて rename（読み手に書きかけが見えない）、`list` には出ない。TypeScript 版と vitest の契約テストは `assets/templates/core-ts/`。

## フィードバックとメモ【実績: Web 参照実装】

| | フィードバック | メモ（Memo） |
|---|---|---|
| 性質 | **一度送って閉じる**。1 件 = 1 記録。送った後は直さない | **書き溜めて何度でも直す**。ページごとに 1 ファイル（ページのメモ + 箇所ごとのメモ）。自動で保存し、次に開くと戻る |
| 置き場 | Inbox の `yymmdd-xx.md`（採番。Sink の `putEntry`） | Inbox の中の `memo/<言語>/<ページ>.md`（上書き。Sink の `writeDoc`）。記録には数えない |
| 状態 | Inbox → Archive（オーケストレータが archive） | 状態は無い。archive しない |
| 持ち主と読み手 | 利用者が書き、loop が消化する（対応して閉じる） | 利用者のもの。loop は**読むだけ**（意図の手がかり）。書き換えない |
| 向く使い方 | 「ここを直して」「これ大丈夫？」と、その場で 1 つ頼む | ページを読みながら、考え・評価（field: 読みやすさ・優先度など）・まだ頼むほどでない気づきを溜める |

- 使い分け: 対応してほしいことはフィードバックで送る。考えの途中・あとで見直す気づきはメモに書き、まとまったらフィードバックにする（メモから送る入口は導入先で決める）。
- loop はメモを記録の意図の手がかりに使い、記録とメモが食い違ったら新しいほうを優先して両方を報告する。記録の無いメモだけの指摘は直さず報告に並べる（`references/loop/loop.md` §2.4）。
- 作り方は `references/build/memo.md`。

## 参照実装の状況と、この skill が足したもの

- 参照実装での運用: ユーザーがアプリのフィードバックモードで書く → オーケストレータが運用 skill（`/feedback-loop`）で読み取って消化する。数週間・数十件の運用で、運用ルール・分類・落とし穴（`references/loop/lessons.md`）が固まった。
- この skill が足したもの【追加】: Sink / Transport / 受け口の抽象化、debug ビルド限定の前提と本番検査、送信 body のスキーマ、HTTP の受け口・ブラウザ/モバイル/Tauri のテンプレート、履歴を「クローズのたびに 1 行足す」運用、環境の判定（`scripts/detect.sh`）。
- Web 参照実装（Astro 7 + Starlight の docs サイト）で足したもの【実績: Web 参照実装】: Dev Toolbar に載せる形（`references/build/platforms.md` §2.1）、記録の schema 1、TypeScript の Sink と vitest の契約テスト、場所を一意に指す手がかり（`references/build/locate.md`）、popover・モードの外の入口・スマホ（`references/build/interaction.md`）、メモ（`references/build/memo.md`）、loop がメモを読む手順。
- 参照実装が扱っていない: 取り込みの自動トリガ（人の指示のみ）、ステータス欄、多言語。
- 参照実装の未決・実機未確認は `references/build/build.md` §4。
