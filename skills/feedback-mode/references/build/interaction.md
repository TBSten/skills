# 操作（Web）: popover・モードの外の入口・ページ全体・スマホ

構築側の資料。凡例・用語は `references/concepts.md`。`architecture.md` §1.2 のモードの設計（アプリの操作を止める・選ぶ単位・下書き・終わり方）を前提に、Web 参照実装（Astro 7 + Starlight の docs サイト）で変えた・足した操作と、レビュー・通し検証で分かったことをまとめる（§5 以降: スクショ・複数選択・ドラッグ・下書き・キー・ダークテーマ・保存側・E2E）。最小のコードは `assets/templates/web/astro/feedback-app.ts`。

## 1. モーダルより popover【実績】

吹き出しを全面の幕つきモーダルにすると、書きながら元のページを読み返せない。docs のように「本文を見ながら書く」場面では、選んだもののそばに浮かぶ popover にする。

- 幕を敷かない。ページは見えたまま、スクロールもできる。popover は選んだものの矩形を追う（`scroll`・`resize` で置き直す）。下に入らなければ上に出す。画面の端で切れないよう、左右を詰める。
- 外を押したとき: **コメントが空なら閉じる、書いてあれば閉じない**（書いた文を守る）。外を押して別の要素を選び直すこともしない（誤って選び直すと、書いた文の対象が変わる）。閉じるのは「やめる」と Esc。
- 書きかけの文は、明示の操作（ひな形では帯の「このページについて」）で選び直しても持っていく（`open()` が前の文を引き継ぐ）。ページの上を押しての選び直しはしない（ひな形の `pickAt()`）。
- 幅 600px 以下はシート（§4）。popover とシートで中身の作りが変わるので、開くときに `matchMedia('(max-width: 600px)')` で決める。

## 2. モードの外の入口: テキスト選択 + 右クリック【実績】

モードに入らなくても、本文のテキストを選んで右クリックすると自前のメニュー（「フィードバック…」）を出す。読みながら気づいたその場で書ける。

- 常設するリスナーは `contextmenu` の 1 つだけ。メニューや popover を出している間だけ他のリスナー（外の押下・Esc・スクロール）を付け、閉じたら `AbortController` でまとめて外す。
- 何もしない（ブラウザ標準のメニューのまま）: Shift+右クリック（メニューに「Shift+右クリックでブラウザのメニュー」と書く）、入力欄・contenteditable の中、選択が空、タッチ端末の長押し（`(pointer: coarse)`）、自前の UI と toolbar の上。
- 書きかけの popover があるときに別の場所で右クリックされても、その popover を壊さない。
- ページ遷移（View Transitions の `astro:before-swap` を含む）と `pagehide` で片付ける。

## 3. 何も選ばずに送る: ページ全体・サイト全体【実績】

要素を選ばない送信を、モードの案内の帯に「このページについて」「サイトについて」として置く（参照実装の「フィードバック情報をコピー」と同じ位置づけ。`architecture.md` §2.1）。記録の `kind` は `page` / `site`（`references/concepts.md`）。page はページのソースファイル（分かれば）を、site は開いていたページの URL を手がかりに書く。

## 4. スマホのブラウザ【実績: iPhone・Pixel のエミュレーション（WebKit・Chromium）で確認】

同じページを `astro dev --host` などでスマホから開いて使う場合。

