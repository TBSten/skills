---
name: feedback-mode
description: >-
  アプリの debug ビルドに「フィードバックモード」（画面の部品を選んでコメントすると、画面・状態・ログ・スクリーンショット付きの記録が
  フィードバック置き場に溜まる）を構築し、溜まった記録を Claude Code が実装・調査・設計に振り分けて消化するループを回す skill。
  Desktop（Electron / Tauri）・Web・モバイル（iOS / Android / React Native / Flutter）アプリが対象。
  環境を判定して、feedback mode があれば消化（loop）側だけ、なければ構築（build）側だけを読む。
  保存先は Sink（ファイル / HTTP / Issue トラッカー等）で差し替えられ、本番ビルドには入れない前提で配線する。
  逆向き（agent → 人）の Review（agent の指摘を画面の印と popup で見せ、人はボタンで答える）と、人の判断を溜めて次の回に聞き直さない判断の記録、
  agent が作る 1 枚の HTML（レポート・チェックリスト）に人がコメントしてまとめてコピーで返す仕組みも扱う。
  Use when requested: "フィードバックモードを作って", "アプリにフィードバック機能を入れたい", "debug ビルドにフィードバックを入れたい",
  "feedback mode", "フィードバックの保存先を変えたい", "feedback を対応して", "フィードバックをループして", "feedback/ を一通り対応",
  "溜まったフィードバックを処理", "feedback ループを回したい", "指摘を画面で見せたい", "レビューの指摘を人に答えてもらう",
  "直さないと答えたのをまた聞かないで", "判断を記録して", "レポートの HTML にコメントできるようにしたい",
  "add feedback mode to my app", "process the feedback inbox", "show review findings to a human".
metadata:
  status: Experimental
  group: テスト・検証
---

# feedback-mode（ルーター）

アプリのフィードバックモード（作る）と、溜まったフィードバックの消化ループ（回す）と、agent の指摘を人に答えてもらう Review の入口。**呼ばれたら、まず環境を判定し、build・loop・review のどれか 1 つの入口だけを読む**。全部を最初から読まない。

> **具体パスは参照実装（社内向け Electron ツール）の例**: この skill に出てくる `feedback/`・`feedback/archived/`・`.local/tmp/feedback/`・`yymmdd-xx.md`・`npm run check` などは参照実装での値。**自分の環境（アプリの種類・リポジトリ・保存先）に置き換えて読む**。置き場は抽象語「フィードバック置き場（Inbox）」、保存先の差し替え層は「Sink」（`references/concepts.md`）。
> 凡例: 【実績】参照実装で確認済み（【実績: Web 参照実装】は Astro + Starlight の docs サイトで確認済み）/ 【追加】汎用化で足した設計・コード / 【未検証】推測・未検証。
> パスは特記がなければ、この SKILL.md があるディレクトリ（`${CLAUDE_SKILL_DIR}`）基準。

## 1. 判定（最初にやる）

