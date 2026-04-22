# android-error-handling

CoroutineExceptionHandler と StateFlow を活用した Android 向け共通エラーハンドリング基盤。

## 特徴
- **launchSafe 拡張**: Coroutine 内の例外を自動的にキャッチし、StateHolder へ通知します。
- **ErrorStateHolder**: エラー UI（ダイアログ等）の状態を ViewModel またはアプリ全体で管理します。
- **標準化された例外**: `AppException` を通じて、API エラーなどの共通的な例外を統一的に扱えます。

## 前提条件
- Kotlin Coroutines を使用した Android プロジェクト。
- 依存関係注入（Hilt 等で `ErrorStateHolder` を提供することを推奨）。

## 使い方
1. `npx skills add tbsten/skills --skill android-error-handling` を実行します。
2. `example/error/` 内のファイルをプロジェクトのエラー処理パッケージに移動します。
3. ViewModel で `ErrorStateHolder` を利用し、`launchSafe` を通じて処理を実行します。