- **タップは pointerup で判定する**: iOS Safari は、click ハンドラを持たない要素（段落など）のタップでは `click` を出さない。document の capture リスナーは「ハンドラ」に数えられない。そこでタッチは `pointerdown` の位置・時刻を覚え、`pointerup` で「動いた距離が小さい（約 10px 未満）・短い（約 600ms 未満）」ならタップとして要素を選ぶ。`click` は（来たときに）リンクやボタンを動かさないためだけに止める。
- **長押しのテキスト選択は `selectionchange` で拾う**: スマホにはマウスのドラッグが無い。選択範囲が変わったら、少し落ち着いてから（debounce）テキストを選んだことにする。マウスのドラッグ中（`mousedown`〜`mouseup`）は途中なので確定しない。
- **幅 600px 以下は下から出るシート**: 幅いっぱい・最大 85dvh・中身はスクロール・`overscroll-behavior: contain`。送るボタンの列は `position: sticky; bottom: 0` で、`env(safe-area-inset-bottom)` を足す。
- **入力は 16px 以上**（iOS は 16px 未満の入力欄にフォーカスすると拡大する）、**押せる高さは 44px 以上**。`@media (max-width: 600px), (pointer: coarse)` で当てる。
- **`visualViewport` を見る**: ソフトキーボードで見える範囲が縮むので、シートの外枠の `top` / `height` を `visualViewport.offsetTop` / `height` に合わせる（`resize`・`scroll` で付け直し、閉じたら外す）。
- タッチの長押しで `contextmenu` が来る端末もある。そのときは自前メニューを出さず、選択のそばのボタンに任せる。

## 5. スクリーンショット【実績: Web 参照実装】

- **ライブラリ**: DOM を絵にする `modern-screenshot` を devDependencies に入れ、toolbar アプリの中でだけ動的 import する（`const { domToCanvas } = await import('modern-screenshot')`）。Vite は動的 import の依存を初めて読んだ瞬間にページを再読み込みすることがあるので、`optimizeDeps.include` に先に入れておく。
- **撮るのは送信の瞬間**。1 回に 6 秒ほどかかるので、送信ボタンを押している間は「撮影中」を出し、書いている間は撮らない。撮れなくても記録は送り、理由を節に書く（`architecture.md` §2.2 の規則 ④）。
- **2 枚にする**: 画面全体（viewport。スクロール分は `transform: translate(-scrollX, -scrollY)` で合わせる）と、選んだ場所のまわりの切り抜き（余白 24px ほど。画像と viewport の比で画像の px にし、画像の中に収める。収まる部分が無ければ作らず、理由を書く）。画像の大きさを抑えるため、倍率は `min(devicePixelRatio, 2)`。
- **赤い枠と番号**: 撮った canvas に、選んだ場所の枠（要素は 1 つ、テキストは行ごと。上限を決める）を赤で描く。複数の要素（§6）では、すべての枠の左上に番号を付け、記録の `### n.` と対応させる。
- **自前の UI は写さない**: 複製のフィルタで、自前の UI の要素と Astro の toolbar を落とす。ダイアログを開いたままでも写らない。`display: none` の HTML 要素も飛ばす（配置は変わらず、速くなる。SVG の `<defs>` は `<use>` が参照するので飛ばさない）。
- **全面が白になる**: 複製の XML に `:xmlns` のような不正な属性名が 1 つでもあると、画像が全部白になる（code block の折りたたみの svg に付いた例）。複製のときに `/^[A-Za-z_][-\w.]*(:[A-Za-z_][-\w.]*)?$/` に合わない属性を外す（`onCloneEachNode`）。
- **撮っている間はログのバッファへの書き込みを止める**: 複製でカスタム要素が出す例外が、記録のコンソールの節に紛れる。
- **限界**: 外部ドメインの画像は CORS で白い四角になる。固定ヘッダーは写らないことがある。どちらも許容し、記録の節に「写らないことがある」と書いておく。
- **本番に入れない**: ライブラリ名を dist の検査のパターンに足す（`assets/templates/web/astro/check-no-dev-tools-in-dist.sh` の既定に入れてある）。
- **撮れたことの確かめ**: ファイルがあるだけでは、真っ白でも通る。E2E では画素のばらつき（RGB を各 4 ビットに丸めた色の種類の数・輝度の標準偏差）に下限を置く（例: 色 10 種類以上・標準偏差 8 以上。実ページはこれを大きく超える）。切り抜きは画面全体より小さいことも見る。
- **TODO**: ライブラリが複製の途中で投げると、sandbox の iframe が body に残る。自前で context を作り、`finally` で破棄する。

## 6. Shift で複数の要素を選ぶ【実績: Web 参照実装】

