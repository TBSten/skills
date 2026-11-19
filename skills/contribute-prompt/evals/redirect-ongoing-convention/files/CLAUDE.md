# piyo-app (Kotlin / Android)

## ViewModel の規約

- 画面状態は `sealed interface XxxUiState` (`Loading` / `Success` / `Error`) で表し、`StateFlow<XxxUiState>` で公開する
- 一回限りのイベント (遷移・トースト) は `Channel<XxxEvent>` + `receiveAsFlow()` で流し、UiState に入れない
- ViewModel から `Context` を参照しない
- 新しい ViewModel を書くたび、既存 ViewModel をレビューするたびにこの規約を守る
