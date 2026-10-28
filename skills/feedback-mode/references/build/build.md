# 構築（build）: アプリに feedback mode を作って入れる

アプリ上で部品を選んでコメントすると、画面・状態・ログ・スクリーンショット付きの記録がフィードバック置き場に溜まる仕組みを、新しいアプリに入れる。**実践（溜まった記録の消化）は `references/loop/loop.md`**（ここでは読まない）。

先に `references/concepts.md`（凡例・用語・記録の形・Sink の契約）を読む。**具体パス（`feedback/` など）は参照実装の例で、自環境に置き換える**。パスは skill ルート基準（同じフォルダの `architecture.md` などは単に名前で書く）。詳細は必要になったときだけ: `architecture.md`（参照実装の仕組みと汎用設計・Sink・debug 限定の理由）、`platforms.md`（環境別の実装指針と本番から外す具体策）、`assets/templates/`（コード）、`scripts/`（本番検査）。

> 導入先のコードを変更するので、**冒頭の前提と §1 の手順 2（debug 限定の判断）・手順 3（方式）を、書き込む前にユーザに示して承認を取る**。

## 前提: debug ビルドだけに配線する【追加】（参照実装には無い）

feedback mode は開発・検証用の道具で、スクショ・ログ・画面の状態を外へ送る。**本番アプリには入れない**。プランを組む段階（§1 の手順 2）で必ず「debug 限定にするか・どう本番から外すか」を決め、作るときも常にこの前提を守る。本番ユーザー向けに意図して入れたい場合は別の判断（同意・個人情報・送信先。§4 の末尾）として分ける。

## 1. 手順

1. **調べる**: アプリの種類（Electron / Tauri / Web / RN / Flutter / iOS / Android）・UI 層（DOM の有無）・ログの仕組み・正本 docs（README / `CLAUDE.md`）・テスト/確認コマンド・ヘッドレスで動かす方法・ビルドの種類（debug / release、環境変数）・`.gitignore`・Inbox と添付置き場になる場所が既にあるか。
1b. **（plan）既存のデバッグ基盤を探す【追加】（原則: あれば、そこに載せる）**: `scripts/detect.sh` の「5. 既存のデバッグ基盤」の出力と、`platforms.md` §0.2 の探し方（grep のキーワード・環境別の例）で、アプリ内の debug menu・開発者設定画面・隠しジェスチャ・feature flag・内製の dev panel・Dev Menu / debug drawer などを探す。
   - **見つかったら**: 新しい入口（メニュー・ホットキー・FAB）も新しいガードも作らない。その基盤の入口（メニュー項目・パネルのタブ・ジェスチャ・ショートカット）に「フィードバックモードへ」を1つ足し、**その基盤がすでに持っている本番除外の仕組み**に乗る。最小例: `assets/templates/existing-debug/`。載せる前に `platforms.md` §0.2 の注意（本当に本番から外れているか・UI 規約・権限・スクショ/ログの再利用）を確認し、外れていない基盤はユーザに指摘して確認する。
   - **見つからなければ**: 以降の手順どおり新規に作る。
   - 見つかった場合も、手順 2 の「外すものの一覧」と確認方法（`check-no-feedback-in-prod.sh`）は省かない（基盤が外れていても、足した部分が残っていないかを確かめる）。
1c. **（plan）ログ基盤の扱いを決める【追加】（feedback と別タスクにする）**: feedback mode はログを**読んで添付するだけ**の消費者にとどめる。ログの収集・形式・保存・ローテーション・圧縮・redact・本番での扱いは、**別タスク（別 plan・別 PR）**として検討・実装する（理由: `architecture.md` §2.5 — 本番でも要る基盤・ライフサイクルが違う・個人情報と容量の論点が独立）。
   - **既存のログ基盤がある**（`platforms.md` §0.3 の探し方、`detect.sh` の「6. 既存のログ基盤」）: それを読む。`getRecentLogs` にそのバッファ/ファイルの読み出しを繋ぐ。ログの形が分かる案内を、記録の冒頭の `pointer` に書く。新しいログ機構を feedback 側に作らない。
   - **無い**: feedback 側は差し込み口（`getRecentLogs` / `logEvent`）だけ置く。ログ節は空でよい（後から繋げる）。ログ基盤は別 plan にする。暫定が要るなら `assets/templates/web/log-buffer.example.js`（console のフック。本番に持ち込まない）。
   - 参照実装のログ圧縮（debug の繰り返しの 1 行化）は未移植。ログの形が決まってから、ログ基盤側の検討に含める。
