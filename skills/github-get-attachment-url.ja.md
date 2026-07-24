# github-get-attachment-url

ローカルファイルを GitHub にアップロードし、Issue を作成せずに `user-attachments` URL を取得する。

## インストール

```sh
gh skill install tbsten/skills github-get-attachment-url
```

## 概要

ローカルファイル(画像・動画・文書・アーカイブなど GitHub が添付として受け付けるもの)を、公開アクセス可能な GitHub ホスト URL に変換するスキル。同梱の決定的な Python + Playwright ランナーが GitHub の **Issue 下書き** を開いてファイルを添付し、生成された `user-attachments` URL または Markdown を読み取る。**Issue は送信しない** — 下書きのアップロード副作用だけを利用する。

モデルはブラウザー操作を組み立てず、同梱ランナーを実行するだけ。そのため結果は再現可能で、その場限りの自動化のようにページ変更で挙動がぶれない。

## 使うタイミング

- 「このファイルを GitHub にアップして URL がほしい」
- 「この画像 / 動画 / PDF の添付 URL がほしい」
- 「ローカルのスクショを GitHub ホストのリンクにしたい」
- 「このファイルを埋め込む Markdown がほしい」
- 「Issue を作らずに user-attachments URL を取得したい」

## 仕組み

1. `scripts/run.sh` が Python 3.11+ を検出し、初回のみ Playwright `1.61.0` + Chromium を専用のユーザーキャッシュ(`$XDG_CACHE_HOME/github-get-attachment-url`)へインストールする。インストールは `--allow-install` を渡したときだけ実行され、初回は `setup_required` を返して確認を求める。
2. `scripts/upload.py` が可視の Chromium で `https://github.com/TBSten/actions-test/issues/new` を開き、GitHub への手動サインインを最大 5 分待ち(プロファイルはローカルに保存され次回以降に再利用)、各ファイルを Issue 下書きに添付してアップロード完了を待つ。
3. 判定に使うのは stdout の JSON だけで、画面上のブラウザー状態は解釈しない。

## 使い方

```sh
"$SKILL_ROOT/scripts/run.sh" --format direct_url "/absolute/path/to/file"
```

- `--format direct_url`(既定)は生の添付 URL を、`--format markdown` は Markdown スニペットを返す。
- 1 件以上の絶対パスを、それぞれ個別の引数として引用して渡す。
- ランナーが `setup_required` を返したときだけ、先頭に `--allow-install` を一度付けて再実行する。

## 出力(stdout の JSON)

| status | 意味 |
|---|---|
| `ok=true`(`status=completed`) | `results[]` に `{ file, url }` または `{ file, markdown }`。`issue_created` は常に `false`。 |
| `setup_required` | Playwright + Chromium の初回インストールが必要。`--allow-install` を付けて再実行。 |
| `python_required` | Python 3.11+ が使えない。 |
| `login_timeout` | 開いたブラウザーで GitHub にサインインしてから再実行。 |
| `ui_changed` / `upload_timeout` / `upload_failed` | GitHub の UI かアップロードが失敗。`diagnostic` にスクリーンショットのパスが入ることがある。 |

## 前提条件

- Python 3.11 以上
- 対話的にサインインできる GitHub アカウント(初回 / セッション失効時に実ブラウザーのウィンドウが開く)
- 初回に Playwright + Chromium をインストールするためのネットワーク接続

## 安全性

- ファイルのアップロードは外部送信である。ユーザーがアップロードを明示していなければ、実行前に対象ファイルを確認する。
- 機密情報・認証情報・個人識別情報・金融/医療/法務/人事情報を含む可能性があるファイルは、明示的な承認なしにアップロードしない。
- ランナーは Issue を作成しない。完了報告に `issue_created: false` を含める。

## セルフテスト

Markdown 差分抽出と URL 抽出ロジックを検証する、依存不要のセルフテストを同梱している:

```sh
sh scripts/run.sh --self-test   # -> {"ok": true, "status": "self_test_passed"}
```