- Shift+クリックで選択に足す・入っていれば外す。**上限を設け**（例: 20）、超えたら知らせて足さない。選びが 1 つなら `kind: element`、2 つ以上なら `elements` に保つ（追加・削除のたびに作り直す純粋な関数にすると、テストしやすい）。テキストを選んでいたときは、そこから数え直す。Shift+クリックでテキストの選択が広がらないよう `mousedown` を止める。
- 記録は `kind: elements`。**frontmatter の場所の手がかり（source・selector・heading など）は配列で、要素の数にそろえる**。値の無い要素は `null` にし、番号とずらさない（値の無い要素を詰めると、2 番目の値が 1 番目のものに見える）。
- 本文の節は要素ごとに `### 1.`・`### 2.` … と並べる。要約は「N 個の要素（最初のソース ほか）」。
- スクリーンショットには全部の枠に番号を付ける（§5）。
- メモ（`memo.md`）では、Shift で足すのではなく要素ごとに別の箇所にする。続けて足せる。

## 7. header のドラッグ【実績: Web 参照実装】

- popover の header をつかんで動かす。**pointer events** で作る（`pointerdown` で `setPointerCapture`、`pointermove` で置き、`pointerup` / `pointercancel` で終える。header には `touch-action: none`・`user-select: none`）。
- ボタン・入力欄・`label`・`a`・`summary`・`contenteditable` の上から始めたドラッグは動かさない（`closest()` で弾く）。
- **画面の外に出さない**（余白 4px ほど残して収める。popover が画面より大きければ左上）。ダブルクリックで元の位置（選んだものを追う位置）に戻す。動かしたら、選んだものを追うのをやめる。
- 位置の覚え方: フィードバックはその回だけ（次に開いたら選んだもののそば）、メモは `localStorage` に覚える（ページを読みながら書き溜めるので、いつもの場所にあるほうがよい）。壊れた保存値は捨てる。
- **シート（幅 600px 以下）では動かさない**。

## 8. 下書き【実績: Web 参照実装】

- 書きかけの文とチェックの状態を `sessionStorage` に持つ。**鍵は URL（hash を除く）・選んだ場所（要素は XPath、複数は連結、テキストは選んだ文字列 + 開始側ブロックの XPath、ページ全体は固定）・宛先（このページ / サイト全体。同じ鍵を共有するので値の側に持つ）**。
- Esc や誤操作で閉じても、同じ場所を選び直せば戻る。**送信したら消す**。コメントが空でチェックも既定のままなら、空の下書きを残さず消す。タブを閉じれば消える。
- 読み書きは全部 try/catch で囲む（プライベートウィンドウ・容量超過でも入力に影響させない）。

## 9. モード中のキー【実績: Web 参照実装】

- サイトのショートカット（検索の `/`・Ctrl+K など）を動かさないため、**window の capture で `keydown` / `keyup` / `keypress` に `stopImmediatePropagation` だけをする**（ページのどのリスナーより先に来る）。
- **`preventDefault` はしない**。ブラウザの既定の動作（コピー・ページ内検索・Space や矢印のスクロール・Tab の移動）を残すため。
- リンクやボタンが動くのを止めるのは、キーではなく **click の横取り**（`preventDefault` + `stopPropagation`）でやる。Enter / Space は click になるので、そこで止まる。
- **自前の UI の中のキーも window の capture で止める**。host 要素の bubble で止めるのでは、document の capture にあるサイトのショートカットが先に動く（フィードバックを書いている途中で検索が開く）。文字の入力・改行・Tab・ボタンの Enter / Space は既定の動作なので残る。自前の UI にキーの処理（Ctrl+Enter で送る など）を足すときは、host の中のリスナーには届かないので、この window の capture のリスナーで扱う。Astro の toolbar の上のキーは通す。
- **背景の編集欄にフォーカスを入れない**。伝播を止めても既定の編集（文字の入力・貼り付け）は止まらず、Tab やクリックの mousedown（click の横取りより前）でページの input・textarea・select・contenteditable にフォーカスが入ると中身を変えられる。window の capture の `focusin` で外し、モードに入る前から入っていた分も外す。外すだけだと Firefox は次の Tab がまた同じ欄に入って先へ進めないので、外した欄に `tabindex=-1` を付け、モードを終えるときに元に戻す。
- **Esc の keyup**: Astro の Dev Toolbar は Esc の `keyup` でアプリを閉じ、toolbar を隠す。こちらで扱った Esc の keyup は、次の 1 回を capture で止める（閉じたあとに届く分も。1 秒で外す）。IME の変換中の Esc はモードを終えない。

