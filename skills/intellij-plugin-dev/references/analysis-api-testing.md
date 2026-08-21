# Analysis API を使う機能テスト (正しさの主軸)

`@YourSpec` のような注釈を **Kotlin Analysis API (AA / K2)** で解析する frontend を、
`BasePlatformTestCase` の fixture 上でヘッドレスにテストする。実 UI (Swing/Compose パネル) では
なく **パネルが呼ぶ純ロジック (AA→model→IR→ナビ先)** を分解してアサートするのがコツ。

実ファイル (SSoT): `example/src/test/kotlin/com/example/plugin/AnalysisTestBase.kt` (harness:
`ignoreUnrelatedLoggedErrors` / `runReadActionBlocking`) + `ExampleAnalysisTest.kt` (土台レシピの実装)。
snippet から再構築せず `scripts/scaffold.sh` で example から生成する (SKILL.md)。

## 土台のレシピ

```kotlin
@OptIn(KaAllowAnalysisOnEdt::class)
internal class MySpecAnalysisTest : BasePlatformTestCase() {

    override fun tearDown() = ignoreUnrelatedLoggedErrors { super.tearDown() }  // 下記

    fun testResolvesAnnotation() {
        myFixture.addFileToProject("com/example/spec/Annotations.kt", ANNOTATION_STUB)
        val ktFile = myFixture.configureByText("SampleState.kt", SAMPLE_SRC) as KtFile
        val sampleState = ktFile.declarations.filterIsInstance<KtClass>().first { it.name == "SampleState" }

        runReadActionBlocking {            // テストは EDT。analyze は read action 内で
            allowAnalysisOnEdt {           // EDT 上の analyze を許可 (@OptIn(KaAllowAnalysisOnEdt))
                analyze(sampleState) {        // KaSession
                    val yourSpec = sampleState.symbol.annotations.firstOrNull {
                        it.classId?.asFqNameString() == "com.example.spec.YourSpec"
                    } ?: error("@YourSpec が見つからない")
                    // initial = [X::class] の解決
                    val initialArg = yourSpec.arguments.firstOrNull { it.name.asString() == "initial" }?.expression
                    val names = (initialArg as? KaAnnotationValue.ArrayValue)?.values?.mapNotNull { v ->
                        (v as? KaAnnotationValue.ClassLiteralValue)?.let { (it.type as? KaClassType)?.classId?.asFqNameString() }
                    }.orEmpty()
                    assertTrue(names.any { it.endsWith("Loading") })
                }
            }
        }
    }
}
```

- この形の実ファイルは `ExampleAnalysisTest.kt` (example が SSoT)。`runReadActionBlocking` /
  `ignoreUnrelatedLoggedErrors` は `AnalysisTestBase.kt` に定義してある。
- **K2 強制**: `tasks.test { systemProperty("idea.kotlin.plugin.use.k2", "true") }` + `plugin.xml` の
  `<supportsKotlinPluginMode supportsK2="true"/>` (`setup/basics.md`)。
- **read action + EDT**: テストは EDT なので `runReadActionBlocking { allowAnalysisOnEdt { analyze(...) } }`
  で囲む。プロダクション側の非同期解析は `ReadAction.nonBlocking()` を使う (`ide-integration.md`)。

## ジェネリック注釈の型引数 `@YourGenericAnno<A>` (最大リスク・確定レシピ)

`KaAnnotation` は型引数を直接出さない。**注釈 PSI の型引数を `analyze { typeRef.type }` で解決**すれば
確定的に取れる。

```kotlin
val yourGenericAnno = content.annotationEntries.first { it.shortName?.asString() == "YourGenericAnno" }
val typeArgRef = (yourGenericAnno.typeReference?.typeElement as? KtUserType)
    ?.typeArgumentList?.arguments?.firstOrNull()?.typeReference ?: error("型引数が PSI から取れない")
analyze(content) {
    val classId = (typeArgRef.type as? KaClassType)?.classId?.asFqNameString()  // → "…​.SampleAction.Reload"
}
```

## fixture の注釈スタブ

- plugin モジュールは runtime 注釈に依存を持たないので、**注釈スタブを fixture 内ソースとして同梱**する
  (KSP テストの注釈スタブと同型)。FQN を本物と合わせて `classId` 判定を通す。
