# feedback の仕組み: 全体像・データフロー・保存形式

構築側（`references/build/`）の資料。凡例（【実績】【追加】【未検証】）・用語（フィードバック置き場 = Inbox / 対応済み置き場 = Archive / 添付置き場 / Sink / Transport / 受け口 / 履歴）・記録の形（名前・採番・Markdown の構成・状態遷移）・Sink の契約は **`references/concepts.md`** にある。先にそちらを読む。**この資料の具体パス（`feedback/` など）は参照実装の例で、自分の環境に置き換える。**

参照実装（Electron アプリ）のソース内パスは、そのアプリのソースルートからの相対（`SRC` = `src/`）。Agent 側の運用（取り込み・分類・クローズ）は `references/loop/loop.md`。

---

## 1. 参照実装の仕組みの要約【実績】

### 1.1 一言で

アプリ上でユーザが「気づいたこと」を書き、アプリが**文脈（どの部品か・状態・ログ・スクリーンショット）を自動で付けた Markdown 1 件**にして、フィードバック置き場（参照実装ではリポジトリの `feedback/yymmdd-xx.md`）に置く。Agent（Claude Code）が人の指示で置き場を読み、サブエージェントに振り分けて対応し、終わったら対応済み置き場（参照実装では `feedback/archived/`）へ移す。

### 1.2 入力 UI

| 入口 | 内容 | 根拠 |
|---|---|---|
| メニュー「フィードバック」（macOS メニューの一番右） | 「フィードバック情報をコピー」（部品を選ばない全体のコピー。いつも全節を入れる）と「フィードバックモードへ」（⌥⌘F） | `SRC/main/appInfo.ts`（`macMenuTemplate`・`FEEDBACK_MODE_ACCELERATOR`）、参照実装の README「フィードバック」 |
| フィードバックモード | 右上の帯「フィードバックモード — 要素をクリックしてコメント。Esc で終了」。ホバーで部品の輪郭と名前（`data-fb` の道筋）。クリックで吹き出し | `SRC/renderer/feedback.ts` |
| 吹き出し | 部品の名前・要素の説明・テキストエリア（自動フォーカス）・「親を選ぶ」「選び直す」「閉じる」・「オプション」（折りたたみ）・「ファイルに追加」「フィードバック情報をコピー」（⌘Enter） | 同上 |

モードの設計の要点（参照実装が苦労して決めたところ。汎用版でも守る）:

- **アプリの操作は一切させない**: 透明な全面オーバーレイがポインタを受け、`document.elementsFromPoint` で下の部品を探す。念のため window の capture フェーズで click・pointer・key・focus も止める（吹き出しと帯の中だけ通す）。プログラムからの `el.click()` も止まる。アプリのキーボードショートカットも止める。定期の再描画も止める。入力中だった欄は確定させずにフォーカスを外し、出たら戻す。
- 選ぶ単位: 操作できる要素（button・a・input・select・textarea・summary・role=button 等・img）はそのもの、ほかは `data-fb` のある最も近い祖先、どちらも無ければその要素（`SRC/renderer/elementInfo.ts` の `meaningfulTarget`）。
- 名前の道筋: 要素と祖先の `data-fb` を外側からつなぐ（`ヘッダ/保存ボタン`）。`data-fb` が無い要素は「祖先の道筋 › 要素の説明」。アプリ側で主な部品に `data-fb` を付けて回る（参照実装では画面側の 1 ファイルに約 200 箇所）。
- 打った文は部品ごとの下書き（アプリを開いている間は、モードを出ても残る。送ったら消す）。「選び直す」は文を次の部品に持っていく。「閉じる」はモードを終える（下書きは残る）。Esc は吹き出しを開いていても 1 回でモードを出る。
- 終わり方: コピー／ファイルに追加が成功したらトーストを出してモードを終える。失敗したら吹き出しに理由を出してモードを続ける。
- ダイアログ（`<dialog>` のモーダル）を開いているときは、モードの部品をそのダイアログの中に置く（モーダルの外は操作できないため）。

### 1.3 コンテキストの自動付与

吹き出しの送信（コピー・ファイルに追加）で、`FeedbackRequest { view, target, options }` を renderer → main に渡す（IPC `app:feedback-copy` / `app:feedback-save`。`SRC/shared/api.ts`・`SRC/main/preload.ts`）。main の `buildFeedback`（`SRC/main/main.ts`）が次をそろえる:

