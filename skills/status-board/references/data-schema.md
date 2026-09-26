# board.json / overlay.json スキーマ

`build-board.mjs` の入力。下書きはスクラッチパッドに置く
（`.local/` に残るのは出力 HTML とビルドが書く [snapshot](#snapshot--前回の-overlay-を引き継ぐ) だけ）。

```bash
node scripts/build-board.mjs <board.json> [--overlay <overlay.json>] -o <out.html>
```

- `board.json` … `collect.mjs` が作る下書き。**基本的に手で書き直さない**
- `overlay.json` … 会話コンテキスト由来の差分だけを書く小さいファイル（[書式](#overlayjson)）

```jsonc
{
  "meta":     { "title", "repoUrl", "repoLabel", "updatedAt" },
  "props":    { "showChildren", "dimUnrelated", "nodeWidth" },
  "statuses": [ { "id", "label", "short", "stroke", "fill", "pane", "collapsedByDefault" } ],
  "epics":    [ { "id", "label", "fill", "border" } ],
  "urgency":  [ "statuses[].id", ... ],
  "items":    [ { ... } ],
  "edges":    [ { "from", "to", "kind", "label" } ]
}
```

## meta

| キー | 必須 | 意味 |
| --- | --- | --- |
| `title` | ○ | ヘッダー左のタイトル。ブラウザのタブ名にもなる |
| `repoUrl` | — | ヘッダーのリンク先 |
| `repoLabel` | — | リンクの表示文字列。省略時は `repoUrl` |
| `updatedAt` | — | **この HTML 自体**の最終更新。チケットの更新日ではない |

## props

| キー | 既定 | 効果 |
| --- | --- | --- |
| `showChildren` | `false` | 図の子チケット全表示の初期値。ヘッダーのトグルで切り替わる |
| `dimUnrelated` | `true` | ホバー・選択時に無関係なノードを薄くするか |
| `nodeWidth` | `198` | 図のノード幅 = レーン幅（160〜260） |

## statuses — カンバンの列 = ノードの色

`{ id, label, short, stroke, fill, pane }` すべて必須。`collapsedByDefault: true` で
その列を最初から折りたたむ（既定では `closed` のみ）。

**自由に追加してよい。** 色は DS の派生色に限る:

| 用途 | stroke | fill | pane |
| --- | --- | --- | --- |
| 完了系（leaf） | `#3E9E7A` | `#E7F3EE` | `#EDF6F2` |
| レビュー（amber） | `#E0A32E` | `#FCF3E4` | `#FBF5EA` |
| 議論（coinDark） | `#C69A08` | `#FBF2D8` | `#FBF6E6` |
| 実装（navy） | `#3F4B7F` | `#DBEFF1` | `#EAF6F7` |
| ブロック（heart） | `#E2596B` | `#FCEBEE` | `#FCF0F2` |
| 未着手（navy-300） | `#849AB5` | `#F7FAFB` | `#F7FAFB` |
| クローズ（gray） | `#C4D0D3` | `#EDF2F3` | `#F7FAFB` |

- `label` … カンバンの列名・詳細パネルのピル・絞り込みチップ
- `short` … 図のノード内チップ。長いと枠を割るので 5 文字程度まで

## epics — 図の枠 = カンバンの段

`{ id, label, fill, border }` すべて必須。`fill` は薄い面、`border` は破線の色。

**同じエピックのメンバーの `col` は連続させる。** 範囲内に別のエピック（またはエピック無し）の
ノードが入っているとビルドがエラーで止まる。図の枠が重なって読めなくなるため。

## urgency

畳んだエピックを 1 ノードにしたときの代表ステータスを決める優先順。上ほど緊急。
省略すると `statuses` の並び順が使われる。

## items — チケット 1 件 = 1 行

| キー | 必須 | 意味 |
| --- | --- | --- |
| `id` | ○ | 内部キー。`edges` から参照 |
| `key` | ○ | 表示用の識別子（`repo#123` / `migrate-1` / `human` など） |
| `title` | ○ | 図のノード内で 1〜2 行に収まる長さ |
| `status` | ○ | `statuses[].id` |
| `kind` | ○ | `anchor` / `pr` / `task` / `human` / `idea` |
| `col` | — | 図のレーン番号（整数）。**省略するとカンバンだけに出る** |
| `anchorY` | — | そのレーンで中心線から外して置く時の y。300 くらいまで |
| `epic` | — | `epics[].id` |
| `human` | — | `true` で 🙋 人間待ちチップ |
| `next` | — | 1 から振る「次にやる順」。金色の NEXT 旗。多くても 3 つまで |
| `meta[]` | — | 図のノード内に出る補足行。1〜2 行 |
| `desc[]` | — | 詳細パネルの本文 |
| `notes[]` | — | 詳細パネルの備考 |
| `children[]` | — | `{ key, title }`。子チケット |
| `links[]` | — | `{ label, url, note }`。**先頭が図の ↗ のジャンプ先** |
| `ask` | — | 確認したいこと。文字列か文字列の配列。詳細パネルに確認欄が出る |
| `updated` | — | 最終更新の文字列 |
| `kanban` | — | `false` でカンバンに出さない（`main` のようなアンカー用） |

### `ask` — 確認欄

`ask` があるか、`human:true` / `kind:'human'` の item は、詳細パネルに
「確認したいこと」と「回答」の textarea が出る。`ask` の内容が確認欄の初期値になる。

選択肢がある論点は 1 行 1 選択肢にしておくと答えやすい:

```jsonc
"ask": [
  "旧 API の廃止時期は A / B / C のどれにしますか。",
  "A: 次のマイナーで deprecated、次のメジャーで削除",
  "B: 即 deprecated、削除時期は未定",
  "C: 当面そのまま残す"
]
```

書き込みは `localStorage` に入る。キーは**ボード名 (`meta.repoLabel` か `meta.title`) と
item の `id`** なので、**`id` を安定させれば再生成しても回答が引き継がれる**。
PR / issue 由来の id (`pr123` / `issue45`) は `collect.mjs` が安定して振る。
会話由来のノードは自分で意味のある id を付ける（`api-deprecation-policy` など。連番は避ける）。

`kind` の見た目:

| kind | 形 | 使いどころ |
| --- | --- | --- |
| `anchor` | 小さい丸ピル | `main` などの基準点。`key` 行を出さない |
| `pr` | 角丸箱 | PR |
| `task` | 角丸箱 | 作業単位 |
| `human` | 六角形 | 人間待ち・未決の判断 |
| `idea` | 点線の角丸箱 | まだやらない構想 |

## edges

`{ from, to, kind, label }`。`kind` の既定は `flow`。

| kind | 線 | 向き | 意味 |
| --- | --- | --- | --- |
| `flow` | 実線・navy | 左 → 右 | 依存・順序 |
| `block` | 破線・赤 | **下 → 上** | 止めている |
| `idea` | 点線・薄い navy | 左 → 右 | 構想へのつながり |

**制約**: `from` / `to` はどちらも `col` を持つこと。図に出ないチケットには線を引けない。
`flow` / `idea` は `from.col < to.col` であること。

### block は下から上へ

`block` だけは向きが違う。**止めている側を相手の真下に置いて、下から上へ矢印を突き上げる。**
横に長い線で図を横断させるより「この未決がこれを止めている」が一目で分かるため。

そのため `block` の `from` に **`col` / `epic` / `anchorY` を書かない**。ビルド時に相手から
導出してレーンを合わせ、items の末尾へ回して相手の下に積む
（複数を止めているときは一番左の相手に合わせ、残りは従来どおりの横向きになる）。
`label` は縦向きのときは出ない。ノード間に置き場所が無く、赤い破線と上向きの矢尻、
🙋 六角形だけで意味が足りているため。

## collect.mjs が決めるところ

```bash
node scripts/collect.mjs -o <board.json> [--days 7] [--title "..."] [--all] [--no-gh] [--prev <dir>]
```

GitHub への問い合わせは **GraphQL 1 往復だけ**（PR 一覧・CI・レビュー状態・未 resolve
コメント・issue をまとめて取る）。owner/repo は `git remote` から取るので `gh repo view` も使わない。
`gh` の起動は 1 回 ≈ 1.2s なので、**PR ごとに `gh pr view` を足すと本数に比例して遅くなる**。

| 決めるもの | どうやって |
| --- | --- |
| `meta` | `git remote`。`--title` で上書き |
| `statuses` / `urgency` | 既定の 8 件 |
| PR の items | open + 直近 `--days` に merged したもの |
| `status` | CI・draft・未 resolve コメント・`reviewDecision` から下の順で判定 |
| `meta[]` | `CI 6/8 pass` / `未 resolve コメント 2 件` / `draft` / `merged` |
| `col` と `edges` | stacked PR（base が別 PR の head）の連なりから深さを算出 |
| `epics` | 2 本以上の stacked PR の連なりを 1 エピックに。枠が重なる配置になる場合は作らない |
| `next` | 図に出ている未完了のものを緊急度順に並べ、上位 3 件へ 1〜3 を振る |
| issue / ローカルブランチ | `col` を振らずカンバンのみ。関係するものは後から昇格する |

**`epics` と `next` は既定が入る。そのまま使ってよい。** 会話で分かっていることがあるときだけ
overlay で上書きする。

`status` の判定順（上から評価し、最初に当たったものを採用）:

1. merged / closed → `closed`
2. CI に失敗ジョブがある → `blocked`
3. draft → `implementing`
4. 未 resolve のレビューコメントがある → `nits`
5. `reviewDecision == APPROVED` かつ CI が全通過・待ち無し → `merge-wait`
6. それ以外の open PR → `review`
7. open issue → `backlog`

**決めないもの**（overlay で足す）: `ask` / `kind:'human'` の人間待ち・未決 /
`kind:'idea'` の構想 / issue・ブランチの `col` 昇格。
会話コンテキストからしか出てこず、図の価値はここに集中しているので、スクリプトでは推測しない。

既定では「今動いているもの」だけを拾う（`--days` 以内に更新のあった issue・ブランチ、
merged PR は最大 6 件）。塩漬けの issue とブランチを全部載せると図もカンバンも潰れる。
全部欲しいときだけ `--all`。

`--no-gh` または `gh` が使えないときは git ローカルだけで作り、その旨を stderr に出す。

## overlay.json

`board.json` を書き直さずに差分だけ重ねる。**足したいものだけ 10〜20 行書けばよい。**

```jsonc
{
  "items": [
    { "id": "decision-api-deprecation", "key": "decision", "title": "未決 — 旧 API の廃止時期",
      "kind": "human", "status": "blocked", "human": true,
      "ask": ["A / B / C のどれにしますか。"], "updated": "2026-08-16" }
  ],
  "edges": [{ "from": "decision-api-deprecation", "to": "pr123", "kind": "block" }]
}
```

合成規則:

| キー | 規則 |
| --- | --- |
| `meta` / `props` | 浅くマージ |
| `statuses` / `epics` / `items` | `id` で upsert。既存はフィールド単位で上書き、無ければ末尾に追加 |
| `edges` | `from` + `to` が同じものは置き換え、無ければ追加 |
| `urgency` | あれば丸ごと差し替え |

既存の値を変えたいだけなら `id` とそのフィールドだけ書けばよい
（例: `{"items":[{"id":"pr200","next":1}]}`）。

`col` を持つ item を足すとき、その col がエピックの範囲内なら `epic` も付ける。
忘れてもビルドが直し方つきで止めるので、先に悩まない。
**`block` の起点だけは例外で、`col` / `epic` / `anchorY` を書かない**（相手から導出される）。

## snapshot — 前回の overlay を引き継ぐ

`build-board.mjs` は出力 HTML と同じ basename の `<ts>.json` を隣に書く:

```jsonc
{ "html": "<ts>.html",
  "board": { ... },     // 描画した最終データ（overlay 合成・block 補正後）
  "overlay": { ... }    // 渡した overlay の原文（合成前）。--overlay 無しなら null
}
```

`collect.mjs` は `--prev <dir>`（既定 `.local/status-board`）の最新 snapshot から
`overlay` を取り出し、下書きの隣に `overlay.prev.json` を書いて要約を stderr に出す。
会話由来の人間待ち・未決・構想はセッションを跨ぐと snapshot にしか残らないため。

機械的に死ぬものはこのとき落とす（残すか消すかの判断を「片付いたかどうか」だけにするため）:

- 端点が居なくなった `edges`（相手の PR が merge されて板から消えた等）
- board から消えた item への部分上書き（`{"id","next"}` だけのような差分）
- 消えた epic への参照（`epic` フィールドだけ外す）
- `next`（毎回 collect が振り直す）

引き継ぐかどうかは呼び出し元が決める: **まだ生きている項目だけ残して `overlay.json` にする。**

## 検証（`build-board.mjs`）

落ちたら非ゼロ終了する。**JSON 側を直して解消すること。**

- `items[].id` の重複 / `statuses[].id` の重複 / `epics[].id` の重複
- `items[].status` が `statuses` に無い / `items[].epic` が `epics` に無い
- `items[].kind` が 5 種以外
- `edges[].from/to` が `items` に無い、`col` を持たない、`from.col >= to.col`（`block` を除く）
- `edges[].kind` が 3 種以外
- エピックの `col` 範囲に別のノードが入っている
- `next` の重複
- `links[]` の `url` / `label` 欠け、`children[]` の `key` / `title` 欠け
- `ask` が文字列でも配列でもない
