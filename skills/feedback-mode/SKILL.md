---
name: feedback-mode
description: >-
  アプリの debug ビルドに「フィードバックモード」（画面の部品を選んでコメントすると、画面・状態・ログ・スクリーンショット付きの記録が
  置き場に溜まる）を入れ（setup）、溜まった記録を Claude Code が実装・調査・設計に振り分けて消化する（loop）skill。
  Web（DOM・React・Astro）・Desktop（Electron・Tauri）・モバイル（React Native・Flutter・iOS・Android）を adapter で差し替え、
  保存先（ファイル・HTTP・Issue）は Sink で差し替える。本番ビルドには入れない。
  agent の指摘を画面の印と popup で人に答えてもらう Review、人の判断を溜めて二度聞かない判断の記録、
  agent が作る 1 枚の HTML に人がコメントして返す仕組みも扱う。
  Use when requested: "フィードバックモードを作って", "アプリにフィードバック機能を入れたい", "debug ビルドにフィードバックを入れたい",
  "フィードバックの保存先を変えたい", "feedback を対応して", "溜まったフィードバックを処理", "feedback ループを回したい",
  "指摘を画面で見せたい", "直さないと答えたのをまた聞かないで", "レポートの HTML にコメントできるようにしたい",
  "add feedback mode to my app", "process the feedback inbox", "show review findings to a human".
metadata:
  status: Experimental
  group: テスト・検証
---

# feedback-mode

アプリの debug ビルドに「フィードバックモード」を入れ（**setup**）、溜まった記録を消化する（**loop**）。逆向きの **Review**（agent の指摘に人が答える）も扱う。

```
人 ──（アプリの上で部品を選んでコメント）──▶ adapter ──Transport──▶ 受け口 ──▶ Sink（Inbox）
                                          環境ごとに差し替え            共通（core）     ファイル / HTTP / Issue
Inbox ──▶ loop（オーケストレータが分類 → サブエージェントが対応 → archive → 報告）
agent ──（指摘）──▶ review/ ──▶ 人がボタンで答える ──▶ loop が拾う・判断の記録に写す
```

**呼ばれたら、まず §1 でタイミングを決め、その行のファイルだけを読む**。全部を最初から読まない。パスはこの SKILL.md のあるディレクトリ（`${CLAUDE_SKILL_DIR}`）基準。具体的なパス（`feedback/` など）は例なので、導入先の値に読み替える。

## 1. タイミングを決める

1. 人の明示が最優先: 「作って・入れたい・導入」→ **setup**、「対応して・ループ・処理して」→ **loop**、両方 → setup のあと loop。「指摘を見せたい・人に答えてもらいたい」→ **review**、「判断を記録して」→ `references/review/decisions.md`、「agent が作る HTML にコメントできるように」→ `references/setup/web/single-html.md` だけ。
2. 明示が無ければ判定 script を実行する（読み取りだけ。末尾に `VERDICT=` を出す）。script は読解・書き換え・再実装せず、そのまま実行する。

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/detect.sh" <対象リポジトリのルート>
```

| VERDICT | 状況 | 読むもの |
|---|---|---|
| `setup` | 置き場も配線も無い | `references/setup/setup.md` と、そこで選ぶ adapter のファイル 1 枚 |
| `partial` | 配線はあるが記録が無い | `references/setup/setup.md` の手順 8〜10 だけ → 記録が 1 件できたら loop |
| `loop` | 置き場がある（配線の有無は問わない） | `references/loop/loop.md` |
| `review` | `review/` だけがある | `references/review/review.md` |

- 判断がつかない（結果が食い違う・候補が複数・Sink が不明）ときは、人に 1 つだけ聞く。推測で書き込みや移動をしない。
- 対象環境に独自の消化用 skill（`feedback-loop` など）があれば、loop はそれを正とし、`references/loop/` は差分の確認に使う。
- 出力に `NOTE: review/` が出たら、loop のついでに `references/review/review.md` §8 で人の答えを処理する。
- 途中で別のタイミングが必要になったときだけ、その入口を読む（loop 中に受け口を直したい → setup、など）。

## 2. references の並び

```
references/
├ setup/      最初の導入で読む
│  ├ setup.md       共通の手順・debug 限定・既存のデバッグ基盤・ログ・チェックリスト
│  ├ adapter.md     差し込み口の一覧・adapter の一覧・新しい環境の adapter の作り方
│  ├ web/           dom / react / astro / single-html（＋ interaction・locate・memo: Web の optional 機能の作り方）
│  ├ desktop/       electron / tauri
│  └ mobile/        common（先に読む）/ react-native / flutter / ios / android
├ loop/       ループ中に読む: loop.md（＋ subagent-prompt・full-check・lessons）
├ review/     agent の指摘に人が答えるとき: review.md・ui.md・decisions.md
└ spec/       共通仕様（setup・loop の中で指されたところだけ読む）
   ├ features.md    満たすべき機能（必須 / optional）
   ├ data.md        ディレクトリ・記録・メモ・履歴の形
   └ transport.md   Sink の契約・Transport・受け口の API
```

## 3. 同梱のテンプレートと script

| パス | 中身 |
|---|---|
| `assets/templates/core/` | 共通のコア（JS）: Sink の契約と `submitFeedback`・採番・redact・記録の組み立て（`feedback-core.mjs`）、Sink（ファイル・メモリ・HTTP）、HTTP の受け口（`feedback-dev-server.mjs`）、`node:test`（36 件 + 1 skip） |
| `assets/templates/core-ts/` | コアの TypeScript 版（ファイル・メモリの Sink・採番・パス検査）と vitest の契約テスト（32 件） |
| `assets/templates/adapters/web/dom/` | DOM 版のフィードバックモード（`feedback-mode.js`）・開発ビルドの入口・暫定のログ |
| `assets/templates/adapters/web/astro/` | Astro の Dev Toolbar 統合（受け口）と toolbar アプリ・dist の検査 |
| `assets/templates/adapters/desktop/electron/` | main の IPC の受け口・既存の開発用メニューに足す例 |
| `assets/templates/adapters/mobile/react-native/` | `FeedbackProvider` / `FbTarget`・Dev Menu に足す例 |
| `assets/templates/adapters/mobile/flutter/` | `FeedbackRoot` / `FbTarget` |
| `assets/templates/data/` | 送信 body と記録の frontmatter の JSON Schema・記録とメモの例 |
| `assets/templates/review/` | Review の指摘・人が足す選択肢・判断の記録の例 |
| `scripts/detect.sh` | タイミングの判定（読み取りだけ） |
| `scripts/copy-core.sh` | `core/` を導入先にコピーして `node --test` を通す |
| `scripts/check-no-feedback-in-prod.sh` | 本番の成果物にフィードバックモードの痕跡が無いかを grep |
| `scripts/review.mjs` | Review の cli（`node:test` 19 件） |
| `scripts/decisions.py` | 判断の記録の検査・照合・Review からの写し（unittest 21 件） |

## 4. 共通の注意

- 導入先のコードを書き換える、記録を archive する、外部（tracker・サーバ）へ送る前には、方針を人に示して承認を取る。archive はオーケストレータだけが行う。
- script（`scripts/*.sh`・`scripts/review.mjs`・`scripts/decisions.py`）は読解・書き換え・再実装せず、そのまま実行する。失敗したら stderr の指示に従い、原因を分析して人に報告する。
- 各 adapter の検証状況は、そのファイルの冒頭と `references/setup/adapter.md` §2。未検証の adapter を使うときは、そう人に伝え、本番の成果物の検査を必ず通す。
