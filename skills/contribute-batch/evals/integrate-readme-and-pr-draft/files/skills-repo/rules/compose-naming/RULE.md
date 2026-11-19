# Composable の命名・引数規約

- 画面ルートは `XxxScreen` と命名する
- 省略可能引数の先頭に `modifier: Modifier = Modifier` を置く
- `ViewModel` は `XxxScreen` だけが受け取り、部品には state と lambda を渡す
