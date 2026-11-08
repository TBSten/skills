---
name: visual-explanation
description: >
  PR の本文・チケット (issue / Notion / Linear など) の説明・人間向けレポートに、
  読む人が最初の 1 枚で要点をつかめる図・スクショを付ける。
  HTML 雛形 (ライト / ダーク対応トークン付き) で描き、ヘッドレスの Chrome で section ごとに PNG を撮り、
  PR ブランチに一度だけコミットして SHA 固定 URL を作り (最終ツリーには残さない)、本文が決まりに沿っているかを検証する。
  PR テンプレートに「1 枚目に図を置く」枠を入れるセットアップも含む。
  方針 (いつ・何を見せるか) は rule `visual-explanation` と組み合わせて使う。
  Use when requested: "PR に図を入れて", "PR にスクショを貼って", "チケットに図解を付けて",
  "説明を図にして", "Before / After を撮って", "visual explanation", "PR に画像を添付",
  "PR テンプレートに図の枠を入れて".
metadata:
  status: Experimental
  group: Git / GitHub
---

# visual-explanation

PR やチケットの説明に、見た目で分かる 1 枚目を付ける。描く・撮る・添付する・検証するの決定的な部分は `scripts/` にあるので、**script を読解・書き換え・再実装せず、そのまま実行する**。AI の仕事は「何を見せるか決める」「HTML に中身を書く」「本文を書く」の 3 つ。

## Usage

- PR を作る / 更新するとき、チケットを起票するとき、人間向けの説明を書くときに使う。
- 数行の変更・CI だけの変更など、画像が要らない種類もある。Step 1 で判断し、要らなければ Step 2〜4 を飛ばす。
- プロジェクトに初めて入れるときは、先に「セットアップ」を 1 回行う。

## セットアップ (プロジェクトごとに 1 回)

1. 方針の rule を入れる (常に読まれるように):
   ```sh
   curl -fsSL https://rules.tbsten.me/i | bash -s -- visual-explanation
   ```
2. PR テンプレートに枠を入れる:
   ```sh
   bash "${CLAUDE_SKILL_DIR}/scripts/setup-pr-template.sh"
   ```
   - `CREATED` / `ALREADY` ならそのまま次へ。
   - `NEEDS_MERGE <path>` (テンプレートごとに 1 行) なら、それぞれの「何をしたか」に当たる節の直下に、出力された snippet を足す (既存の節の名前・順番は変えない)。
3. 最初の 1 本の PR を、型の見本になるように丁寧に作る。画像が PR 上で表示されるか (特に非公開リポジトリ) を確かめ、合わなかった点は rule ファイルに戻す。

## 手順

### Step 1: 見せ方を決める

rule `visual-explanation` の「見せ方は変更の種類で選ぶ」の表から、変更・内容に合う見せ方を選ぶ (rule が入っていなければ先に「セットアップ」の 1 を行う)。選んだ見せ方を描く部品は [references/formats.md](references/formats.md) にある。画像が要らない種類なら、diff のコードブロックか `<details>` の実際の出力を本文に書いて Step 5 へ進む。表で「画像なしでよい」(CI だけ・設定だけ) を選んだときは、Step 5 の検証に `--no-visual` を付ける。

### Step 2: HTML に描く

1. [references/formats.md](references/formats.md) の「1 枚の作り方」に従い、1 枚ごとに伝えたい 1 文と型を決める。1 文が書けないものは図にせず本文に書く。
2. `${CLAUDE_SKILL_DIR}/assets/template.html` をリポジトリの外 (`.local/visuals/<topic>/` や共有フォルダ) にコピーする。
3. 例の section を消し、1 枚にしたい単位ごとに `<section id="01-...">` を書く。id はそのままファイル名になる。
4. 中身は実際の実行・実データから書く。文字量・色・コードの ⭐️・推奨度は formats.md の「3. 書く」に従う。Before / After は同じ入力で main とこのブランチを実行した結果を使う。

### Step 3: 撮る

```sh
node "${CLAUDE_SKILL_DIR}/scripts/capture.mjs" <html> <out-dir> [--width=420] [--scale=3] [--schemes=light,dark]
```

Chrome / Chromium の headless コマンドで撮る (npm パッケージは要らない)。見つからないときは `CHROME_PATH` に実行ファイルのパスを入れる。

`OK` の後に撮った PNG の一覧が出る。`WARN:` (見出しの数・40 文字超え・横のはみ出し) が出たら、その行に従って HTML を直して撮り直す。最後に Read で 1 枚ずつ見て、formats.md の「4. 撮る前のセルフチェック」を確かめる。

画面収録できない環境で動きを見せるときは、[references/formats.md](references/formats.md) の「実ログから GIF を作る」に従ってコマを撮り、GIF にする:

```sh
bash "${CLAUDE_SKILL_DIR}/scripts/make-gif.sh" [--delay=800] [--width=840] <out.gif> <out-dir>/frame-*-light.png
```

### Step 4: 添付する

**PR の場合**: PR ブランチで実行する。

```sh
bash "${CLAUDE_SKILL_DIR}/scripts/attach-pr-assets.sh" --push <out-dir>/01-xxx-light.png <out-dir>/01-xxx-dark.png
```

- ファイル名は英数字と `. _ -` だけにする (URL にそのまま入るため)。`.github/pr-assets/` に同名ファイルがあると中断するので、そのときはリネームする。
- 出力された Markdown の画像行をそのまま本文に使う。alt は「何が分かる図か」の 1 文に書き換える。
- `gh` が無い・公開 / 非公開が判定できない環境では `--visibility=public` / `--visibility=private` を付ける。

**チケットの場合**: ツールが画像添付できるなら PNG を添付し、HTML を埋め込めるなら HTML を埋め込む。どちらも無理なら PR と同じく `attach-pr-assets.sh` で SHA 固定 URL を作る。

### Step 5: 本文を書いて検証する

- 「何をしたか」(チケットなら「背景」) の節の頭に 1 枚目 (ライト版) を置き、ダーク版は `<details><summary>ダークモード</summary>...</details>` に入れる。文章は図の補足にする。
- 撮り方 (画面収録 / 実ログから描いた再現 / headless 描画) と、どの PR・コミット範囲・コマンドから作ったかを書く。
- 本文をファイルに書き出して検証する:
  ```sh
  node "${CLAUDE_SKILL_DIR}/scripts/check-body.mjs" [--no-visual] <body.md>
  ```
  `NG:` が出たら、その行の内容に従って本文を直し、`OK` になるまで繰り返す。

## リソース

| パス | 用途 |
|---|---|
| `references/formats.md` | 見せ方と template.html の部品の対応、1 枚の作り方 (1 文・型・文字量・色・⭐️・推奨度)、実ログから GIF を作る手順 |
| `assets/template.html` | ライト / ダーク対応トークン付きの HTML 雛形 |
| `assets/pull_request_template.md` | PR テンプレートの雛形 (setup-pr-template.sh が使う) |
| `scripts/capture.mjs` | section[id] ごとに PNG を撮り、見出し・はみ出しを `WARN:` で知らせる (Chrome / Chromium が必要) |
| `scripts/make-gif.sh` | 連番の PNG を GIF にする (ffmpeg が必要) |
| `scripts/attach-pr-assets.sh` | 画像を一度コミットして消し、SHA 固定 URL を出す |
| `scripts/setup-pr-template.sh` | PR テンプレートに枠を入れる |
| `scripts/check-body.mjs` | 本文の検証 (見た目の有無・alt・SHA 固定・URL の形) |
