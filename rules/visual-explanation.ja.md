# visual-explanation Rule

[English](./visual-explanation.md)

人間に見せる説明 (PR の本文・チケットの説明・レポート) の頭に、図・スクショ・動画・実際の出力など見た目で分かるものを付けさせる
[Claude Code](https://docs.anthropic.com/en/docs/claude-code) 用ルール。

## クイックスタート

### 1. ルールを入れる:

```bash
curl -fsSL https://rules.tbsten.me/i | bash -s -- visual-explanation
```

### 2. (推奨) 作業部分を担う skill を入れる:

```bash
gh skill install tbsten/skills visual-explanation
```

[`visual-explanation` skill](../skills/visual-explanation.ja.md) に、HTML 雛形・撮影 script・PR に SHA 固定 URL で画像を付ける script・本文の検証・PR テンプレートのセットアップが入っている。
rule は skill が入っていればそれを使うよう指示する。

## 内容

パス指定の無い、常に読まれるルール。Claude に次を指示する:

| 項目 | 内容 |
|---|---|
| いつ | PR の本文・チケットの説明・人間向けレポートを書くとき |
| 何を見せるか | 変更の種類で選ぶ: 見た目・出力の変更は Before / After、バグは仕組みの図、新 API は 1 枚まとめ、動くものは GIF、削除は消える / 残るの図、起票は現状 / 目指す姿の図、数行は diff、見た目が無ければ `<details>` の出力、CI だけなら無し |
| 作り方 | 描く前に伝えたい 1 文を決めて見出しにする (書けなければ図にしない)・1 枚 1 メッセージ・図の中は見出しと短いラベルだけ・意味のある色だけ 2〜3 色・alt に何が分かるか・実際の実行から作る (モック禁止)・作り方を正直に書く・HTML + ヘッドレス Chrome (scale 2〜3)・420px 前後で読める・プロジェクトのトークン |
| 置き場所 (PR) | PR ブランチの `.github/pr-assets/` にコミットし次のコミットで消す。本文は SHA 固定 URL (ブランチ名 URL は使わない)。1 枚 500 KB 以下が目安 (merge commit なら main の履歴に残り、squash merge なら URL のコミットは PR からしか辿れない) |
| 置き場所 (チケット) | ツールに添付。無理なら SHA 固定 URL |
| 位置 | 「何をしたか」(チケットは「背景」) の頭に 1 枚目。文章は補足 |

## ファイル

ルールは `RULE.md` だけ (`.claude/rules/visual-explanation.md` に入る)。参照ファイルはあえて持たない。
インストーラはカレントディレクトリのファイルを上書きするので、`.github/pull_request_template.md` を同梱すると既存テンプレートを消してしまうため。
テンプレートは skill の `setup-pr-template.sh` が扱う。

## 経緯

あるプロジェクトで、スレッドでの一言をきっかけに PR に図・スクショが入るようになり、Claude のプロジェクトメモリ経由で全スレッドに広がった
(画像入りの PR が 0 / 9 → 19 / 27、他に文字で見せたものが 6)。運用がメモリにしか無く、メモリが消えたり別の環境で動いたりすると止まるおそれがあるので、リポジトリに書く形にした。
