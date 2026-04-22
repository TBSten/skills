---
name: android-error-handling
description: >
  Set up centralized error management for Android using Coroutines and Hilt.
  It includes safe coroutine launch utilities, state holders for error UI,
  and standardized exception types.
  Use when requested: "Setup error handling", "Safe coroutine launch in ViewModel", "Manage error dialogs globally".
---

# android-error-handling

Android プロジェクトにおいて、CoroutineExceptionHandler と StateFlow を組み合わせてエラー処理を共通化する。

## 特徴
1. **launchSafe**: 例外を自動的にキャッチし、StateHolder に通知する Coroutine 拡張。
2. **ErrorStateHolder**: エラー状態（ダイアログ表示など）を管理する。ViewModel 単位、またはアプリ全体（Singleton）で利用可能。
3. **統一された例外型**: API エラーなどの共通的な例外を `AppException` として定義。

## 手順
1. `example/error/` 内のファイルをプロジェクトの適切なパッケージ（`domain.error` など）に配置する。
2. `ErrorStateHolder` を ViewModel または Singleton に DI (Hilt 等) する。
3. ViewModel で `LaunchSafe(errorStateHolder)` を使用して `launchSafe { ... }` を実行する。
4. UI 側で `errorState` を収集し、エラーダイアログ等を表示する。

## Resources
- `example/error/`: エラー処理に必要なクラス群。
