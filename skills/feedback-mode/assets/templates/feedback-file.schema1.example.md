<!-- 記録の例（schema 1。submitFeedback に meta を渡したとき）【実績: Web 参照実装の形を汎用版の節に合わせたもの】。
     frontmatter の無い記録（feedback-file.example.md・手書きのメモ）は旧形（schema 0）として読む。両方が同じ置き場に混ざってよい。 -->
---
schema: 1
kind: "element"
screen: "/guides/setup/"
target: "本文/手順"
source: "<docs-dir>/guides/setup.md:42-45"
created: "2026-10-29T01:23:45.000Z"
dirty: false
stale: true
omitted: ["shots","env","view"]
---

# <project> docs: 画面へのフィードバック

案内: <project> の docs サイトへのフィードバック。ソースは <docs-dir>/、文の決まりは <正本 docs の節>。（この 1 行が Agent への案内。導入先ごとに書き換える）

注意: 画面を開いた後にソースが変わっています。記録の位置・文言は今のソースとずれているかもしれません。

（オプションでオフ: スクリーンショット・環境・画面に出ている情報）

## フィードバックの文

> 手順 3 の前に、設定ファイルの場所を書いてほしい

## 選んだ位置

- 画面: `/guides/setup/`
- 部品の道筋（data-fb / testID など）: `本文/手順`
- 要素: ol「設定ファイルを開く…」
- セレクタ: `main > ol:nth-of-type(2)`
- XPath: `/html[1]/body[1]/main[1]/ol[2]`
- ソースの行: `<docs-dir>/guides/setup.md:42-45`
- 矩形: x 312・y 640・720 × 96（CSS px / dp。ウィンドウの左上から）
- 要素の状態:
  - なし

---

以下はデバッグ情報。

## 今の画面

- url: http://localhost:4321/guides/setup/
- viewport: 1280x800
- dpr: 2

## ログ

- 1 件をすべて入れた

### error / warn の要約（0 件）

- なし

### 一覧（古い順。新しいものが下）

```text
01:23:40.120 info  [vite] connected.
```
