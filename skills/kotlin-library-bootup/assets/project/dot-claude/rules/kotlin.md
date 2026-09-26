---
paths:
  - "**/src/**/*.kt"
---

# Kotlin コード規約 (ライブラリ本体)

## 可視性

ライブラリの宣言は次の 4 段階で管理する。宣言を足すたびに、**必要最小限の可視性**を検討する。

| 可視性 | 意味 |
|---|---|
| `private` | そのファイル・宣言の中だけで使う |
| `internal` | そのモジュールの中だけで使う |
| `public` + `@InternalExampleLibApi` | ライブラリの別モジュールから使うために public にしているが、利用者には触ってほしくない |
| `public` | 利用者が使う公開 API |

- `explicitApi()` が有効なので、公開宣言には `public` を**省略せずに書く** (省略するとコンパイルエラー)
- まだ形が固まっていない公開 API には `@ExperimentalExampleLibApi` を付ける。利用者は opt-in が必要になる
- `@InternalExampleLibApi` / `@ExperimentalExampleLibApi` は自モジュールでは convention plugin が
  opt-in 済み。自モジュールのコードに `@OptIn` を書かない
- 公開 API を変えたら `./gradlew apiDump` を回し、`api/*.api` (klib は `*.klib.api`) の差分をコミットする。
  差分は「意図した公開面の変化か」を 1 行ずつ確認する
- **見るのは修飾子ではなく到達可能性。** `internal class` の中の `public` メンバは外から触れない

## 公開 API の設計

- 破壊的変更は Maven Central に出た瞬間に戻せない。迷ったら `internal` か `@ExperimentalExampleLibApi` にしておく
- `data class` を公開するときは `copy` / `componentN` も公開 API になる (将来のプロパティ追加が破壊的変更になる) ことを考える
- `sealed` / `enum` にサブタイプ・エントリを足すと利用者の `when` が壊れる。公開する前に拡張予定を考える
- デフォルト引数を増やすとバイナリ互換性が壊れる場合がある。`apiCheck` の差分で確認する

## コメント

コメントは英語で書く。

### 公開宣言 (`public`、`@InternalExampleLibApi` を含む)

必ず KDoc を書く。形式:

`````kt
/**
 * <One-line summary. Up to 150 characters.>
 *
 * <More details. Optional.>
 *
 * ## Example
 *
 * ```kt
 * // A minimal, compilable usage example.
 * ```
 *
 * @param <optional>
 * @return <optional>
 * @throws <optional>
 * @see <optional, but strongly recommended when a related declaration exists>
 */
`````

- `## Example` は公開宣言では 1 つ以上必須。例外: `override` した宣言 (ドキュメントは override 元から引き継がれる)
- KDoc は Dokka で API リファレンス (`/api-docs/`) になる。利用者が読む文章として書く

### 公開宣言以外

- `private` / `internal` には、本当に背景が必要な箇所だけコメントを書く。増やしすぎない
- 式の中のコメントには「何をしているか」ではなく「**なぜ**そうしているか」を書く

## 例外

- ライブラリが投げる例外は専用のクラスにする。名前は `ExampleLib<何が起きたか>Exception`。
  スタックトレースに一般名 (`IllegalStateException` 等) しか出ないと、利用者は自分のコードと
  ライブラリのどちらが悪いのか切り分けられない
- `check` / `require` / `error` / `checkNotNull` / `requireNotNull` で済ませない。専用の例外を定義する
- `message` を引数に取らない。値をプロパティで受け取り、文面は例外クラスの中で組む
  (利用者が `catch` した後にプロパティで判別できる / 文面を直す場所が 1 箇所に決まる)
- 文面は英語・ASCII のみ。「何が起きたか → なぜだめか → どう直すか」の順に書く
- ライブラリ自身の前提が崩れた (バグ) ときは、文面に「example-lib のバグであること」と
  報告先 `https://github.com/<owner>/<repo>/issues` を書く
- 例外の基底クラスは「利用者が取るべき行動」(入力を直す / 環境を直す / バグとして報告する) で分ける。
  基底を増やす前に分類を疑う
- 例外クラスは公開 API なので、KDoc と `## Example` を書く

## その他

- `as` を使わない。`as? ... ?: throw <専用の例外>` にする (素の `ClassCastException` は原因が伝わらない)
- `!!` を使わない。null になり得ない理由があるなら型で表す
- `commonMain` に置けるものは `commonMain` に置く。プラットフォーム固有の API が必要なときだけ `expect` / `actual`
