package __package__

// TODO: import をプロジェクトに合わせて追加する
//  (ViewModel, DI アノテーション, StateHolder 等)

/**
 * __Feature__ 画面の ViewModel。
 *
 * docs/architecture/ui.md の ViewModel, StateHolder 規約:
 * - 1 Screen : 1 ViewModel で画面ごとの状態管理を行う
 * - 状態管理はデータの種類ごとに StateHolder (SimpleLoader / InfiniteLoader /
 *   EventHolder / HandleError, HandleWarning / Navigator) にカプセル化する
 */
@Inject
@ViewModelKey(__Feature__ViewModel::class)
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class __Feature__ViewModel(
    // TODO: StateHolder を inject する (例: __feature__Loader: __Feature__Loader)
) : ViewModel() {
    init {
        // TODO: 必要なら初回読み込みを開始する (例: __feature__Loader.initialLoad())
    }

    // TODO: StateHolder の state を公開する (例: val __feature__ = __feature__Loader.state)
}
