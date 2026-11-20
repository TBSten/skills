# 1 枚の HTML（agent が作るレポート・チェックリスト）にフィードバックを付ける

検証状況: ひな形 `assets/templates/adapters/web/single-html/feedback-report.html` を `scripts/single-html.mjs new` で作り、ヘッドレスの Chromium（PC 1280px・スマホ幅 390px・保存が止められた状態）で `scripts/single-html.e2e.cjs` を通した（選ぶ → Shift で足す → 書く → 保存 → 印 → 再読み込みで書きかけが残る → ドラッグで引用 → ページ全体 → details を開ける → Esc → まとめてコピーの中身 → 2 回目は新しいものだけ → すべて消す → 戻す、横スクロール・コンソールのエラーが無い）。スマホの実機のブラウザでも確かめた: Android 16 エミュレータの Chrome 134（CDP のタッチ。`scripts/single-html.android-chrome.cjs`）と iOS 26.2 シミュレータの Safari（Maestro 2.3.0）で、入る → タップで選ぶ → ソフトキーボードで書く → 保存 → 終える → まとめてコピー（iOS は `xcrun simctl pbpaste` でクリップボードの中身まで）。実機（USB の端末）は未確認。作り方そのものは、コードベースのレポートと導入のチェックリストで運用したもの。

対象は**アプリではなく、agent が作って人に渡す 1 枚の HTML**（調査レポート・導入のチェックリスト・設計の比較表・質問票）。サーバーも置き場も無い。人は HTML を開いて項目にコメントし、質問に答え、まとめてコピーしてチャットに貼る。agent は貼られた文を読んで HTML のデータを直す。

| | アプリ（ほかの adapter） | 1 枚の HTML（ここ） |
|---|---|---|
| 置き場 | ファイル・HTTP | 無い。ブラウザの `localStorage` に溜め、クリップボードで渡す（`references/spec/data.md` §3.2 の「会話に貼る」） |
| 宛先 | 部品・ソースの行 | **HTML の中の JSON の場所**（`sections[id=roles].blocks[1].items[0]`）。agent が直すのは JSON だけ |
| 本番から外す | 要る | 要らない（HTML そのものが人に渡す成果物。送り先は人のクリップボードだけ） |

## 1. 作る（agent の手順）

```bash
S="${CLAUDE_SKILL_DIR}/scripts/single-html.mjs"
# ⭐️ データ（JSON）を書いてから、ひな形に入れる。殻は書き換えない
node "$S" new <出力先>/report.html --data <data.json>
node "$S" check <出力先>/report.html          # 直したあとも毎回
node "$S" get <出力先>/report.html > data.json # 直すときは取り出して、直して、set で戻す
node "$S" set <出力先>/report.html data.json   # 検査に落ちたら書かない
```

- 例のデータ: `assets/templates/adapters/web/single-html/example.data.json`。`file` は `new` が出力先の名前に合わせる。
- 殻（CSS・JS）は直さない。足りない見せ方があれば、データの書き方で表せないかを先に考える（表・リスト・注記・折りたたみの組み合わせ）。

## 2. データの形

```json
{
  "title": "題", "kind": "コードベースのレポート", "file": "report.html", "subtitle": "一言",
  "questions": [ { "id": "q1", "text": "質問", "recommend": "a",
                   "options": [ { "value": "a", "label": "選択肢", "detail": "補足（任意）" }, { "value": "b", "label": "…" } ] } ],
  "sections": [ { "id": "overview", "title": "1. 概要", "blocks": [ … ] } ]
}
```

| ブロック（`type`） | 書くもの | 宛先（`data-fb`）になる単位 |
|---|---|---|
| `p`（省略時） | `text` | ブロック |
| `list` | `items`（文字列の配列）・`ordered`・`label` | リストと各項目（`.items[i]`） |
| `table` | `columns`（`{ key, label }`）・`rows`（`key` ごとの値。`id` を付けると場所が番号でなく名前になる）・`label` | 表・各行（`.rows[id=x]`）・各セル（`.rows[id=x].<key>`） |
| `callout` | `text`・`tone`（`warn`） | ブロック |
| `details` | `summary`・`blocks`（入れ子） | ブロックと中身 |

- `id`（節・行・質問）は英数字と `_` `-` `.`。付けると JSON の場所が `[id=…]` になり、並べ替えても宛先がずれない。付けなければ `[番号]`。
- 宛先の名前（画面に出る道筋）は、節の `title`・ブロックの `label`・表の 1 列目の値・列の `label` から作られる（「2. 役割 › 守ること › ViewModel から…」）。
- ページ全体へのコメントは、末尾の「ページ全体へのコメントはここをクリック」（場所は `page`）。

## 3. 人ができること（ひな形に入っている）

