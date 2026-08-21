---
name: status-board
description: >
  今抱えている作業（GitHub の PR / issue、ローカルブランチ、会話中の未決事項・構想）を集めて、
  依存グラフの SVG 図 + エピック別カンバン + 詳細パネルからなるペラいち HTML を 1 ファイルだけ生成する。
  人間待ちのタスクと「次の一手」が一目で分かることを最優先にした、状況把握用のボード。
  確認したいことは詳細パネルの textarea に書き込めるので、回答をブラウザで受け取れる。
  Shift+クリックで複数のチケットを選ぶと、そこだけを浮かせた図を SVG / PNG に書き出せる。
  gh / git / node があれば言語・フレームワークを問わず使える。
  Use when requested: "状況をまとめて", "status board", "作業状況を1枚にして", "今の状況を可視化",
  "ステータスボード", "進捗を図にして", "何が止まってるか見たい", "PR の詰まりを可視化".
metadata:
  status: Experimental
  group: タスク管理
---

# status-board

いま抱えている作業を **1 枚の standalone HTML** にまとめる。図（依存グラフ）とカンバンで
「人間待ちはどれか」「次の一手はどれか」が一目で分かることが最優先。

自分用ツールなので、生成物に操作説明・凡例・飾り見出しは入れない。

## 速さが仕様

**ユーザーに HTML を渡すまで 5 ターン以内・1 分以内。** 検証はバックグラウンドに回して待たせない。

そのために次を守る。守れないと体感が一気に悪くなる。

- **迷ったら `collect.mjs` の既定をそのまま使う。** `col` / `epics` / `next` / `status` は
  自動で入る。読みにくいと分かってから直せばよく、先回りして考えない
- **`board.json` を書き直さない。** 足したいものだけ overlay に 10〜20 行書く
- **コマンドはまとめて 1 回で打つ。** ステップごとに小さく打たない
- **検証は `__verify()` を 1 回呼ぶだけ。** 個別チェックを組み立てない
- **目視はスクリーンショット 1 枚だけ。** ズームやリサイズを試さない

## 成果物

`.local/status-board/<yyyy-MM-dd-HH-mm>.html` **1 ファイルだけ**。CSS / JS は全部インライン。
外部依存は Google Fonts の `<link>` のみ。中間ファイルはスクラッチパッドに置く。

前回結果とのマージはしない。毎回フル再生成する。

## 手順

以下 `SB=.claude/skills/status-board`、`W=<スクラッチパッド>` とする。

### 1. 環境チェックと収集（コマンド 1 回）

```bash
TS=$(date +%Y-%m-%d-%H-%M); OUT=".local/status-board/$TS.html"
node -v && git rev-parse --show-toplevel
git check-ignore -q .local && echo "out: ignored" || echo "out: NOT ignored"
git check-ignore -q .playwright-mcp && echo "pw: ignored" || echo "pw: NOT ignored"
node $SB/scripts/collect.mjs -o $W/board.json
echo "OUT=$OUT"
```

`collect.mjs` は GraphQL 1 往復で PR / CI / レビュー状態 / 未 resolve コメント / issue /
ローカルブランチを集め、`status` 判定・stacked PR からの `col` と `edges`・エピック・
`next` まで入れて `board.json` を書く。**出力の一覧を読むだけでよく、中身を開く必要はない。**

`gh` が無ければ git ローカルだけで続行する（その旨が stderr に出る）。

### 2. overlay を書く（会話で分かっていることだけ）

`collect.mjs` が出せないのは会話コンテキスト由来のものだけ。**それが無ければこのステップを飛ばす。**

```jsonc
// $W/overlay.json — 足すものだけ。board.json は触らない
{
  "items": [
    { "id": "decision-api-deprecation", "key": "decision", "title": "未決 — 旧 API の廃止時期",
      "kind": "human", "status": "blocked", "human": true,
      "meta": ["A 次マイナー / B 即時 / C 据え置き"],
      "ask": ["A / B / C のどれにしますか。"], "updated": "2026-08-16" }
  ],
  "edges": [{ "from": "decision-api-deprecation", "to": "pr123", "kind": "block" }]
}
```

入れる価値があるのは 3 種類だけ:

| 何 | 書き方 |
| --- | --- |
| ユーザーにしかできない作業 | `kind:"human"`, `human:true`（六角形 + 🙋） |
| 方針が決まっていない論点 | 上に加えて `status:"blocked"` と、止めている先への `kind:"block"` エッジ |
| 今回やらないが視野にある話 | `kind:"idea"`（点線枠） |

**`block` の起点には `col` / `epic` / `anchorY` を書かない。** 止めている相手の真下に自動で
置かれ、下から上へ矢印が突き上がる（横に長い線で図を横断させるより読めるため）。

**確認したいことは `ask` に書く。** 詳細パネルに textarea が出て、ユーザーが回答を書き込むと
図とカンバンの表示が 🙋 から ✓ に変わる。回答は `localStorage` に残り、id が同じなら次の生成にも引き継がれる。

`col` を持つ item を足すときは、その col がエピックの範囲内なら `epic` も付ける。
忘れてもビルドが直し方つきで止めてくれるので、先に悩まない。
overlay の書式は [references/data-schema.md](references/data-schema.md)。

