# android-error-handling

Centralized error management for Android using Coroutines and Hilt.

## Features
- **launchSafe Extension**: Automatically catch exceptions in Coroutines and notify a state holder.
- **ErrorStateHolder**: Manage error UI states (like dialogs) at ViewModel or Application scope.
- **Standardized Exceptions**: Define common exception types like `AppException` for consistent error handling across the app.

## Prerequisites
- Android project using Kotlin Coroutines.
- Dependency Injection (Hilt is recommended for providing `ErrorStateHolder`).

## How to use
1. Run `npx skills add tbsten/skills --skill android-error-handling`.
2. Move files from `example/error/` to your project's error handling package.
3. Inject `ErrorStateHolder` and use `launchSafe` in your ViewModels.
