---
name: review-docs
description: >
  このリポジトリ (tbsten/skills) の人間向けドキュメント (README.md / README.ja.md と
  skills|rules|prompts/<name>.md / .ja.md) を、機械検査では拾えない観点でレビューして直すためのスキル。
  詳細ドキュメントの記述が SKILL.md / RULE.md / PROMPT.md と同梱ファイルの実体と食い違っていないか、
  .claude/rules/docs-writing.md の「文章の方針」に沿っているかを照合する。
  このスキルはリポジトリのコントリビューター向けであり、skill / rule / prompt の利用者向けではない。
  Use when requested: "ドキュメントをレビュー", "README を見直して", "詳細ドキュメントの品質チェック",
  "docs review", "ドキュメントと実装がずれていないか", "review docs".
---

# review-docs: 人間向けドキュメントのレビュー

機械で検査できること (日英の組・構成・リンク・言い換え・文の長さ・README と SSoT の一致) は
`scripts/docs-check/check.sh` が見る。このスキルは、その残りの「読んで判断する」部分を受け持つ。

書き方の決まりの正は `.claude/rules/docs-writing.md`。このスキルに決まりを書き写さない。

## 手順

### 1. 機械検査を通す

```bash
scripts/docs-check/check.sh --base origin/main
```

失敗したら先に直す (言い換え辞書の指摘は `--fix` で直る)。機械検査が通らないままレビューに入らない。

### 2. 対象を決める

- 名前を渡されたら、その skill / rule / prompt の詳細ドキュメント (日英) と README の行
- 何も渡されなければ、`git diff --name-only origin/main...HEAD` で変わった skill / rule / prompt
  (詳細ドキュメントだけでなく SKILL.md・scripts などの実体が変わったものも含める。実体だけが変わると、ドキュメントが古くなる)
- 「全部」と言われたら、`python3 scripts/docs-check/docs_check.py list-ja` の全件

### 3. 実体と照合する (日本語版だけを読む)

英語版は訳なので、日本語版 (原本) を実体と照合する。1 件ずつ、次を確かめる。

| 観点 | 照合先 |
|---|---|
| 同梱ファイル・script 名・ディレクトリの説明 | `skills/<name>/` の実際のファイル (`ls -R`) |
| script のオプション・出力・前提条件 | script の usage (`--help`) と冒頭のコメント |
| 何をするか・使うタイミング | SKILL.md の `description` と本文 |
| status・group | SKILL.md の `metadata` (README とは check.sh が照合済み) |
| 件数・対応環境・バージョン | SKILL.md・references の記述、テストの数 |
| 関連 skill へのリンク | リンク先の skill が今もその役割を持っているか (Archived になっていないか) |

食い違いは、実体が正しいなら日本語版を直す。実体の方が誤っていそうなら直さずに報告する。

### 4. 文章の方針と照合する

`.claude/rules/docs-writing.md` の「文章の方針」を 1 項目ずつ当てる。🆖/🆗 の例は、似た別の場面にも当てはめる。
「該当なし」で済ませず、各項目で見た箇所の数を数えて報告に書く。

### 5. 直す

1. 日本語版を直す
2. 同じ変更を英語版に訳して入れる (節の構成を 1 対 1 に保つ)
3. README の説明を変えたなら、README.md / README.ja.md の両方を直す
4. `scripts/docs-check/check.sh --base origin/main` を通す

### 6. 決まりを育てる

レビューで見つけた問題が、次も起きそうなら決まりに足す。足し方は `.claude/rules/docs-writing.md` の「人に直されたら」と同じ。

### 7. 報告する

対象ごとに、直したもの (ファイル・行・理由)、直さずに報告するもの (実体が誤っていそうなもの・判断に迷ったもの)、足した決まりを並べる。