- **spike のスタブと parity fixture を区別する**: `ExampleAnalysisTest.kt` (example) の `ANNOTATION_STUB`
  は AA 解決を実証する**最小 spike** で、実 runtime とは差がある (`Unit::class`↔`Nothing::class` /
  `KClass<*>`↔`KClass<out State>` / `@Retention(SOURCE)`・`@Repeatable`・`A : Any` bound の欠落)。
  production frontend の正しさを担保する fixture にはそのままコピーせず、**実 runtime 定義から生成した
  共通スタブ (parity fixture)** を使う。
- **その parity fixture は実 runtime と同期**する: `Nothing` default / `@Retention(SOURCE)` / `Repeatable` /
  generic bounds を再現しないと、実挙動と乖離したテストになる。生成 or fixture 共通定義で SSoT。
- デフォルト light fixture の stdlib で `KClass` は解決でき、**stdlib の明示追加は不要**だった。
- `addFileToProject` は最小限に。多いと shutdown で `Stubs index ... stale file ids` が出やすい
  (marker は解析対象ファイルにインライン)。

## harness の落とし穴

- **無関係な logged error を失敗に昇格させない**: 統合 IDEA (IU) の bundled plugin (Vue LSP 等) の
  VFS listener が tearDown の fixture 削除で初期化失敗 → logged error → テスト失敗になる。
  `tearDown()` を `LoggedErrorProcessor` で包み、既知の無害カテゴリ (`Vue` / `Lsp` /
  `stale file ids`) だけ握り潰す。抑制は**例外型/category/known issue に限定**し、実装由来エラーは
  隠さない。実装 (SSoT): `AnalysisTestBase.ignoreUnrelatedLoggedErrors` (example)。
- **この substring 一致は spike の最小策で、本番テストには広すぎる**: category + message + stacktrace を
  連結した文字列への部分一致なので、新規の実装エラーの message/stack に `Lsp` 等が偶然含まれるだけで
  `emptySet()` になり失敗へ昇格しなくなる。production では **logger category の完全一致 + 例外 class +
  既知 message prefix の組み合わせ**へ絞り、**抑制した件数/内容をテスト出力に残す**。既知パターン以外は
  必ず `super.processError(...)` へ委譲することもテストで固定するとよい。
- **テスト基盤は非 hermetic**: 同一 uncached test が bundled plugin の初期化/index/storage エラーで
  散発的に落ちる。**単発 green では不十分** → `--rerun-tasks --no-build-cache --no-daemon` で
  clean/uncached を連続複数回成功させて確認する。**flaky を再実行で green にするだけを完了扱いに
  しない** (infra failure と assertion failure をレポート上で区別する)。

## AA / PSI の正しさ (壊れたコードへの耐性)

- **AA は編集中の壊れたコードに例外でなく error type を返す**。`mapNotNull` で unresolved を黙って
  捨てると欠落が隠れる → **Resolved / Unresolved / Missing を表現する解析結果型**を導入し、
  `model.degraded` を正直に立てる。degraded/broken source で partial 表示を明示的に assert する。
- **discovery は PSI shortName 依存は脆い**。`@YourSpec\b` 正規表現は初期 scaffold の割り切りに留め、
  最終的に **AA の `classId`/FQN で確認**する (typealias supertype も扱う)。
- **正規化契約を runtime/KSP と揃える**: 空配列/省略→`Stay` のような正規化が frontend と食い違うと
  図がずれる。runtime/KSP/IDE で同じ正規化にし、**同一 matrix の parity test (fixture)** で固定する
  (model 型が別なので厳密な型等値ではなく構造比較)。

## アサート観点 (パネルを組み立てず純ロジックを見る)

1. `initial` 解決 = `[*]` 起点になっているか
2. 遷移 (= 遷移表) が期待通りか
3. reachability (到達不能 state の検出)
4. **ノード→ソース**: ノードの `SmartPsiElementPointer.element` が期待の `KtClass` を指すか
   (= クリック遷移の実体。実 `NavigationUtil` 起動は不要)
5. degraded fallback: 壊れた `.kt` で落ちずに素 PSI simpleName / エラーパネル state になるか