| 節（`FEEDBACK_SECTIONS`） | 見出し | 中身 | 既定 |
|---|---|---|---|
| （常に）フィードバックの文 | フィードバックの文 | 引用（`> `）。空なら「（書いていません）」 | 常に入る |
| `target` 選択位置 | 選んだ位置 | 画面 ID・hash・ダイアログ・`data-fb` の道筋・要素の種類とラベル・CSS セレクタ・矩形・クリック座標・ウィンドウの大きさ(dpr)・スクロール・要素の状態（押せるか・押せない理由・値…。`stateOf` がアプリ固有の状態を足す） | オン |
| `shots` スクリーンショット | スクリーンショット | `window.png`（選んだ部品を赤い枠で囲む）・`element.png`（周り 24px の切り抜き）のパス | オン |
| `log` 操作ログ | ログ | main のバッファ（起動からの直近 3000 件）。古い順。200KB を超えたら古い方から省く。error/warn は先頭に要約。debug の同じ繰り返し（3 回以上）は 1 行にまとめる | オン |
| `env` 環境 | 環境 | 版・起動の種類・Electron/Chromium/Node・OS・時刻・main から見たウィンドウ・各種パス・実行中の処理 | オフ |
| `screen` 今の画面 | 今の画面 | 画面名・hash・日付・処理の状態・ダイアログ・ウィンドウ・編集中か・表示モード・スクロール・ペインの幅 | オフ |
| `view` 画面の状態 | 画面に出ている情報 | **renderer の状態（モデル）から作る**（DOM の文字は拾わない）。フォーム・一覧・詳細画面・警告などアプリ固有の状態 | オフ |

- オフの節は見出しごと省き、冒頭に「（オプションでオフ: 環境・今の画面・…）」の 1 行を入れる（Agent が「意図して省いたもの」と分かるように）。選択は `<userData>/ui-settings.json` の `feedbackOptions` に覚える。スクリーンショットをオフにすると撮影もしない。
- 冒頭に **Agent への案内**を 1 行（「アプリ名のフィードバック。ソースは `…`、ログの形は README の「ログ」」）。→ 受け取る Agent が自力で調べ始められる。
- 最後に文の全体へ `redact`（Bearer・JWT・`Cookie:`・`_csrf=`/`token=`/`password=`/`secret=`/`api_key=`（値 6 文字以上）・`sk-`/`ghp_`/`github_pat_`）を 1 回掛ける（`SRC/main/util.ts`）。ログは書くときにも伏せている。
- ログ側の前提（フィードバックの価値を決める）: 画面の操作（`ui.click` に `data-fb` の道筋）・IPC 呼び出し・状態変化・エラーを、1 行 1 件の `key=value` で、相関 ID `actionId` つきで main の 1 か所に集めている（`SRC/main/log.ts`、参照実装の README「ログ」）。

### 1.4 保存・送信先（具体パスは参照実装の例。用語表で抽象語に読み替える）

| 出力 | 保存先 | 備考 |
|---|---|---|
| クリップボード | OS のクリップボード | ヘッドレスでは書かず、結果の `text` を返す（テストはこれを見る） |
| **ファイルに追加** | `<リポジトリ>/feedback/yymmdd-xx.md`（`config.yaml` の `feedback.dir`。リポジトリルートからの相対・ルート内のみ。既定 `feedback`） | `SRC/main/feedbackFile.ts`。下の 1.5 |
| スクリーンショット | `<リポジトリ>/.local/tmp/feedback/<YYYYMMDD-HHMMSS>/window.png`・`element.png`（環境変数で変更。同じ秒なら `-2`） | `SRC/main/feedbackShots.ts`。main の `webContents.capturePage()`。吹き出し・帯・強調は隠し、選んだ要素は renderer が枠で囲んでから撮る。`.local/tmp/` は「Agent が自由に使ってよい作業用一時ディレクトリ」（`CLAUDE.md`） |
| 吹き出しのオプションの記憶 | `<userData>/ui-settings.json`（`{"mode":…, "feedbackOptions":…}`） | `SRC/main/uiSettings.ts`。ヘッドレス・スモークでは読み書きしない |
| ログのファイル | `~/Library/Logs/<アプリ名>/main.log`（1MB で回して `main.1〜3.log`） | フィードバックには、バッファから作った分が入る |

### 1.5 記録（`yymmdd-xx.md`）の規則【実績】

