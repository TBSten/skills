# contribute-prompt

プロジェクトの知見を [TBSten/skills](https://github.com/TBSten/skills) リポジトリに一回限りのプロンプトとして登録するための Claude Code スキル。

## インストール

```sh
gh skill install tbsten/skills contribute-prompt
```

## 概要

プロジェクトで得た知見を一回限りのプロンプト — raw URL 経由で配布され、ユーザーが Claude Code にコピペするだけで実行できる Markdown 指示書 (スキルのインストール不要) — としてパッケージングし、TBSten/skills リポジトリへの PR 作成までを自動化する。セットアップやスキャフォールドのような 1 回で完結する手順に向く。

## 使い方

スキルをインストール後、現在のプロジェクトから知見の登録を依頼する:

```
知見をプロンプトとして登録して: この CI セットアップ手順
```

```
このスキャフォールド手順をワンショットプロンプトにしたい
```

## 実行内容

1. 現プロジェクトから知見を **収集** (CLAUDE.md, ルール, スキル, コードベース)
2. 知見を self-contained な一回限りのプロンプトに **整理**
3. TBSten/skills を一時ディレクトリに **clone**
4. リポジトリの規約に従い `prompts/<name>/PROMPT.md`・詳細ドキュメント・README 行を **作成**
5. TBSten/skills に対して **PR を作成** し、タイトル・本文フォーマットを検証

## 前提条件

- `git` がインストール済み
- `gh` CLI がインストール・認証済み (`gh auth login`)
- TBSten/skills リポジトリ (または fork) への書き込み権限
