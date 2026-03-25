# contribute-rule

プロジェクトの知見を [TBSten/skills](https://github.com/TBSten/skills) リポジトリに rule として登録するための Claude Code スキル。

## 概要

プロジェクトで得た規約・ベストプラクティス・ガイドラインを再利用可能な Claude Code rule としてパッケージングし、TBSten/skills リポジトリへの PR 作成までを自動化する。

## 使い方

スキルをインストール後、現在のプロジェクトから知見の登録を依頼する:

```
この規約をルールとして登録して: Kotlin のコーディング規約
```

```
contribute rule: このプロジェクトのコミットメッセージルール
```

## 実行内容

1. 現プロジェクトから知見を **収集** (CLAUDE.md, ルール, コードベース)
2. 知見を rule 形式 (RULE.md) に **整理**
3. TBSten/skills を一時ディレクトリに **clone**
4. リポジトリの規約に従いルールファイルを **作成**
5. TBSten/skills に対して **PR を作成**

## 前提条件

- `git` がインストール済み
- `gh` CLI がインストール・認証済み (`gh auth login`)
- TBSten/skills リポジトリ (または fork) への書き込み権限
