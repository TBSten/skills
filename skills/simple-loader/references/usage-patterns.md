# SimpleLoader Usage Patterns

## Pattern 1: typealias + Factory + DI

ドメイン固有の Loader 型を typealias で定義し、Factory 経由でインスタンスを生成、DI でバインドする。

```kotlin
// Domain layer: typealias で Loader の型を定義
typealias ItemListLoader = SimpleLoader<List<Item>>

// Data layer: Factory で実装を生成し DI バインド
@Inject
@ContributesBinding(AppScope::class)
class ItemListLoaderImpl(
    getItemListUseCase: GetItemListUseCase,
    simpleLoaderFactory: SimpleLoaderFactory,
) : ItemListLoader by simpleLoaderFactory.create(
    coroutineScope = MainScope(),
    load = { getItemListUseCase() },
)
```

**ポイント**:
- `by` デリゲートで SimpleLoader interface を自動実装
- `load` ラムダ内は `SimpleLoader.LoadScope` (= CoroutineScope) がレシーバ
- `MainScope()` を渡して ViewModel のライフサイクルに合わせる

## Pattern 2: ViewModel での利用

```kotlin
class HomeViewModel(
    private val itemListLoader: ItemListLoader,
    private val handleError: HandleError,
) : ViewModel(itemListLoader) {  // AutoCloseable として渡してライフサイクル管理
    // StateFlow をプロパティデリゲートで公開
    val loadItemListState by itemListLoader::state

    init {
        itemListLoader.initialLoad()  // 画面表示時に初回読み込み
    }

    fun reloadInitial() = handleError {
        itemListLoader.initialLoad()
    }

    fun refresh() = handleError {
        itemListLoader.refresh()
    }
}
```

**ポイント**:
- `ViewModel(itemListLoader)` で ViewModel の onCleared 時に `close()` が呼ばれる
- `by itemListLoader::state` でプロパティデリゲートとして公開
- `initialLoad()` と `refresh()` を明示的に使い分ける
- `load()` を使うと現在の状態に応じて自動選択される

## Pattern 3: Compose UI での状態表示

### AnimatedView (推奨)

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val loadItemListState by viewModel.loadItemListState.collectAsStateWithLifecycle()

    loadItemListState.AnimatedView(
        onInitialErrorRefresh = viewModel::refresh,
        initialLoading = { SkeletonView() },  // カスタムローディング表示
    ) { state ->
        // この scope 内では ViewWithDataScope のプロパティが使える
        ItemListView(
            itemList = state.data,
            isRefreshLoading = isRefreshLoading,  // ViewWithDataScope.isRefreshLoading
            modifier = Modifier.thenIf(isRefreshLoading) { alpha(0.5f) },
        )
    }
}
```

### View (アニメーションなし)

```kotlin
loadItemListState.View(
    initialError = { /* InitialError 時のカスタム表示 */ },
    initialLoading = { /* InitialLoading 時のカスタム表示 */ },
) { state ->
    ItemListView(itemList = state.data)
}
```

**ポイント**:
- `AnimatedView` は状態間の遷移アニメーション付き（fadeIn/fadeOut）
- `withData` ブロックのレシーバは `ViewWithDataScope` で `isRefreshLoading` / `isRefreshError` にアクセス可能
- `initialLoading` / `initialError` のデフォルト実装はカスタマイズ可能

## Pattern 4: WithPartialData (プログレス表示)

ローディング中に部分データ（プログレスなど）を更新する。

```kotlin
val (loader, progressFlow) = withPartialData(
    initialPartialData = 0.0,
) {
    simpleLoaderFactory.create(
        coroutineScope = MainScope(),
        load = {
            withAutoProgress {
                // 実際のデータ取得処理
                repository.fetchData()
            }
        },
    )
}
```

**ポイント**:
- `withPartialData` は `Pair<Loader, StateFlow<PartialData>>` を返す
- `emitPartialData` で任意のタイミングで部分データを更新可能
- `withAutoProgress` は自動プログレスバーのユーティリティ
- Loading 状態に遷移すると partialData は initialPartialData にリセットされる

## Pattern 5: テストでの利用

```kotlin
// FakeSimpleLoader で ViewModel をテスト
val fakeLoader = FakeSimpleLoader<List<String>>(
    coroutineScope = TestScope(),
    state = SimpleLoader.State.Loaded(listOf("item1", "item2")),
)
val viewModel = HomeViewModel(itemListLoader = fakeLoader)

// 状態を直接指定してテスト
val errorLoader = FakeSimpleLoader<List<String>>(
    coroutineScope = TestScope(),
    state = SimpleLoader.State.InitialError(Exception("Network error")),
)
```

## Pattern 6: Preview での利用

```kotlin
@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        loadItemListState = SimpleLoader.State.Loaded(
            data = listOf("item-1", "item-2"),
        ),
        onRefresh = {},
    )
}

// 各状態の Preview
@Preview @Composable fun LoadingPreview() {
    HomeScreen(loadItemListState = SimpleLoader.State.InitialLoading, ...)
}

@Preview @Composable fun ErrorPreview() {
    HomeScreen(loadItemListState = SimpleLoader.State.InitialError(Exception()), ...)
}
```