名前・採番・作り方 3 通り・Markdown の構成は `references/concepts.md`「記録（1 件）の形」。参照実装の実装: `SRC/main/feedbackFile.ts`（`wx`・10 回再試行・パス検査）、コピーと保存は main の `buildFeedback` 1 か所を共有（`test/feedbackFile.test.ts` が同一性を確認）。

### 1.6 Agent への受け渡しと処理【実績】

拾うきっかけは人の指示（`/feedback-loop`）で、自動の監視・hook は無い。手順・分類・運用ルール・落とし穴は **`references/loop/loop.md`**（参照実装の運用 skill を汎用化したもの）。

### 1.7 状態遷移・クローズ【実績】

状態は「Inbox にあるか Archive にあるか」だけ（`references/concepts.md`「状態遷移」）。クローズの手順と履歴は `references/loop/loop.md`。

### 1.8 テスト・確認の仕組み【実績】

- 単体: `test/feedback.test.ts`（文の組み立て・節の出し分け・redact・ログの圧縮・吹き出しの置き方・スクリーンショットの切り抜き・メニュー）、`test/feedbackFile.test.ts`（採番・競合・保存先の検証・コピーと保存の同一性）。
- スモーク: `SRC/main/smokeFeedback.ts`（`--smoke-feedback`）。ヘッドレスで本物の入力（デバッガの `Input.dispatchMouseEvent`）を送り、「クリックしてもアプリの操作（ボタン等）が動かない」「コピー・保存の中身」「オプション」を確かめる。リポジトリルートが一時ディレクトリのときだけ「ファイルに追加」を押す（本物の `feedback/` に書かない）。
- ヘッドレスでは本物のクリップボードに書かない。

---

### 1.9 ログの作られ方と、feedback への添付【実績】

feedback の価値の多くは「ログ」節が決める（記録の約 67K 字がログという実例もある）。参照実装ではログは feedback とは**別の基盤**として作られ、feedback はそれを**読んで添付する消費者**になっている。

| 段 | 参照実装の中身 | 根拠 |
|---|---|---|
| 集約 | 出口は main の 1 か所（`SRC/main/log.ts`）。renderer は IPC `app:log`（fire-and-forget）で main に送り、main の処理と同じ順序で並ぶ（`SRC/renderer/uiLog.ts`） | 参照実装の README「ログ」 |
| 1 件の形 | `{seq, ts, level, src(M/R), cat, event, screen?, runId?, date?, actionId?, data}`。ファイルは 1 行 1 件の `key=value`（人も機械も読める。`parseLogLine` で読み戻せる）。例: `… info R ui.click screen=top actionId=a3 target=ヘッダ/保存ボタン` | `log.ts` |
| 相関 | 画面の 1 操作に `actionId`（`a<番号>`）を付け、続く 4 秒の IPC と画面のログにも preload が同じ ID を付ける | 参照実装の README |
| レベルと保存 | `debug`/`info`/`warn`/`error`。ファイルは info 以上（`~/Library/Logs/<アプリ>/main.log`、1MB で回して `main.1〜3.log`）、メモリのバッファは debug 以上で直近 3000 件。`config.yaml` の `log.level`/`bufferLevel`/`bufferSize` で変える | `log.ts`・README |
| 秘密 | 呼ぶ側で渡さない（IPC の引数は `ipcLog.ts` で要約: 依頼文・CSV は長さだけ、hash は先頭 8 文字、URL はクエリなし）+ キー名（`token`/`cookie`/`csrf`/`authorization`/`password`/`secret`/`api_key`/`credential`/`env`）で値を `***` + 文字列中身を `redact`。ユーザが書いた文は 300 文字で切る | `log.ts`・参照実装の README「秘密の扱い」 |
| feedback への添付 | main の `buildFeedback` が `log.entries()` と `log.stats()` を取り、`feedbackMarkdown` が ①古い順に並べる ②200KB 超は古い方から省き、省いた件数とログファイルの場所を書く ③error/warn を先頭に要約（新しい 40 件・24KB まで）④**debug の同じ繰り返しを 1 行にまとめる**（`compressLogEntries`: 出どころ・区分.イベント・主なキーが 3 回以上なら `21:08:31–21:59:25 debug R ui.refresh × 102（what=list）`。info 以上と `ok=false`/`code`/`error`/`msg` のある debug はまとめない。**まとめるのはコピーの文だけで、ログのファイル・バッファは変えない**。実例で 90,286 字 → 6,418 字）⑤文の全体に `redact` を最後に 1 回 | `SRC/main/feedbackText.ts`・`main.ts` |
| feedback 自身のログ | `cat=feedback`（`enter`/`exit`/`select`/`copy`/`saved`/`save-failed`/`shots`/`file-saved` …）。文は書かず、パス・バイト数・字数だけ | 参照実装の README「出すイベント」 |

