---
name: github-get-attachment-url
description: 同梱の決定的な Python Playwright スクリプトで、画像、動画、文書、アーカイブなど GitHub が対応するローカルファイルを TBSten/actions-test の Issue 下書きへアップロードし、Issue を作成せずに user-attachments URL または Markdown を取得する。GitHub にファイルをアップして URL 化したい、添付 URL を取得したいときに使用する。
metadata:
  status: Archived
  group: Git / GitHub
---

# GitHub 添付 URL を取得

モデルでブラウザー操作を組み立てず、同梱ランナーだけを実行する。Playwright MCP、Browser Use、Computer Use、`browser-use` Agent、非公式 GitHub API は使用しない。`scripts/upload.py` を読解・書き換え・再実装せず、そのまま使う。

## 入力

- 1 件以上のローカルファイルの絶対パス
- 出力形式: `direct_url` または `markdown`。既定は `direct_url`

対象ファイルや出力形式が不明な場合だけ質問する。アップロード先は常に `https://github.com/TBSten/actions-test/issues/new` とし、リポジトリは質問しない。

## 実行

1. この `SKILL.md` があるディレクトリを `SKILL_ROOT` とする。Claude Code では `${CLAUDE_SKILL_DIR}` を使う。
2. ファイルパスを個別の引数として安全に引用し、次だけを実行する。

```bash
"$SKILL_ROOT/scripts/run.sh" --format direct_url "/absolute/path/to/file"
```

3. 標準出力の JSON だけを判定する。
   - `ok=true`: `results` をファイル名ごとに返し、Issue を作成していないと伝える。
   - `status=setup_required`: 「専用キャッシュへ Playwright 1.61.0 と Chromium を初回インストールしてよいですか？」と一度だけ確認する。許可されたら、同じ引数の先頭へ `--allow-install` を追加して再実行する。
   - `status=python_required`: Python 3.11 以上が必要と伝える。
   - `status=login_timeout`: Playwright のブラウザーで GitHub にサインインしてから再実行するよう伝える。
   - その他の失敗: `error` をそのまま伝える。`diagnostic` があればパスも示す。

初回または GitHub セッション失効時、ランナーはブラウザーを表示して最大 5 分待つ。標準エラーに `ACTION_REQUIRED` が出たら、ユーザーへ「開いたブラウザーで GitHub にサインインしてください」とだけ伝え、ランナーの完了を待つ。認証情報を代わりに入力しない。

## 安全性

- ファイルのアップロードは外部送信である。ユーザーが対象ファイルと GitHub へのアップロードを明示していなければ、ランナー実行直前に確認する。
- 機密情報、認証情報、個人識別情報、金融・医療・法務・人事情報を含む可能性があるファイルは、明示的な承認なしにアップロードしない。
- ランナーは Issue を作成しない。`issue_created` が `false` であることを完了報告に含める。
