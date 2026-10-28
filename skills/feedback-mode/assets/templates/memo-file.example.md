<!-- メモの例（references/build/memo.md）【実績: Web 参照実装の形を汎用化したもの】。置き場は <Inbox>/memo/<言語>/<ページのパス>.md。
     見える部分（frontmatter・ページのメモ・箇所のメモ）は Agent が読む用。末尾の HTML コメントの JSON は画面の読み直し用で、loop は読まなくてよい。
     メモは利用者のもの。loop は書き換えない・archive しない。 -->
---
schema: "feedback-memo/1"
page: "/ja/guides/setup/"
lang: "ja"
title: "セットアップ"
source: "<docs-dir>/ja/guides/setup.md"
updated: "2026-10-29T03:10:00.000Z"
---

# メモ: セットアップ

## ページのメモ

- 最終更新: 2026-10-29T03:10:00.000Z

- 読みやすさ: 手順の粒度がばらばら
- 優先度: 高

前半は初めての人向け、後半は移行する人向けに分けたほうが読みやすそう。

## 箇所のメモ

### 1. 手順 3

- 場所
  - 種類: 要素
  - source: <docs-dir>/ja/guides/setup.md:42-45
  - heading: ## 手順
  - excerpt: 設定ファイルを開く…
  - xpath: /html[1]/body[1]/main[1]/ol[2]
  - selector: main > ol:nth-of-type(2)
- 作成: 2026-10-29T02:58:00.000Z
- 最終更新: 2026-10-29T03:05:00.000Z

設定ファイルの場所が先に無いと迷う。

<!-- feedback-memo:data v1
{"page":{"path":"/ja/guides/setup/","lang":"ja","title":"セットアップ","source":"<docs-dir>/ja/guides/setup.md"},"note":{"values":{"読みやすさ":"手順の粒度がばらばら","優先度":"高"},"text":"前半は初めての人向け、後半は移行する人向けに分けたほうが読みやすそう。","updatedAt":"2026-10-29T03:10:00.000Z"},"spots":[{"id":"s1","locator":{"kind":"element","excerpt":"設定ファイルを開く…","source":"<docs-dir>/ja/guides/setup.md:42-45","heading":"## 手順","xpath":"/html[1]/body[1]/main[1]/ol[2]","selector":"main > ol:nth-of-type(2)"},"values":{},"text":"設定ファイルの場所が先に無いと迷う。","createdAt":"2026-10-29T02:58:00.000Z","updatedAt":"2026-10-29T03:05:00.000Z"}]}
-->
