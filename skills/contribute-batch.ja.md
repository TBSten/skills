# contribute-batch

プロジェクトで得た複数の知見を skill / rule / prompt に仕分けし、[TBSten/skills](https://github.com/TBSten/skills) リポジトリへ 1 つの PR としてまとめて登録するための Claude Code スキル。

## インストール

```sh
gh skill install tbsten/skills contribute-batch
```

## 概要

このスキルはオーケストレータとして動作する。現在のプロジェクトから複数の知見を収集し、それぞれを skill / rule / prompt (または見送り) に仕分け、並列 subagent で成果物を一括作成し、TBSten/skills リポジトリへの 1 つの PR にまとめる。種別が決まった単一の成果物を登録するだけなら contribute-skill / contribute-rule / contribute-prompt を使う。

## 使い方

スキルをインストール後、現在のプロジェクトから知見の一括登録を依頼する:

```
知見をまとめて contribute して: このプロジェクトのセットアップ手順と運用規約
```

```
contribute batch: CLAUDE.md と .claude/rules/ の知見を仕分けして PR にまとめたい
```

## 実行内容

1. 現プロジェクトから複数の知見を **収集** (CLAUDE.md, ルール, スキル, コードベース)
2. 知見ごとに skill / rule / prompt / 見送りを **仕分け** し、結果を表で提示して確認
3. 同梱の `scripts/setup-workspace.sh` で **ワークスペースを準備** (clone + 作業ブランチ、冪等)
4. 成果物ごとに subagent を **並列起動** (最大 5) し、リポジトリの contribute ガイドに従い作成
5. README 更新・セルフレビュー・意味単位の commit を経て **1 つの PR を作成**

## 前提条件

- `git` がインストール済み
- `gh` CLI がインストール・認証済み (`gh auth login`)
- TBSten/skills リポジトリ (または fork) への書き込み権限
