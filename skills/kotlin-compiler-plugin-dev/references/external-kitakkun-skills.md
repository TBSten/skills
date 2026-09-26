# 外部リファレンス: kitakkun/kotlin-compiler-plugin-skills

FIR / IR の各 Extension の **API の使い方・実装手順** は、外部 skill [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) の `kotlin-compiler-plugin` skill が持つトピック別 guide を参照する。本ファイルはその対応表 (本リポジトリ内の SSoT)。

- 出典: [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (作者: kitakkun, **MIT License**)。本リポジトリには内容をコピーせず、リンクで参照する
- 基準: Kotlin 2.4.x。API の主張は JetBrains/kotlin のソースで裏取り済み (`EVIDENCE.md`)。Kotlin バージョン間の API 変更は `CHANGES.md` (いずれも存在するトピックのみ)
- `verification/` に 13 個の動く参考プラグインあり: <https://github.com/kitakkun/kotlin-compiler-plugin-skills/tree/main/verification>

## 取得方法

1. **ローカルにインストール済みならそちらを優先する**。以下のいずれかに該当すればインストール済み:
   - 利用可能な skill 一覧に `kotlin-compiler-plugin` がある
   - `~/.claude/plugins/cache/` 配下に `skills/kotlin-compiler-plugin/references/` がある (`find ~/.claude/plugins/cache -path '*kotlin-compiler-plugin/references*' -name guide.md` で確認)
2. 未インストールなら raw URL を WebFetch または `curl -fsSL <url>` で取得する:

```
https://raw.githubusercontent.com/kitakkun/kotlin-compiler-plugin-skills/main/skills/kotlin-compiler-plugin/references/<topic>/guide.md
```

`EVIDENCE.md` / `CHANGES.md` が必要な場合は同じディレクトリの `guide.md` をファイル名に置き換える (無いトピックもある)。

ユーザーにインストールを勧める場合は Claude Code 上で以下を案内する:

```
/plugin marketplace add kitakkun/kotlin-compiler-plugin-skills
/plugin install kotlin-compiler-plugin-skills@kotlin-compiler-plugin-skills
```

## トピック一覧 (24)

URL は `<base>/<topic>/guide.md`。`<base>` = `https://raw.githubusercontent.com/kitakkun/kotlin-compiler-plugin-skills/main/skills/kotlin-compiler-plugin/references`

### Foundation

| topic | いつ読むか |
|---|---|
| `compiler-plugin-bootstrap` | 新規プラグインの scaffold・hello-world・登録の全体像を確認するとき |
| `compiler-plugin-debugging` | `MessageCollector`、IR dump、デバッガ attach で挙動を可視化したいとき |
| `compiler-plugin-testing` | 公式テストインフラ (diagnostic テスト / IR box テスト) を書くとき |
| `gradle-plugin-integration` | `KotlinCompilerPluginSupportPlugin` で Gradle plugin として配布するとき |
| `multi-version-kotlin-support` | 1 つのプラグインで複数 Kotlin バージョンを支えるとき |

### FIR (K2 frontend)

| topic | いつ読むか |
|---|---|
| `fir-extensions-overview` | FIR の 17 Extension Point の全体像・FirSession ライフサイクル・選び方 |
| `fir-predicate-system` | アノテーションマッチング DSL (`annotated` / `parentAnnotated` 等)。ほぼ全 FIR extension で使う |
| `fir-additional-checkers-extension` | 独自のコンパイル時診断 (warning / error) を出すとき |
| `fir-declaration-generation-extension` | ソースから見えるクラス・関数・プロパティ・コンストラクタを合成するとき |
| `fir-supertype-generation-extension` | 既存クラスに supertype (interface / 基底クラス) を注入するとき |
| `fir-status-transformer-extension` | 既存宣言の modifier (visibility / modality / inline 等) を変えるとき |
| `fir-expression-resolution-extension` | 呼び出し解決に暗黙の extension receiver を注入するとき |
| `fir-session-components` | FIR extension 間で計算結果を共有するとき |
| `fir-type-attribute-extension` | 型にプラグイン独自の attribute (例: `@Positive Int`) を付けるとき |
| `fir-sam-conversion-transformer-extension` | SAM 変換をカスタマイズするとき (sam-with-receiver パターン) |
| `fir-assign-expression-alterer-extension` | `x = v` を任意の文 (例: `x.assign(v)`) に書き換えるとき |
| `fir-function-type-kind-extension` | 新しい関数型ファミリ (例: `@Composable () -> Unit`) を定義するとき |
| `fir-function-call-refinement-extension` | 呼び出し地点で戻り値型を精緻化するとき (dataframe 型推論パターン、advanced / unstable) |
| `fir-scripting-extensions` | `.kts` 風のスクリプト方言を定義するとき |
| `fir-repl-snippet-extensions` | REPL / Jupyter 風の方言 (snippet 間の可視性) を実装するとき |

### IR (backend)

| topic | いつ読むか |
|---|---|
| `ir-plugincontext-usage` | シンボル検索・診断報告・metadata 登録。IR extension を書くとき最初に読む |
| `ir-call-rewriting` | ユーザーコードの関数呼び出しを別の関数呼び出しに置換するとき |
| `ir-body-modification` | 既存関数の body を改変するとき (FIR で宣言した stub の body を埋める場合を含む) |
| `ir-synthetic-class-generation` | 新しい IR クラスを合成するとき (FIR 生成宣言の IR 側) |

## 本リポジトリの reference との使い分け

| 知りたいこと | 本リポジトリ (kotlin-compiler-plugin-dev) | kitakkun guide |
|---|---|---|
| 「似たことをやっている既存プラグインは?」 | `overview.md` / `patterns.md` / `details/` (30+ プラグインの前例調査、ソース URL 付き) | — |
| 「その Extension の API をどう書くか」 | `details/` の前例コード | 該当 `fir-*` / `ir-*` guide (最新 API の書き方) |
| Extension Point の選択 | `patterns.md` の選択ガイド + 前例 | `fir-extensions-overview` |
| 複数 Kotlin バージョン対応 | `compat-module-setup.md` / `source-set-separation.md` / `multi-version-workflow.md` / `version-gating.md` / `reflection-shim.md` (実運用パターン) | `multi-version-kotlin-support` を併読 (API 差分の吸収方針) + 各 guide の `CHANGES.md` |
| テスト | kctfork ベース (`kotlin-compiler-plugin-setup` の testing-patterns、`ci-matrix.md`) | `compiler-plugin-testing` (公式テストインフラ: diagnostic / box テスト) |
| デバッグ | `troubleshooting.md` (バージョン差分起因の失敗) | `compiler-plugin-debugging` (IR dump / MessageCollector / debugger) |

- 前例探し・設計判断は本リポジトリ、実装時の API 確認は kitakkun guide、と役割を分ける
- 両者の記述が食い違う場合は、対象プロジェクトの Kotlin バージョンに近い方を優先する。kitakkun guide は 2.4.x 基準、本リポジトリの `details/` は調査時点のソースに基づく