- 右下の「フィードバック」で入る。項目をクリックで入力欄、Shift + クリックで宛先を足す・外す（20 まで）、文字をドラッグで「引用」付き。Esc で出る。
- 書きかけは宛先ごとに `sessionStorage` に残る（外を押しても、再読み込みしても消えない）。入力欄はドラッグで動かせ、位置を覚える。開いてもページは飛ばない。
- 保存したコメントと回答は `localStorage` に溜まり、同じ中身をタブの `sessionStorage` にも写す（Chromium は file:// で開いたページの `localStorage` を再読み込みの直後に失うことがあった。ヘッドレスで 1 割ほど。読むときは新しい方を使い、失った `localStorage` に書き戻す）。項目に番号の印が出る。閉じた details の中のものは、いちばん外の閉じた見出しに出る。一覧で直す・消す（消したら「戻す」）。
- 質問は選択肢 + 補足。推奨には印。
- 下のメニュー: まとめてコピー（まだ渡していないものだけ。全部渡し済みなら全部）・コピーされる内容を見る・一覧・回答を JSON で保存・すべて消す（「戻す」あり）。コピーできないときは中身を全選択して出す。
- 保存できない環境（プライベートウィンドウなど）では、そう出して、すぐコピーするよう促す。
- スマホ幅では入力欄が下からのシート、入力 16px・押せる高さ 44px。ソフトキーボードが出るとシートを見えている範囲（`visualViewport`）の下端まで持ち上げる。ダークテーマに追従。

## 4. コピーされる文と、agent の読み方

````text
コードベースのレポート（report.html）の質問への回答です（1 件）。全部の質問に答えました。
```json
{ "target": "report.html", "answers": { "q1": { "answer": "vm", "note": null } } }
```

コードベースのレポート（report.html）へのフィードバックです（2 件）。「対象」はこのファイルの JSON の場所です。直して、直せないものは理由を返してください。

1. 1. 概要 › 基本情報 › 主なフレームワーク › 内容 ほか 1 件
   対象: sections[id=overview].blocks[0].rows[id=frameworks].value（1. 概要 › 基本情報 › 主なフレームワーク › 内容）
   内容: 「Compose, Koin」
   対象: sections[id=overview].blocks[0].rows[id=targets].item（1. 概要 › 基本情報 › ターゲット › 項目）
   内容: 「ターゲット」
   > バージョンも書いて
````

1. 1 行目でファイルを決め、`get` で JSON を取り出す。
2. `対象:` ごとにその場所を直す（事実が違うと言われたらコードを見直す）。直さないものは理由を返す。
3. `内容:`（保存した時点の文）と今の JSON が違えば、自分が後から直した可能性を考える。`引用:` は人が選んだ文字。
4. 回答は JSON の `answers` に写す（`note` が `null` なら選んだだけ）。答えのない質問の id は 1 行目に出る。
5. `set` で戻し、`check` を通す。直した宛先を人に伝える。人のブラウザのコメントは人が消すまで残る。

## 5. 確かめる

`node scripts/single-html.e2e.cjs [<report.html>]`（Playwright が要る。無ければ skip）。殻を直したときは必ず通す。

スマホの実機のブラウザ（殻のスマホ向けの部分を直したとき）:

- **Android の Chrome**（エミュレータか USB の実機）: `report.html` のあるディレクトリで `python3 -m http.server 8090 --bind 127.0.0.1`、`adb reverse tcp:8090 tcp:8090`、Chrome で `http://localhost:8090/report.html` を開き、`adb forward tcp:9333 localabstract:chrome_devtools_remote` のあと `node scripts/single-html.android-chrome.cjs http://127.0.0.1:9333`。新しい AVD の Chrome は初回の画面が出るので、規約に同意する代わりに `adb shell 'echo "chrome --disable-fre --no-first-run" > /data/local/tmp/chrome-command-line'` と `adb shell am set-debug-app --persistent com.android.chrome` で飛ばす。
- **キーボード**: CDP の文字入力ではソフトキーボードが出ない。入力欄を `adb shell input tap` で押して出し、`visualViewport.offsetTop + height` がシートの下端以上か（シートが隠れていないか）を見る。
- **iOS の Safari**（シミュレータ）: `xcrun simctl openurl booted http://127.0.0.1:8090/report.html` で開き、Maestro（`appId: com.apple.mobilesafari`、`tapOn: "<表示の文字>"`）で操作し、コピーのあと `xcrun simctl pbpaste booted` で中身を見る。

実機のブラウザで見つかって直したこと（ヘッドレスのスマホ幅では出なかった）:

- ソフトキーボードが出ても layout viewport は縮まず（Chrome・Safari とも）、`bottom:0` のシートの保存ボタンがキーボードに隠れた → `visualViewport` に合わせて `--fb-kb` だけ持ち上げる。
- 保存の直後のお知らせが「フィードバックを終える」ボタンに重なり、タップを吸った → お知らせをボタンの上に出し、文の部分はタップを通す（e2e に「お知らせが重ならない」を足した）。
- 下のメニューに隠れた項目は、そのままではタップできない（下に余白はあるので、スクロールすれば押せる）。