2. **（plan）debug 限定の判断【追加】**: 次を決めて、プランに書く。
   - どのビルドに入れるか（開発 / ステージング）。本番（リリース・パッケージ版・ストア配布）には**入れない**。
   - どう外すか: `platforms.md` §0.1 のその環境の行（DCE + 動的 import / `app.isPackaged` / `#if DEBUG` / debug source set / 別 entrypoint など）。**主はビルド時に消すこと**、実行時の `enabled` は保険。
   - 外すものの一覧: コード・UI・送信先（URL・鍵）・依存（html2canvas・view-shot 等）・権限/設定（スクショ・ネットワーク・cleartext・ATS・CSP）・受け口。
   - 確認方法: 本番ビルドの成果物を `scripts/check-no-feedback-in-prod.sh` で検査し、CI に入れる。
3. **方式を決める**: `platforms.md` の差分表から、部品の選択・スクショ・**Transport**（IPC / HTTP / 共有シート）・**Sink**（ローカルのファイル（既定）/ HTTP の受け口 / Issue / クラウド。`architecture.md` §2.2）を選ぶ。置き場（Inbox・Archive・添付置き場）の場所を決める。決めた内容をユーザに 1 画面で示す。フレームワークが dev 用の toolbar を持つ Web（Astro の Dev Toolbar）なら、それを入口・Transport・本番除外にする（`platforms.md` §2.1）。ページと箇所ごとに書き溜めるメモ（`memo.md`）も作るかを、ここで決める。
4. **コアを置く**: `bash "${CLAUDE_SKILL_DIR}/scripts/copy-core.sh" <導入先の dev 専用ディレクトリ>`（`assets/templates/core/` の `feedback-core.mjs`・`file-sink.mjs`・`memory-sink.mjs`・`http-sink.mjs` と `*.test.mjs` を `<導入先>/core/` に、契約テストが使う `web/feedback-dev-server.mjs` を `<導入先>/web/` にコピーし、`node --test` が通ることを確認する（32 件。1 件は HTTP の Sink が文書を持たないので skip）。script はそのまま実行する。入力は導入先のパスだけ。既存ファイルがあれば中断するので、上書きは `--force`）。Node が無い環境（Tauri/Rust 等）は規則（`references/concepts.md`）を移植して同等のテストを書く。TypeScript の導入先は `assets/templates/core-ts/`（型付きの Sink とメモリ版・vitest の契約テスト）を手でコピーしてもよい（`architecture.md` §2.2）。**導入先は dev 専用のディレクトリにする**（本番ビルドから除外しやすいように）。
5. **受け口を置く**: Electron は `assets/templates/electron/main-feedback.mjs`（`isDev` が true のときだけ登録）、それ以外は `assets/templates/web/feedback-dev-server.mjs`（`FEEDBACK_REPO_ROOT` を導入先に。裏の Sink を `createHandler({ sink })` で差し替え可）。LAN に出すなら `FEEDBACK_TOKEN` 必須。本番では起動・登録しない。
6. **画面側を置く**: Astro は `assets/templates/web/astro/`（統合 + toolbar アプリ。`platforms.md` §2.1）。ほかの Web 系は `assets/templates/web/feedback-entry.dev.js`（開発ビルドの入口。動的 import）→ `feedback-mode.js`、RN/Flutter は `assets/templates/mobile/*`（別 entrypoint / `__DEV__` require）。`appName`・`pointer`（Agent への案内: ソースの場所・ログの形・正本 docs）・`transport`・`getScreen`・`getView`（画面の状態をモデルから作る。DOM の文字は拾わない。個人的な入力は字数だけ）・`stateOf`（押せるか・押せない理由などアプリ固有の状態）を埋める。送信先の URL・鍵は開発用のビルド変数から（既定値は持たない）。
6b. **場所の手がかり（Web）**: XPath・CSS セレクタは保存の前に引き直して同じ要素に戻ったものだけを書く。テキストはテキストフラグメントの URL。Markdown 由来のページならソースの行も（`locate.md`）。操作（popover・右クリックの入口・ページ全体・スマホ）は `interaction.md`。
7. **部品に名前を付ける**: 主要な 20〜30 箇所から `data-fb`（等）を付け、以後フィードバックが来た部品から増やす。道筋は「画面/領域/部品」の日本語でよい。**行・項目は番号でなく内容で名付ける**（例: 表の行は「3 行目」でなく「行 <時刻 種類>」と内容で名付ける）。部品を移動したら名前の道筋も追従する（`data-fb` の一覧 docs を作った場合はそれも）。
8. **ログを繋ぐ**（作るのは別タスク。手順 1c）: `getRecentLogs` を既存のログ基盤に繋ぐ。画面操作（click の道筋）・API/IPC 呼び出しと結果・状態変化・エラーが入っていないと、フィードバックから原因をたどれない【実績】ので、足りなければログ基盤の別タスクで補う。秘密を出さない一次の防御もそちら（feedback の `redact` は文の最後の保険）。
9. **スクショを確認**: 吹き出し・帯を隠し、選んだ要素を枠で囲んで撮れているか。dpr と切り抜き（`cropRect`）が合っているか。撮れない環境では理由が文に出るか。
10. **動作確認（ヘッドレス・偽データ）**: 入る → 部品にホバー → クリック → 書く → 送信 → Inbox に記録ができる → **下の要素のクリックが届いていない** → Esc で出る。実機のクリップボード・本物のウィンドウ・本番データには触れない（参照実装のスモークは、ルートが一時ディレクトリのときだけ保存を押す）。**本番ビルドを作り、`scripts/check-no-feedback-in-prod.sh` で何も残っていないこと**を確認する。
11. **loop 側を置く**: 導入先に実践の入口を作る — `references/loop/loop.md` §0 の表を自環境の値で埋めて、`.claude/skills/feedback-loop` 相当（または導入先の運用 docs）にする。`CLAUDE.md` に「置き場の場所・オーケストレータの役割（受付だけ）・サブエージェントに任せる」を 1〜2 行足す。
12. **試運用**: 1 件実際に送り、loop 側の手順で処理して archive まで通す（履歴に 1 行足す）。
13. **正本 docs に書く**: 入り方・節と既定・Inbox/Archive/添付置き場・名前の規則・**debug 限定にした方法と検査コマンド**・既知の制約。

