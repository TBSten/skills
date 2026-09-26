---
name: release-note
description: >-
  example-lib のリリースノートとリリース準備資料を作り、リリース手順 (版 bump → GitHub Release を
  published → publish.yml が Maven Central へ publish) を案内する。前バージョンからの変更を
  git log / merged PR から集め、.local/v<version>-release-note/ にまとめる。
  「リリースノート」「release note」「リリース準備」「次のバージョンの変更をまとめて」と言われたら使う。
---

# release-note

成果物は `.local/v<version>-release-note/` に置く (`.local/` はコミットしない)。

| ファイル | 内容 | 作るのは |
|---|---|---|
| `commits.txt` / `prs.json` / `range.txt` | 生の変更一覧 | `scripts/collect-changes.sh` |
| `all.md` | 全変更の分類表 (PR / issue 対応) | AI |
| `release-note.md` | **GitHub Release の本文にそのまま貼るもの**。英語 + `<details>` 内に日本語全訳 | AI |

スクリプトは**読解・書き換え・再実装せず、そのまま実行する。**

## Step 1 — 変更を集める

```bash
bash .claude/skills/release-note/scripts/collect-changes.sh <version>
# 前回タグを明示するなら: --prev v0.1.0
```

`OK` 行で件数を確認する。各 PR の分類・概要は `prs.json` の本文 (body) を根拠にする。
PR を経ない直接 commit も `commits.txt` から拾う。前回のリリースがあれば公開版の形式を手本にする:

```bash
gh release view v<prev> --json body --jq .body
```

## Step 2 — all.md (全変更リスト、日本語)

- 冒頭: 比較範囲 (`range.txt` の内容) と compare リンク
- セクション: ⚠️ 破壊的変更 → 🎉 新機能 → 🐛 修正 → 📚 ドキュメント → 🔧 そのほか (テスト / ビルド / CI)
- 表 `| 内容 | PR | 概要 |`。概要は PR 本文と突き合わせた事実だけ書く
- 公開 API の変化は `api/*.api` の差分 (`git diff v<prev> -- '**/api/*.api'`) と突き合わせ、漏れがないか確認する

## Step 3 — release-note.md (GitHub Release 本文)

```
<英語の intro。1〜2 文、控えめに。pre-release なら "pre-release for early testing" の一文>

# ✨ Highlights
## ⭐️⭐️⭐️ <最重要の変更>   ← 破壊的変更なら見出しに ⚠️、本文に **Migration:** 段落
## ⭐️⭐️☆ <変更>
## ⭐️☆☆ Other fixes & improvements   ← 細かいものは bullet でまとめる

# 🚧 Known Issues   ← あるときだけ。1 行 + issue リンク

<details>

<summary> 日本語 </summary>

(上のすべてのセクションの日本語全訳。見出しは ## レベル)

</details>

# 📝 What's Changed
* <GitHub の自動生成 PR 一覧>

**Full Changelog**: https://github.com/<owner>/<repo>/compare/v<prev>...v<version>
```

- ⭐️ は辛口の絶対評価。⭐️⭐️⭐️ はせいぜい 2〜3 個
- Highlights に載せる基準: 「**利用者の書くコードが変わる / できることが増える**変更か」。
  CI・内部リファクタ・ドキュメント再構成は本文に書かず What's Changed に任せる
- 利用者の言葉で書く。実装の仕組み (なぜ直ったか) は書かない
- ドキュメントへのリンクは実在を確認してから入れる (英語側は `/<repo>/...`、日本語側は `/<repo>/ja/...`)
- `<summary>` の直後に空行を入れる (無いと GitHub が中の Markdown を描画しない)

## Step 4 — セルフレビュー (必須)

```bash
python3 .claude/skills/release-note/scripts/ng_words.py .local/v<version>-release-note/release-note.md
```

`OK` になるまで言い換える。そのうえで subagent に「example-lib を初めて使う利用者」として
release-note.md を読ませ、意味の取れない用語・前提知識を要求する説明・英日の情報量の差を列挙させる。
新しい NG 表現が見つかったら `scripts/ng_words.py` の `NG_WORDS` に足して蓄積する。
「説明を足す」系の指摘は本文を膨らませるので、まず同じ分量での言い換えを検討する。

## Step 5 — リリース手順を案内する

AI は手順を案内して止まる。push・Release の作成はユーザーが行う (明示的に頼まれたときだけ代行する)。

1. `bump-library-version` skill で `gradle/libs.versions.toml` の `example-lib` を `<version>` にして commit、push
2. CI が緑であることを確認する
3. tag `v<version>` で GitHub Release を作成し、本文に `release-note.md` だけを貼って **published** にする
   (pre-release なら "Set as a pre-release")
4. `.github/workflows/publish.yml` が `release: published` で起動し、tag と catalog の版が一致することを
   確かめてから Maven Central に publish する。起動しなければ Actions から `workflow_dispatch` で手動実行
5. 数十分後に https://central.sonatype.com/artifact/<group-id>/example-lib-core で新しい版が見えることを確認する

## チェックリスト

- [ ] `commits.txt` の全 commit / 全 merged PR が all.md に載っている
- [ ] Highlights に利用者に関係の無い変更が混ざっていない
- [ ] 日本語の `<details>` が英語側の全セクションを網羅している
- [ ] 破壊的変更に Migration 手順がある
- [ ] ドキュメントリンクの実在を確認した
- [ ] `ng_words.py` が `OK`、subagent レビューの指摘を反映した
