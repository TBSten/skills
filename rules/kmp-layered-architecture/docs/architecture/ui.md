# Layer2. UI

- Dependencies: Domain -> UI -> App
- モジュール構成:
    - ui
        - core ... 共通コンポーネント・Theme など各画面共通で使うもの
        - feature:{FeatureName} ... 各機能ごとに Screen/ViewModel, StateHolder/Navigation を格納する
        - navigation ... 各画面を連携させて UI の EntryPoint となる AppNavigation を App Layer に提供する
- Compose を使って UI を表示する。
- ViewModel および StateHolder で Domain の UseCase, Model を使用して UI の状態管理を行う。
- Navigation で画面のインプットと他画面へのアウトプット（画面遷移など）の interface を定義する。

## Composable

- public にする唯一の Composable として Screen を定義する。
- 読みやすい単位で Component に分割する。コンポーネントに分割した際は component/ ディレクトリにコンポーネントのファイルを配置する。
- 強制ではないが、Screen > (Section/TopBar/BottomBar) > Screen specified component > Util component という構成でコンポーネント階層を作ると良い。

```kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = metroViewModel(),
) {
    val state1 by viewModel.state1.collectAsStateWithLifecycle()
    // ...

    HomeScreen(
        state1 = state1,
        // ...
    )
}

@Composable
internal fun HomeScreen(
    state1: HomeState1,
    // ...
) {
    // TODO implement UI by states
}

// Previews

private data class HomeScreenParams(
    val state1: HomeState1,
    // ...
)

private class HomeScreenParamsProvider : MapPreviewParameterProvider<HomeScreenParams>(
    "Loaded" to HomeScreenParams(
        state1 = /* TODO */,
        // ...
    ),
    "Loading" to HomeScreenParams(
        state1 = /* TODO */,
        // ...
    ),
)

@Preview
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeScreenParamsProvider::class)
    params: HomeScreenParams,
) = PreviewRoot {
    HomeScreen(
        state1 = params.state1,
        // ...
    )
}
```

## ViewModel, StateHolder

- ViewModel に画面で使用するデータの状態と変更方法を提供する。UI 状態管理のエントリーポイントとなる。
- 画面で使用するデータの状態とその変更方法・状態管理ロジックを、データの種類ごとに StateHolder にカプセル化する。
- 1 Screen : 1 ViewModel で画面ごとの状態管理を行う。
- 1 ViewModel : 0 以上の StateHolder で画面に必要な状態管理。
- 1 StateHolder : 0 以上の Domain UseCase を Inject しデータの状態管理をする。
- StateHolder は事前に用意されたテンプレートが多くあり、それらを利用することで状態管理を簡略化できる。
    - SimpleLoader ... 読み込み中 -> 読み込み完了/Error -> リフレッシュ と遷移する非同期読み込みの状態管理。
    - InfiniteLoader ... 追加読み込み・無限スクロールが可能な非同期読み込みの状態管理。
    - EventHolder ... ViewModel -> UI にイベントを伝える必要がある際の状態管理。
    - HandleError, HandleWarning ... エラーハンドリング・不正な状態の検出時のハンドリング。
    - Navigator ... 画面遷移を簡素化するための StateHolder。

```kt
@Inject
@ViewModelKey(AppConfigViewModel::class)
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class AppConfigViewModel(
    appConfigLoader: AppConfigLoader, // StateHolder
) : ViewModel(appConfigLoader) {
    init {
        appConfigLoader.initialLoad()
    }

    val appConfig = appConfigLoader.state
}

// AppConfigLoader.kt
typealias AppConfigLoader = SimpleLoader<MergedAppConfig>

@Inject
@ContributesBinding(AppScope::class)
open class AppConfigLoaderImpl(
    simpleLoaderFactory: SimpleLoaderFactory,
    getAppConfig: GetAppConfig,
) : AppConfigLoader by simpleLoaderFactory.create(
    coroutineScope = MainScope(),
    load = { getAppConfig() },
)
```

## Navigation

- 各画面を AppNavigation として 1 つに統合する。
- 各画面の Navigator を plain interface として定義し、AppNavigator で統合実装する。

### 各画面での定義 (Navigation.kt)

各 feature モジュールの Navigation.kt に Navigator の plain interface を定義する。

```kt
// ui/feature/home/Navigation.kt
interface HomeNavigator {
    fun onBack()
    fun toAppConfig()
}
```

### Screen 定義 (Screen.kt)

ui/navigation モジュールの Screen.kt に Screen の sealed interface を集約する。

```kt
@Serializable
sealed interface Screen

@Serializable
data object Home : Screen

@Serializable
data object AppConfig : Screen
```

### AppNavigator (AppNavigator.kt)

各画面の Navigator を統合実装する。

```kt
interface AppNavigator :
    HomeNavigator,
    AppConfigNavigator {
    val backstack: List<Screen>
}

class AppNavigatorImpl : AppNavigator {
    override val backstack = mutableStateListOf<Screen>(Home)

    override fun onBack() { if (backstack.size >= 2) backstack.removeLastOrNull() }
    override fun toAppConfig() { backstack.add(AppConfig) }
    // ...
}
```

### AppNavigation (AppNavigation.kt)

Screen と Composable の紐付けを実装する。

```kt
@Composable
fun AppNavigation(appNavigator: AppNavigator) {
    NavDisplay(
        backStack = appNavigator.backstack,
        entryProvider = appEntryProvider(),
    )
}

private fun appEntryProvider(): (Screen) -> NavEntry<Screen> =
    entryProvider {
        entry<Home> {
            HomeScreen()
        }
        entry<AppConfig> {
            AppConfigScreen()
        }
    }
```

### DI 設定

```kt
@ContributesTo(AppScope::class)
interface AppNavigatorProviders {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppNavigator(): AppNavigator = AppNavigatorImpl()

    @Binds
    val AppNavigator.binds: HomeNavigator
}
```