## 2. チェックリスト

- [ ] **本番ビルドに feedback のコード・UI・送信先（URL・鍵）・依存（html2canvas 等）・権限（スクショ・ネットワーク）が含まれていないことを確認した**（`scripts/check-no-feedback-in-prod.sh` + 権限・依存の宣言の目視。CI に入れた）
- [ ] ログ基盤は feedback と別タスクにした（feedback は `getRecentLogs` で読んで添付するだけ。ログの収集・形式・保存・圧縮・redact・本番での扱いを feedback 側に作っていない。既存のログ基盤があればそれを読んでいる）
- [ ] 既存のデバッグ基盤（debug menu・Dev Menu・dev panel・feature flag 等）を探した。あればその入口とその本番除外の仕組みに乗せ（新しい入口・ガードを作っていない）、その基盤が本当に本番から外れていることを確認した（外れていなければユーザに指摘・確認した）
- [ ] 配線は debug 限定のガード付き（ビルド時に消える + 実行時の `enabled`）。静的 import が残っていない
- [ ] 送信先の URL・鍵・`FEEDBACK_TOKEN` は開発用の設定にだけあり、コミットされておらず、本番の設定・CI の本番ジョブに渡っていない
- [ ] 操作を止める: クリック・キー・フォーカス・ホイール以外のジェスチャ（戻る・シェイクなど）。ダイアログ/モーダルの中も選べる
- [ ] 入る前のフォーカス・入力中の欄を壊さない（確定させず、出たら戻す）
- [ ] 失敗したら吹き出しに理由を出してモードを続ける。成功したらモードを終える
- [ ] 文が空でも保存できる（「（書いていません）」）。下書きは部品ごとに残り、送ったら消える
- [ ] 記録の名前にユーザ入力を含めない・上書きしない・Archive の番号も数える（数えてから書くまでの競合も）・保存先がルート外に出ない（Sink の契約テストが通る）
- [ ] 記録を schema 1（frontmatter の `kind`・`omitted`、分かれば `dirty`・`stale`）にするなら、loop が旧形（frontmatter なし）も読めるようにした（`references/concepts.md`「記録の形 schema 1」）
- [ ] Web: XPath・CSS セレクタは引き直して確かめたものだけを書いている。スマホのブラウザで使うなら、タップを pointerup で判定し、600px 以下はシート（16px・44px・`visualViewport`）にした（`interaction.md`）
- [ ] メモを作ったなら: 置き場は Sink の `writeDoc`（記録に数えない）、ページのパスから作るファイル名を検査している、loop がメモを読む（書き換えない）ように §11 の入口に書いた（`memo.md`）
- [ ] redact は組み立ての 1 か所で、入れた節すべてに。パスワード欄の値と個人的な入力の本文を集めない（字数だけ）
- [ ] 添付（スクショ）を Agent が読める場所に置き、冒頭の案内に参照を書く。省いた節は「（オプションでオフ: …）」で分かる
- [ ] 受け口が localhost 以外から書き込めない（LAN 越しにするときは作業中だけ + トークン）
- [ ] ヘッドレスでテストでき、本物の置き場・実データに書かない
- [ ] loop 側: 置き場の場所・確認コマンド・禁止事項を埋めた。archive はオーケストレータだけ・サブエージェントは全文を読む・判断待ちを累積する

