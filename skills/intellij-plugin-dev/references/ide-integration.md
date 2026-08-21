# IDE への組み込み (tool window / Compose ホスト / エディタ追従 / ナビ / lifecycle / 性能)

プラグインを IntelliJ Platform に正しく繋ぐための API とハマりどころ。IntelliJ Platform プラグインの実装から。

## 目次

1. tool window と Compose (Jewel) のホスト
2. plugin.xml 登録
3. エディタ追従 (選択・編集の反映)
4. ナビゲーション (ノード/行 → ソース)
5. PSI テキスト挿入 (コード生成) と threading
6. controller の lifecycle (stale race / invalidation / dumb mode)
7. 性能・スレッド (重い計算・cancellation・runCatching の罠)
8. live 更新の stale 固着バグ

## 1. tool window と Compose (Jewel) のホスト

- `ToolWindowFactory` + `DumbAware` を実装し、`plugin.xml` に `<toolWindow anchor="right" ...>` で登録。
- Compose UI のホストは **`ToolWindow.addComposeTab("…") { … }`** (内部で `JewelComposePanel` +
  `enableNewSwingCompositing`)。この中の Composable を preview と共有する (`headless-preview.md`)。
- 実ファイル: `example/src/main/kotlin/com/example/plugin/ExampleToolWindowFactory.kt` + 共有
  Composable `example/src/shared/kotlin/com/example/plugin/ui/ExampleToolWindowContent.kt`。

## 2. plugin.xml 登録

完成形の実ファイル (SSoT): `example/src/main/resources/META-INF/plugin.xml`。要点:

- `<depends>com.intellij.modules.platform</depends>` + `<depends>org.jetbrains.kotlin</depends>`
  (Analysis API 同梱。将来 optional + config-file に分離可)。
- `<dependencies><module name="..."/></dependencies>` ×6 — `bundledModule(...)` と対を成す v2
  module dependency。plugin classloader から解決させる (`setup/basics.md`)。
- `<supportsKotlinPluginMode supportsK2="true"/>` (defaultExtensionNs="org.jetbrains.kotlin")。
- `<toolWindow id="…" anchor="right" factoryClass="…ToolWindowFactory"/>`。
- gutter アイコンは `<codeInsight.lineMarkerProvider language="kotlin"
  implementationClass="…GutterLineMarkerProvider"/>` — example ではコメントアウトの雛形
  (実装を足したら有効化する)。

- **gutter line marker**: `LineMarkerProvider.collectSlowLineMarkers` で PSI を歩き、**リーフ要素**
  (`KtClass.nameIdentifier` / 注釈の `calleeExpression...referencedNameElement`) を鍵にマーカーを付ける。
  候補 0→無し / 1→即ジャンプ (`navigate(true)`) / 複数→`JBPopupFactory` のポップアップ選択。
  `CachedValuesManager` で KtFile 単位キャッシュ。smart pointer 解決は read action・`navigate` は EDT。

## 3. エディタ追従 (選択・編集の反映)

- 選択追従は message bus の **`FileEditorManagerListener#selectionChanged`**。
- 編集追従は **`MergingUpdateQueue` / `Alarm` で ~200ms デバウンス** + `(VirtualFile, modStamp)` キャッシュ。
- **invalidation を広く取る**: selected file の変更 (url/modStamp) だけを見ると、外部 typealias / 参照型 /
  library root / index 変更で更新されない。**dependency / project modification tracker** を invalidation に
  含める。手動 Reload に正しさを依存させない。

## 4. ナビゲーション (ノード/行 → ソース)

- **PSI 非依存の marker (`SourceAnchor`)** をドメイン model に持たせ、PSI 実装 (`PsiSourceAnchor`) が
  `SmartPsiElementPointer<out KtElement>` で解決する。`KtElement : NavigatablePsiElement` なので、State
  (`KtClassOrObject`) も遷移 (注釈適用箇所 `KtAnnotationEntry`) も **1 型で** `.name` / `.canNavigate()` /
  `.navigate()` を扱える。遷移矢印は「宣言」を持たないので注釈サイトへ飛ばす。
