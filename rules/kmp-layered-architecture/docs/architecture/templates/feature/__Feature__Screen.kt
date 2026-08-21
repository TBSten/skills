package __package__

// TODO: import をプロジェクトに合わせて追加する
//  (Composable, metroViewModel, MapPreviewParameterProvider, PreviewRoot, Preview 等)

/**
 * __Feature__ 画面。
 *
 * docs/architecture/ui.md の Composable 規約:
 * - public にする唯一の Composable は Screen のみ
 * - 読みやすい単位で Component に分割し component/ ディレクトリに配置する
 */
@Composable
fun __Feature__Screen(
    viewModel: __Feature__ViewModel = metroViewModel(),
) {
    // TODO: viewModel の状態を collect して internal な Screen に渡す
    // val state1 by viewModel.state1.collectAsStateWithLifecycle()

    __Feature__Screen(
        // state1 = state1,
    )
}

@Composable
internal fun __Feature__Screen(
    // TODO: 画面が使う state を引数に追加する
) {
    // TODO: implement UI by states
}

// Previews

private data class __Feature__ScreenParams(
    // TODO: Preview に渡す state を追加する
    val placeholder: Unit = Unit,
)

private class __Feature__ScreenParamsProvider : MapPreviewParameterProvider<__Feature__ScreenParams>(
    "Default" to __Feature__ScreenParams(),
    // TODO: "Loading", "Error" など状態パターンを追加する
)

@Preview
@Composable
private fun __Feature__ScreenPreview(
    @PreviewParameter(__Feature__ScreenParamsProvider::class)
    params: __Feature__ScreenParams,
) = PreviewRoot {
    __Feature__Screen(
        // TODO: params の state を渡す
    )
}
