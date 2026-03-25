# SimpleLoader スキル

[English](./simple-loader.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin/Compose Multiplatform プロジェクト向けの非同期データ読み込み状態管理ステートマシンを生成する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
npx skills add tbsten/skills \
  --skill simple-loader
```

### 2. AI エージェントに依頼:

```
SimpleLoader を追加して、非同期データ読み込みの状態管理を実装して。
```

プロジェクト構造を検出し、設定を確認した上で全ファイルを生成します。

## 特徴

SimpleLoader は、非同期データ読み込みを sealed interface ベースのステートマシンで管理します。初回読み込み (initialLoad) とリフレッシュ (refresh) を明確に区別します。

### 生成ファイル

| ファイル | 説明 | 必須 |
|---|---|---|
| `SimpleLoader.kt` | コア interface、State sealed interface、Impl、FakeSimpleLoader | 必須 |
| `SimpleLoaderFactory.kt` | インスタンス生成用 Factory interface | 必須 |
| `SimpleLoaderExt.kt` | 拡張関数 (`dataOrNull`, `dataOr`, `exceptionOrNull`) | 任意 |
| `SimpleLoaderLogger.kt` | `fun interface` によるプラガブルなロギング | 任意 |
| `IllegalStateTransitionHandler.kt` | 不正遷移時のポリシー (throw/warning/noOp) | 任意 |
| `SimpleLoaderWithPartialData.kt` | ローディング中の部分データ対応 (プログレス等) | 任意 |
| UI ヘルパー (`View.kt`, `AnimatedView.kt` 等) | Compose UI での状態表示とアニメーション遷移 | 任意 |

### 状態遷移図

```
Initial ──initialLoad()──► InitialLoading ──成功──► Loaded
                                │                      │
                                └──失敗──► InitialError  │
                                                        │
                           Loaded ──refresh()──► RefreshLoading ──成功──► Loaded
                                                     │
                                                     └──失敗──► RefreshError
```

## 利用方法

### ViewModel での利用

```kotlin
typealias ItemListLoader = SimpleLoader<List<Item>>

class HomeViewModel(
    private val itemListLoader: ItemListLoader,
) : ViewModel(itemListLoader) {
    val loadItemListState by itemListLoader::state

    init { itemListLoader.initialLoad() }
    fun refresh() = itemListLoader.refresh()
}
```

### Compose UI での状態表示

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.loadItemListState.collectAsStateWithLifecycle()

    state.AnimatedView(
        onInitialErrorRefresh = viewModel::refresh,
        initialLoading = { SkeletonView() },
    ) { state ->
        ItemListView(
            itemList = state.data,
            isRefreshLoading = isRefreshLoading,
        )
    }
}
```

### テストでの利用

```kotlin
val fakeLoader = FakeSimpleLoader<List<String>>(
    coroutineScope = TestScope(),
    state = SimpleLoader.State.Loaded(listOf("item1", "item2")),
)
```

## リポジトリ

このスキルは [TBSten/skills](https://github.com/TBSten/skills) の一部です。

```
skills/simple-loader/
├── SKILL.md              ← `npx skills add` でインストールされる
├── example/              ← テンプレートソースファイル
└── references/           ← 利用パターンドキュメント
```