## 10. ダークテーマへの追従【実績: Web 参照実装】

shadow host に `all: initial` を当てると `color-scheme` が既定に戻り、ページが暗くても UI だけ明るくなる（入力欄の既定の色も明るいまま）。**ページの computed な `color-scheme` を host に写し**（`normal` なら `light dark` で OS に従う）、色は CSS の `light-dark(明, 暗)` で持つ。テーマの切り替え（`html` の `data-theme`・`class`・`style` の変更）を `MutationObserver` で見て付け直し、host が外れたら監視をやめる（host を置いた `<html>` の `childList` も見る。属性だけを見ると host を外しても呼ばれず、開け閉めのたびに observer が残る）。最小の実装: `assets/templates/web/astro/feedback-app.ts` の `syncColorScheme`。

## 11. popover の置き場所と field の並び【実績: Web 参照実装】

- **下端の案内の帯を避ける**: popover の置き場所の候補（選んだものの下 → 上）が帯と重なるときは、選んだものの上 → 帯の真上 → 帯の真下の順に、画面に収まって帯を覆わない場所を探す。見つからなければそのまま。
- **field 名が長いとき**（メモの 1 行 field）: grid の列を `grid-template-columns: fit-content(40%) minmax(0, 1fr) auto` にする。`max-width` を viewport 基準にすると、狭い popover で名前が入力欄を押しつぶす。

## 12. 保存側の注意【実績: Web 参照実装】

- **`--host` の警告**: dev サーバーを `--host` で LAN に出すと、同じネットワークの人が記録とメモを書き込め、メモを読み出せる。起動時に警告を出す（止めはしない。未指定・`localhost`・`127.0.0.1`・`::1` は警告しない）。上限（件数・大きさ）で守る。
- **保存に失敗したら添付を消す**: 添付は記録より先に置く（参照を記録に書くため）ので、番号を使い切った・書き込みに失敗した、のどれでも孤児が残る。記録の保存が失敗したら添付のフォルダを消す。
- **添付の合計に上限**を設ける（例: 30MB）。1 件ごとの上限だけだと、送り続けられると増え続ける。添付の名前も固定の 2 つ（全体・切り抜き）に限る。

## 13. E2E の作り方【実績: Web 参照実装】

- **Playwright Test** を使う。dev サーバーは `webServer` で立て、**リポジトリの写しで**起動する（同じリポジトリの dev は、利用者が Markdown を直すたびに全リロードされ、途中で落ちる。`references/loop/lessons.md`）。
- **保存先は実行ごとに用意する**（テストごとに空にした一時フォルダ）。本物の記録を汚さず、件数を数える確認が揺れない。dev サーバーの保存先は 1 つなので `workers: 1`・`fullyParallel: false`。
- **スマホ専用・PC 専用の操作は `projects` で分ける**: PC 専用（Shift+クリック・右クリック・clipboard・画素・CDP）の spec は、スマホの project の `testIgnore` に置く。スマホ（iPhone 相当は WebKit、Pixel 相当は Chromium のエミュレーション）ではタップ・シート・16px / 44px を確かめる。
- **`fill` は画面の外の入力欄にも通ってしまう**: 入力欄が見えない・押せない配置のバグを見逃す。見た目の検証は `boundingBox()` で、画面の中にあること・他の部品と重ならないこと・幅がつぶれていないことを確かめる。
- 撮影の確かめは画素のばらつき（§5）。dist の検査（`check-no-dev-tools-in-dist.sh`）も E2E と同じ流れで回す。