1. ユーザーの明示が最優先: 「作って・入れたい・導入」→ **build**、「対応して・ループ・処理して」→ **loop**、両方 → build のあと loop。「指摘を見せたい・人に答えてもらいたい・判断を記録して」→ **review**（`references/review/review.md`。判断の記録だけなら `references/review/decisions.md`）。「agent が作る HTML にコメントできるように」→ `references/build/static-html.md` だけ。
2. 明示が無ければ、判定 script を実行する（読み取りだけ。置き場・設定・配線・既存 skill を表示し、末尾に `VERDICT=` を出す）。script を読解・書き換え・再実装せず、そのまま実行する。

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/detect.sh" <対象リポジトリのルート>
```

入力は対象リポジトリのルート（省略時はカレント）だけ。AI の仕事は、出力と下の表の突き合わせと、食い違うときの確認。

| 置き場 | 配線 | VERDICT | 進む先 |
|---|---|---|---|
| ある | ある | **loop** | `references/loop/loop.md` だけ読んでループに入る |
| ある | 無い（別アプリ・手書きメモ・他の取り込み元） | **loop** | 同上。作るのは頼まれたときだけ |
| 無い | ある（部分的） | **partial** | `references/build/build.md` の手順 10〜12（動作確認・loop 側の用意・試運用）だけ読み、記録が 1 件できたら loop |
| 無い（`review/` だけある） | 無い | **review** | `references/review/review.md` だけ読む（agent の指摘を人に答えてもらう側） |
| 無い | 無い | **build** | `references/build/build.md` だけ読んで構築を始める（plan 段階で debug 限定の判断をする） |

- 判断がつかない（script の結果が食い違う・複数の候補・Sink が不明）ときは、**ユーザーに 1 つだけ聞く**（「置き場はどこか」「作るか、溜まった分を対応するか」）。推測で書き込みや移動をしない。
- 対象環境に独自の消化用 skill（`feedback-loop` 等）が既にあれば、loop はそれを**正**として使い、`references/loop/` は差分の確認用に読む。
- 片方を読んだあと、もう片方が必要になったときだけ、その入口を読む（例: loop 中に「受け口を直したい」→ build、build の試運用 → loop、loop 中に `review/` に人の答えがある → review の §8）。
- script の出力に `NOTE: review/` が出たら、loop のついでに `references/review/review.md` §8 で人の答えを処理する。

## 2. 入口

| 入口 | いつ読む | 中身 |
|---|---|---|
| `references/loop/loop.md` | 実践（消化ループ） | 自環境への置き換え表・運用ルール・手順 0〜7・Sink ごとの読み方/閉じ方・分類・履歴・報告の形。補助: `references/loop/subagent-prompt.md`・`references/loop/full-check.md`・`references/loop/lessons.md` |
| `references/build/build.md` | 構築 | 前提（**debug ビルドだけに配線し本番に入れない**）・手順 1〜13（1b 既存のデバッグ基盤に乗る、1c ログ基盤は別タスク、を含む）・チェックリスト・既知の制約。詳細: `references/build/architecture.md`・`references/build/platforms.md`（Astro の Dev Toolbar は §2.1）・`references/build/locate.md`（場所を一意に指す）・`references/build/interaction.md`（popover・右クリックの入口・スマホ・スクショ・複数選択・ドラッグ・下書き・モード中のキー・ダークテーマ・E2E）・`references/build/memo.md`（ページと箇所ごとの継続メモ）・`references/build/static-html.md`（agent が作る 1 枚の HTML にコメントしてまとめてコピー）。コード: `assets/templates/`、コアのコピーとテスト: `scripts/copy-core.sh`、本番検査: `scripts/check-no-feedback-in-prod.sh` |
| `references/review/review.md` | agent の指摘を人に見せて答えてもらう（Review） | 置き場とファイルの形（`impact`・`recommend`）・流れ・ボタンと agent の扱い・人が足す選択肢 `_actions.md`・id・cli（`scripts/review.mjs`）・登録と処理の手順。UI を作るなら `references/review/ui.md`、判断の記録は `references/review/decisions.md`（`scripts/decisions.py`） |
| `references/concepts.md` | 用語・記録の形・Sink の契約が必要になったとき（全部が参照） | 凡例・用語表（Inbox / Archive / Sink / 履歴 / Review / 判断の記録）・記録の名前と構成（schema 1 の frontmatter）・状態遷移・Sink の契約・フィードバックとメモの違い・フィードバックの向きと道具 |

```
feedback-mode/
├ SKILL.md           判定と入口だけ
├ references/
│  ├ concepts.md     用語・記録の形・Sink の契約（共通）
│  ├ build/          build.md / architecture.md / platforms.md / locate.md / interaction.md / memo.md / static-html.md
│  ├ loop/           loop.md / subagent-prompt.md / full-check.md / lessons.md
│  └ review/         review.md / ui.md / decisions.md
├ assets/templates/  導入先にコピーするコード（core/ core-ts/ electron/ web/（web/astro/）mobile/ existing-debug/ と schema・記録とメモの例、review/ の指摘・選択肢・判断の例）
└ scripts/           detect.sh / copy-core.sh / check-no-feedback-in-prod.sh / review.mjs / decisions.py（とそのテスト）
```

## 3. 共通の注意

- 導入先・対象リポジトリのコードを書き換える、記録を archive（移動）する、外部（tracker・サーバ）へ送る前には、方針をユーザーに示して承認を取る。記録の archive はオーケストレータだけが行う（`references/loop/loop.md`）。
- 未検証・既知の制約は各入口の末尾（`references/build/build.md` §4、`references/loop/loop.md` §5）。
- 実行する script（`scripts/*.sh`・`scripts/review.mjs`・`scripts/decisions.py`）は読解・書き換え・再実装せず、そのまま実行する。失敗したら stderr の指示に従い、原因を分析してユーザーに報告する。