- Compose canvas 内のクリック当たり判定は headless で確認しづらい → hit-test は純ロジックに切り出して
  JUnit で担保 + 実 IDE で対話確認 (`driver-smoke.md`)。tap 座標は `offset / renderZoom` で pre-scale の
  px 空間へ戻して model 矩形と突き合わせる。Compose-in-plugin の修飾キーは
  `LocalWindowInfo.keyboardModifiers.isShiftPressed`。

## 5. PSI テキスト挿入 (コード生成) と threading

- **純粋生成 (shared) と副作用挿入 (main) を分離**する。生成する文字列/要素は PSI 非依存の純関数にして
  `src/test` でフル単体テスト、実 PSI 挿入だけ main に置く。
- 挿入は**単一 `WriteCommandAction` (1 undo)**: `KtPsiFactory` で要素生成 → `ShortenReferences` /
  `ImportInsertHelper` で参照整理 → `reformat` → `navigate(true)`。同名は連番 dedup。
- **threading 例外**: PSI の read (`uniqueName` 取得等) を WriteCommandAction 外 (EDT 素実行) でやると
  `Read access is allowed from inside read-action only`。**PSI の read/write は全て WriteCommandAction 内**
  へ、挿入後の `canNavigate()` も `ReadAction` で囲む。
- `@Retention(SOURCE)` の注釈は binary output に残らず source 処理時だけ見える。**適用箇所が root と
  同一 source set / module で同時にコンパイルされていれば足りる (同じ `.kt` である必要はない)**。実際
  `SpecInserter` は annotation の**適用**を元の state file に置きつつ、冗長な `@YourSpec` 宣言を
  sibling `<StateFile>.flows.kt` に分割している (注釈の "適用" は宣言サイトに置く必要があるが、宣言自体は
  別ファイルでよい)。

## 6. controller の lifecycle

- **stale race**: 解析は debounce 後 refresh でしか世代更新されず、旧解析の完了で旧結果が一時
  適用され旧ファイルへ誤遷移しうる。**schedule 時点で現世代を無効化**し、**選択 file identity を apply
  条件に含める**。debounce 中は loading 表示か旧結果を操作不能にする。
- **dumb mode / tool window lifecycle**: `enteredDumbMode` で世代+cache 無効化・in-flight 破棄、
  `exitDumbMode` で再解析。tool window を隠しても disposable 破棄まで解析が続くので、listener は
  **「visible な時だけ active な child Disposable」** に紐付ける。

## 7. 性能・スレッド

- **重い計算を `remember` 内で同期実行しない**: `YourLowering` / `YourLayout` を Compose の
  `remember` で回すと UI thread を数百 ms 占有する。**cancellable な background job 化** + generation /
  model identity で stale 破棄 + タブが必要になるまで遅延。
- **cancellation checkpoint**: pure な reachability ループ等に checkpoint が無いと、nonBlocking
  read action が cancel されても typing / write action へ明け渡せない。可能なら pure 計算を Analysis
  session / read action の外へ出す。非同期解析は
  `ReadAction.nonBlocking().coalesceBy(this).expireWith(...).finishOnUiThread(...)` + generation トークン。
- **`runCatching` の罠**: Kotlin の `runCatching` は `Throwable` を捕捉するので `OOM` /
  `StackOverflow` / **`ProcessCanceledException`** まで畳み込んでしまう。回復可能な `Exception` のみ
  捕捉し、cancellation / `ProcessCanceledException` / `VirtualMachineError` は **再送出**する。tree walk は
  iterative か depth/node 上限付きに。
- **表は `LazyColumn`**: `Column + verticalScroll + forEach` は全 row を eager compose する。
  可視 row だけ compose する。

## 8. live 更新の stale 固着バグ

- content state を **state 名で `remember`** すると、同名の再解析で stores/model が **stale に固着**する
  (`initial` を編集して Reload しても図が変わらない)。`updateStores` で最新 model に差し替える
  (selection / zoom / focus は保持、構造的に等価なら no-op)。