つまり参照実装で feedback 側が持つのは、**ログの「圧縮・件数制限・error/warn の要約・最後の redact」という添付の整形**だけ。収集・形式・保存・ローテーション・相関 ID・一次の redact はログ基盤の仕事。

## 2. 汎用化した設計【追加】

プラットフォーム非依存のコア概念。参照実装の各要素を、「アプリが提供する部分」と「共通で使える部分」に分けた。

```
┌ アプリ（画面側）─────────────────────────────────────────────────────┐
│ ①入力 UI: 入る手段(メニュー/ホットキー/FAB/シェイク) → モード(操作を止める) → 部品を選ぶ → 吹き出し │
│ ②コンテキスト収集: 部品の道筋・要素・矩形・状態 / 画面 ID / 画面の状態(モデルから) / ログ / スクショ │
└──────────────┬──────────────────────────────────────────────────────┘
               │ ③送信 body（FeedbackSubmission。assets/templates/feedback.schema.json）
               ▼
┌ 受け口（Sink に書ける側。Electron main / dev サーバ / Tauri Rust / CI 側 …）─────────────┐
│ ④組み立て: 節の出し分け → Markdown → redact（1 か所。コピーと保存で同じ文）            │
│ ⑤保存 = Sink（置き場）: putAttachments → putEntry（採番・上書きしない・対応済みの番号も数える）            │
│     参照実装: feedback/yymmdd-xx.md + .local/tmp/feedback/<日時>/*.png（file-sink）                │
└──────────────┬──────────────────────────────────────────────────────┘
               ▼ 人の指示 or トリガ（⑥）
┌ Agent 側 ────────────────────────────────────────────────────────────┐
│ ⑥取り込み(list/read) → ⑦トリアージ(実装/調査/設計) → ⑧対応 → ⑨反映(docs+テスト) → ⑩クローズ(archive)+履歴 → 報告 │
└──────────────────────────────────────────────────────────────────┘
```

| # | コア概念 | 参照実装 | 汎用化で足した・変えた点 |
|---|---|---|---|
| ① | 入力 UI | メニュー + ⌥⌘F。全面オーバーレイ・吹き出し | 入る手段をプラットフォームごとに差し替え可（FAB・シェイク・長押し・ホットキー）。モバイルは「ホバー」が無いので、タップ→吹き出しの 2 段のみ【追加】 |
| ② | 部品の命名 | `data-fb` を手で付ける | `FbTarget`（RN/Flutter）、`testID`/`accessibilityIdentifier`/`Semantics`（ネイティブ）への対応表【追加】。付ける優先度の指針（主要な部品→全部）【追加】 |
| ② | 状態の収集 | 画面ごとの `feedbackView()`（モデルから作る・DOM の文字は拾わない） | 汎用版は `getView()` の任意オブジェクトを key-value で出すだけ。アプリ固有の整形（`FeedbackPunch` など）は各アプリで書く【追加】 |
| ② | ログ | main の `log.ts` に集約・`actionId` で相関（§1.9） | **ログは feedback と別タスク**（§2.5）。feedback は `getRecentLogs` の差し込み口で既存のログ基盤を読むだけ。ログの質がフィードバックの価値を決める（操作・IPC/API・状態変化・エラーを 1 か所に集める）ので、参照実装並みにするならログ基盤の別 plan で【追加】 |
| ② | スクリーンショット | main の `capturePage` | Web: html2canvas 等、モバイル: view-shot / RepaintBoundary / PixelCopy…。いずれも「オーバーレイを隠し、選んだ要素を枠で囲んでから撮る」順序は同じ【追加】 |
| ③ | 送信（Transport） | IPC（renderer → main） | HTTP（dev サーバ）・共有シート等も許可。どれでも body は同じ【追加】 |
| ④ | 組み立て | `feedbackText.ts`（アプリ固有の節が大きい） | `feedback-core.mjs` の `buildFeedbackMarkdown`（env/screen/view は key-value の汎用整形）【追加】。ログ圧縮（debug の繰り返し 1 行化）は未移植 |
| ⑤ | 保存（Sink） | `feedbackFile.ts`・`feedbackShots.ts`（ファイル直書き） | **Sink インターフェース**に分け、ファイル実装 `file-sink.mjs`・メモリ・HTTP を用意。契約テストで 3 実装が同じ結果になることを確認済み【追加】（§2.2） |
| ⑥ | 取り込み | 人の指示で置き場を `ls` / `cat` | Sink の `list` / `read` に対応づけ（`references/loop/loop.md` の Sink ごとの読み方）。自動化（ファイル監視・hook・`/loop`・スケジュール）は選択肢のみ【追加】【未検証】 |
| ⑦〜⑩ | Agent ループ | `feedback-loop` スキル | `references/loop/` に汎用化（アプリ固有の確認コマンド・禁止事項を穴埋めに）【追加】 |
| — | ステータス | 場所のみ（置き場 → 対応済み置き場）。| Sink の `state: open/archived` として一般化。front matter の `status` 拡張は**採用しない既定**【追加】 |
| — | 履歴 | 無し（事後にまとめた表） | クローズ時に 1 行足す手順にした【追加】（`references/loop/loop.md`） |
| — | 本番での無効化 | 概念なし（個人用ツール。パッケージ版にも入っている） | **debug ビルドだけに配線し、本番に入れない**を前提にする【追加】（§2.3・`platforms.md`） |

