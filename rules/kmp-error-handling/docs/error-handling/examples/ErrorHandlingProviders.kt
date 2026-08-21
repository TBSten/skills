// 配置先: App 層 (例: app/di/ErrorHandlingProviders.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

// TODO: DI フレームワークの import を追加する。
//  以下は Metro のアノテーション例。Dagger/Hilt, Koin 等を使う場合は読み替えること
//  (このファイルは DI フレームワークを導入しないとコンパイルできない)。

@ContributesTo(AppScope::class)
interface ErrorHandlingProviders {
    @Provides
    @SingleIn(AppScope::class)
    fun provideHandleErrorDefault(): HandleErrorDefault = HandleErrorDefault()

    @Binds
    val HandleErrorDefault.bindsHandleError: HandleError

    // 複数のハンドラを使う場合は plus 演算子で合成した HandleError を provide する。
    // 例:
    // @Provides
    // @SingleIn(AppScope::class)
    // fun provideHandleError(
    //     default: HandleErrorDefault,
    //     crashlytics: CrashlyticsHandleError,
    // ): HandleError = default + crashlytics

    @Provides
    fun provideHandleWarning(): HandleWarning = object : HandleWarning {
        override fun invoke(vararg warnings: String) {
            // TODO: Debug では println、Release では Crashlytics 等の外部サービスに報告する
        }
    }
}