## 3. 検証状況（このテンプレートで実際に確かめたこと）

確認済み: `assets/templates/core/`（node:test 32 件。Sink の契約をメモリ・ファイル・HTTP に掛けた。schema 1・writeDoc・採番の競合を含む）、`core-ts/`（vitest 28 件）、`web/astro/`（Astro 7 の最小プロジェクトの dev と build。`platforms.md` の冒頭）、`web/feedback-mode.js` + `feedback-dev-server.mjs`（ヘッドレス Electron）、`scripts/check-no-feedback-in-prod.sh`（偽の成果物）。**未検証【未検証】**: `existing-debug/`（RN Dev Menu・Electron 開発用メニューの 1 項目の例）・Electron の配線（`main-feedback.mjs`）・Tauri・RN・Flutter・iOS/Android の断片・html2canvas でのスクショ・LAN 越しの HTTP・スマホ実機・各環境の「本番から外す」ビルド設定（`platforms.md` §0.1）。

## 4. 構築側の既知の制約・未検証事項

- 送信 body のフィールド名（`feedback.schema.json`）は汎用版の独自定義。参照実装は型で固定していたので、既存実装に合わせる場合は揃える。
- 取り込みの自動トリガ（受け口の通知・`/loop`・hook）は参照実装も人の指示のみ。
- 参照実装で残っていた未決・実機未確認の点: 帯が他の UI（モード切り替えトグルなど）を覆う、「ファイルに追加」後もモードを続けるか、redact が普通の文を伏せすぎないか、開発起動で古いプロセスを検出する仕組みが無い、日付またぎの連番リセットの実機未確認、`data-fb` 一覧 docs の追従、スクリーンショットの自動削除（今は消えない）、`window.focus/blur` のログレベル。実機で確かめる点: オプションを開いた見た目・Tab/Space の操作感・選択が再起動後も残るか・本物のクリップボード・トーストがダイアログを覆うこと。
- Markdown の見出しは日本語固定。多言語ならローカライズと Agent 側の抜粋コマンドの変更が要る。
- ログ: 参照実装のログ圧縮（debug の繰り返しを 1 行に）は**意図して未移植**。ログ基盤は feedback と別タスク（手順 1c・`architecture.md` §2.5）で、圧縮はそのログの形が決まってから検討する。feedback 側が持つのは `getRecentLogs` の差し込み口、新しい方から 200KB の件数制限、error/warn の要約、最後の redact だけ。未決: ログ基盤の別 plan を誰がいつ立てるか／既存のログ基盤の形に合わせた添付の整形。
- モバイル実機から開発機への送信経路（`adb reverse` / LAN / 共有シート / トラッカー）とそのセキュリティ。
- Issue トラッカー・クラウドストレージの Sink は概要のみ（実装・認証・個人情報の外部送信の同意は未設計）。
- **本番ユーザー向けに意図して入れる場合**（debug 限定の前提と別の判断）: 同意（オプトイン）・スクショ/ログ/画面の状態に写る個人情報・送信先と保存期間・鍵の持ち方（クライアントに埋め込まない）・ストア審査。この skill のテンプレートはその用途を想定していない。
- `feedback/` と添付置き場を git 管理に入れるか（個人情報が入りうるので、導入先で判断する。既定は `.gitignore`）。
