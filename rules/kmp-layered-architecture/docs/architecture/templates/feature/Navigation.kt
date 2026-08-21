package __package__

/**
 * __Feature__ 画面のナビゲーション interface。
 *
 * docs/architecture/ui.md の Navigation 規約:
 * - plain interface としてここに定義し、ui/navigation の AppNavigator で統合実装する
 * - 画面のインプットと他画面へのアウトプット (画面遷移など) をここで定義する
 */
interface __Feature__Navigator {
    fun onBack()
    // TODO: この画面から遷移する先を追加する (例: fun toDetail(id: ItemId))
}