### 2.1 データ形式の位置づけ

- **Markdown の構成・節の順・オプションの名前と初期値・redact の範囲は、参照実装とこの skill の `buildFeedbackMarkdown` で一致している**（順: 文 → 選んだ位置 → スクリーンショット → 環境 → 今の画面 → 画面に出ている情報 → ログ。オプション名: 選択位置・操作ログ・スクリーンショット・環境・今の画面・画面の状態。初期値: 前 3 つオン・後 3 つオフ）。差分: 参照実装の節の中身（アプリ固有の画面の状態など）はアプリ固有で、汎用版は key-value の汎用整形。ログ圧縮は未移植。
- 参照実装のメニュー「フィードバック情報をコピー」（モードに入らない・選んだ位置もスクリーンショットも無く・オプションに関わらず全節）は、汎用版では「部品を選ばない送信」（`target` 無し）として `schema` が許す。UI の入口は各アプリで作る。
- 送信 body（`feedback.schema.json`）は汎用版の独自定義。参照実装は型（`FeedbackRequest` など、`SRC/shared/feedback.ts`）。【追加】
- 「view には本文を書かない」流儀: 画面の状態にはメモの**字数だけ**を書き、本文は書かない。ログも「ユーザが書いた文は出してよいが 300 文字で切る」【実績】。個人的な入力は字数に。
- Markdown の見出しは日本語。別言語にするなら Agent 側の抜粋コマンド（`grep '## フィードバックの文'`）も合わせて変える。

### 2.2 Sink（フィードバック置き場の実装）の設計【追加】

参照実装は置き場が「ローカルのファイル」固定だった。保存先はアプリの種類・ユーザの環境（スマホ実機はリポジトリに書けない、など）で変わるので、次のインターフェースに分けた（実装: `assets/templates/core/feedback-core.mjs` の JSDoc と `submitFeedback`）。

インターフェース（`putAttachments` → `putEntry`・`list`・`read`・`archive`、`capabilities`）は `references/concepts.md`「Sink の契約」。

Sink が守る規則（参照実装 `feedbackFile.ts` 由来【実績】）: ① id は日付 + 連番（`yymmdd-xx`）で、**対応済みの番号も数えて再利用しない** ② 上書きしない・競合したら採番し直す ③ id・名前にユーザ入力を入れない（パス区切りを含められない）④ スクショ失敗は投げずに理由を返す（文に「撮れませんでした」と出る）⑤ 保存の失敗は理由つきで投げる（画面が吹き出しに出してモードを続ける）。redact は Sink ではなく `buildFeedbackMarkdown` の 1 か所（コピーと保存で同じ文）。