### 3. ビルドして即座に渡す（コマンド 1 回）

```bash
node $SB/scripts/build-board.mjs $W/board.json --overlay $W/overlay.json -o "$OUT" && echo "$PWD/$OUT"
```

overlay を書かなかったら `--overlay` ごと省く。検証に落ちたら**指摘のとおり overlay を直す**
（テンプレートや検証を緩める方向へ逃げない）。

**ここで絶対パスをユーザーに報告する。**「検証はバックグラウンドで続ける」と添える。

### 4. 検証をバックグラウンドへ投げる

3 と同じターンで subagent（model: sonnet）を起動し、待たない。渡すもの:

- `$OUT` の絶対パス / `$W/board.json` / `$W/overlay.json` / `$SB` の絶対パス
- [references/review-checklist.md](references/review-checklist.md)

subagent がやること（4 ターン程度で終わる想定）:

```bash
node <SB>/scripts/serve.mjs "$(dirname <OUT>)" 8731 > /tmp/sb-serve.log 2>&1 &
sleep 1 && cat /tmp/sb-serve.log     # URL が出れば成功。ポートが埋まっていれば非ゼロ終了
```

1. `browser_navigate` で `http://127.0.0.1:8731/<ファイル名>`
2. `browser_evaluate` に **`() => __verify()`** を渡す。生成物に検証コードが同梱してあるので、
   静的チェックも操作チェックもこれ 1 回で終わる。`{ pass, failures[], info }` が返る
3. `browser_take_screenshot` を 1 枚だけ撮り、目視項目を見る
4. `failures` があれば **その項目だけ** overlay を直して再ビルド → 2 を 1 回だけやり直す
5. 後始末して結果だけ返す

```bash
pkill -f "serve.mjs"; rm -rf .playwright-mcp; git status --short
```

Playwright MCP が使えなければ 4 ごと飛ばし、5 で「未検証」と明示する。
**検証できないことを黙って伏せない。**

### 5. 検証結果を報告

通知が来たら結果だけ伝える。直した場合は「同じパスを上書きしたので再読み込みを」と添える。
1 でどれかが `NOT ignored` だったら、`.gitignore` への追加をここで提案する
（**勝手に追記しない**。追跡ファイルなので作業中のブランチの差分が汚れる）。

## 遅くなったときに疑うところ

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| 収集に 5 秒以上かかる | `gh` を PR ごとに叩いている | `collect.mjs` は GraphQL 1 往復。個別に `gh pr view` を足さない |
| 検証が長い | 個別の evaluate を組み立てている | `() => __verify()` 1 回で済む |
| 図の調整で往復している | 先回りしてレイアウトを考えている | 既定のまま出し、`__verify()` が落ちてから直す |
| ターンが増える | ステップごとにコマンドを打っている | 1 と 3 はそれぞれ 1 コマンドにまとめる |

## 生成物でできること（ユーザーに伝える価値があるもの）

- **Shift+クリックで複数選択。** 図でもカンバンでも同じ。通常クリックは今まで通り 1 件だけ
- 複数選んでいる間、詳細パネルは選択中の一覧になる（行を押すとその 1 件の詳細に潜る、✕ で外す）
- **選択したまま `SVG` / `PNG` を押すと、選んだものだけを浮かせた全体図が出る。**
  選択とその隣接以外は薄く沈み、選択同士を直接つなぐ依存線だけが太くなる。**PR やイシューに貼る図はこれで作る**
- **「全画面」で図だけをビューポート一杯に広げられる。** もう一度押すか Esc で戻る。全画面中もパン / ズーム / 選択は通常どおり
- **選択やフィルターの表示状態は URL の query parameter に入る（`?sel=` など）。** URL をコピーすれば同じ表示のまま共有できる

点灯は 2 段。**選んだものとその隣接（依存元・依存先） = そのまま / それ以外 = 沈む。**
hover も同じ規則で光る。依存チェーンを辿って連結成分ごと光らせはしない
（一本鎖の stacked PR では 1 件選んだだけで全点灯になり、絞り込む意味が消えるため）。

## テンプレートの構造

`assets/board-template.html` が出力の雛形。データ部以外は出力とまったく同じ。

- 先頭に `BOARD DATA START` / `END` のマーカーがあり、`build-board.mjs` が
  `META` / `PROPS` / `STATUSES` / `EPICS` / `URGENCY` / `ITEMS` / `EDGES` を差し込む
- その下は原則変更不要。`<style>` とエンジンの `<script>` は末尾にまとめてある
- 末尾の `__verify()` が自己検証。`window.__errors` に実行時エラーを溜めている

**テンプレートを直したくなったら、それはテンプレートのバグの可能性が高い。**
1 回の生成だけ直すのか以後ずっと直すのかを区別し、後者ならテンプレートを直す。

## 既知の制約

- SVG / PNG 書き出しに Google Fonts は焼き込まれない。開いた環境に Zen Maru Gothic が
  無いとフォールバックフォントで描画される
- 図は依存グラフ固定。他の図種には切り替えられない
- `edges` は左レーン → 右レーンのみ。同レーン・逆向きは経路が破綻するため弾いている
- `file://` で開くと `localStorage` が使えないブラウザがある。確認欄の回答を残したいときは
  `serve.mjs` 経由で開く