| 実装 | 状態 | 概要 |
|---|---|---|
| **ローカルのファイル** `file-sink.mjs` | 【実績】の写し。node:test と契約テストで確認済み | `<リポジトリ>/feedback/yymmdd-xx.md` + `feedback/archived/` + 添付は `.local/tmp/feedback/<日時>/`。`archive` = `rename`（参照実装の mv）。保存先はルート内・シンボリックリンクで外に出ない。`repoRoot` / `dir` / `attachDir` で読み替える |
| **メモリ** `memory-sink.mjs` | 【追加】テスト・モックアップ用 | 契約テストの基準 |
| **HTTP**（同じ LAN の開発機の受け口へ） `http-sink.mjs` + `web/feedback-dev-server.mjs` | 【追加】Node 同士・localhost の契約テストは確認済み。**LAN 越し・スマホ実機は【未検証】** | 受け口が裏で別の Sink（通常は file-sink）を持つ。`POST /__feedback/attachments` `POST /__feedback/entries` `GET /__feedback/entries[/:id]` `POST /__feedback/entries/:id/archive`。LAN に出すなら `FEEDBACK_TOKEN`（ヘッダ `x-feedback-token`）必須。受け口でも redact を掛ける。**画面側が JS で文を組み立てられないとき**（Flutter・ネイティブ）は、HTTP Sink ではなく submission（`POST /__feedback`、生の payload + base64 画像）を送り、受け口が `submitFeedback` する |
| Issue トラッカー（GitHub Issues・Jira 等） | 【未検証】概要のみ | `putAttachments` = 添付アップロードまたは別ストレージの URL（Issue API は画像を直接受けないものが多い）、`putEntry` = Issue 作成（id = Issue 番号か `yymmdd-xx` をタイトルに）、`list`/`read` = Issue 検索・取得、`archive` = クローズ + ラベル。採番は tracker に任せるので `yymmdd-xx` は付けない選択もある。認証トークンは受け口（サーバ側・CI）にだけ置き、画面側に出さない。個人情報・ログが外部サービスに出る点の同意が要る |
| クラウドストレージ（S3・GCS・Drive 等） | 【未検証】概要のみ | `putEntry` = オブジェクトを `inbox/<id>.md` に作成（条件付き書き込み = `If-None-Match` で上書きなしを担保）、`archive` = `archived/` 配下へコピー + 削除、`list` = prefix 一覧。署名付き URL で添付。Agent は同じ SDK か CLI で読む |

**Agent 側のループ（取り込み・クローズ）は Sink に依存する**。ファイルなら `ls`/`cat`/`mv`、HTTP なら受け口の置き場を直接読むか `list`/`read`/`archive`、Issue ならトラッカーの検索・クローズ。詳細と対応表は `references/loop/loop.md`。**参照実装の運用（`feedback-loop` スキル）はファイル専用に書かれている**ので、他の Sink では読み替える。

### 2.3 debug ビルド限定（本番に入れない）【追加】

参照実装には無い前提。フィードバックモードは**開発・検証用の道具**で、スクリーンショット・ログ・画面の状態を集めて外へ送るため、本番アプリに入ると個人情報・権限・攻撃面の問題になる。プラン・実装の最初に「どう本番から外すか」を決める（`references/build/build.md` の導入手順 2）。

- 外すもの: ① コード・UI（モード・吹き出し・FAB・メニュー項目・ホットキー）② 送信先（URL・鍵・ポート。ビルド変数にも）③ 依存（html2canvas・view-shot 等）④ 権限・設定（スクショ・ネットワークの許可、Android の `usesCleartextTraffic`・iOS の ATS 例外、CSP の `connect-src`）⑤ 受け口（開発用サーバ・IPC ハンドラ）。
- 二重に守る: (1) **ビルド時に消える**（dead code elimination・debug 専用の source set・別 entrypoint）が主。(2) 実行時の `enabled`（`isDev` / `kDebugMode` / `__DEV__`）は保険。(3) **本番成果物を grep で検査**（`scripts/check-no-feedback-in-prod.sh`）して、CI に入れる。
- プラットフォーム別の具体策は `platforms.md` §0.1。
- 本番ユーザー向けに**意図して**入れたい場合は、これとは別の判断（同意・個人情報・送信先・保存期間・オプトイン。`references/build/build.md` の未確定事項）。この skill のテンプレートはその用途を想定していない。

### 2.4 履歴（History）【追加】

対応結果の一覧（参照実装では事後にまとめた表）。形と、クローズのたびに 1 行足す運用は `references/loop/loop.md`「履歴」。

### 2.5 ログ基盤は feedback から切り出す【追加】

**方針**: feedback mode はログを「読んで添付するだけ」の消費者にとどめる。ログの収集・形式・保存・ローテーション・圧縮・redact・本番での扱いは**別タスク（別 plan・別 PR）**として検討・実装する。既存のログ基盤があればそれを読む（既存のデバッグ基盤に乗るのと同じ考え方）。無ければ、feedback 側には差し込み口 `getRecentLogs()`（`[{ts, level, text}]`、古い順）と、feedback 自身の動きを流す任意の `logEvent(level, text)` だけを置き、中身は後回しか別タスクにする。テンプレートはこの口しか持たない（`assets/templates/web/log-buffer.example.js` は暫定の例で、feedback の一部ではない）。

**なぜ分けるか**（参照実装の構造【実績】と一般論【追加】）:
1. **本番でも要る基盤だから**: ログは障害調査・サポートのため本番にも要る。feedback は debug 限定（§2.3）なので、同じ部品にすると、本番に残すべきログまで一緒に消すか、feedback が本番に紛れる。参照実装でも、ログ（`log.ts`）はパッケージ版でも常に動き、feedback はそれを読む側だった。
2. **ライフサイクルが違う**: ログは常時動き、回り（1MB × 3）、設定（レベル・バッファ）で調整される。feedback は人が押したときだけ読む。変更の頻度・レビューする人も違う。
3. **個人情報・容量の論点が独立**: 何を書かないか（本文は字数だけ・URL のクエリを出さない）、保存期間、ファイルの大きさ、バッファの上限は、feedback とは別に決める必要がある。feedback の redact は「最後の保険」で、一次の防御はログ基盤が持つ。
4. **圧縮などの整形はログの性質に依存する**: `compressLogEntries` は参照実装のログの形（`cat.event`・`ch`/`subtype`/`name`・`ok`/`code`）に合わせて作ってある。ログの形が違えば作り直しになるので、先にログの形を決める。
5. **テスト・差し替えがしやすい**: 差し込み口があれば、feedback はログ基盤が何でも（自作・Sentry breadcrumb・OS のログ・`console`）動く。

**plan での扱い**: `references/build/build.md` 手順 1c。参照実装並みの「操作・IPC/API・状態変化・エラーを 1 か所に集める」ログを作る場合は、feedback の plan とは別に立てる。それまでは `getRecentLogs` を省略（ログ節は空になり、冒頭の「（オプションでオフ…）」ではなく「ログ: 0 件」と出る）してよい。

**参照実装のログ圧縮は未移植**: 上の方針に沿って、`feedback-core.mjs` には持ち込んでいない。ログ基盤を作る別タスクの中で、そのログの形に合わせた圧縮（件数制限・繰り返しの 1 行化）を検討する。`buildFeedbackMarkdown` が今持っているのは、新しい方から 200KB までの件数制限と error/warn の要約（最大 40 件）と最後の redact だけ。

## 3. 参照実装の既知の制約・未検証【実績】

- フィードバックモード中、右上の帯が他の UI（モード切り替えトグルなど）を覆って選べない。
- 「ファイルに追加」のあとモードを続けるか（今は終えて下書きも消える）。
- redact を `token=`・`password=` まで広げた。普通の文を伏せすぎないか未確認。
- 開発起動で main が古いと、画面側だけ新しい食い違いが起きる（質問型フィードバックの多くがこれ）。警告に一言を出すだけで、版のずれの検出は無い。
- 日付が変わると連番が 01 に戻る挙動は単体テストのみ（実機未確認）。
- `data-fb` の一覧 docs が部品の移動に追従しているか。
- モード中の帯の位置、「選び直す」「閉じる」の文の扱い、ログの形（`key=value`、JSON Lines ではない）、「情報をコピー」のショートカット、`.local/tmp/feedback/` のスクリーンショットの自動削除（**今は自動で消さない**）、`window.focus/blur` のログレベル。
- 実機で確かめる点: オプションを開いたときの見た目（最小幅・画面端）・Tab と Space の操作感・選択が再起動後も残るか、「ファイルに追加」で実際の置き場に記録ができトーストにパスが出ること、本物のクリップボード、トーストがダイアログを覆うこと。ヘッドレスのスモークでは確かめていない。
- フィードバックの対応で出たユーザーの判断待ちは、会話の「返事待ち」の一覧に累積する運用（`references/loop/loop.md` §3）。
