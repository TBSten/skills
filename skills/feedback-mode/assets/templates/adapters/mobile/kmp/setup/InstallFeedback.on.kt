// フィードバックを入れるビルドのソース（下のどれか）に置く。FeedbackSetup.kt も同じ場所に置く
//   Android: androidApp/src/debug/kotlin/…        （backHandler に BackHandler を渡す。下のコメント）
//   iOS:     sharedUI/src/iosFeedbackOn/kotlin/   （Xcode の Debug のときだけ使われる）
//   Desktop: desktopApp/src/feedbackOn/kotlin/   （-Pfeedback=true のときだけ）
//   Web:     webApp/src/wasmFeedbackOn/kotlin/   （-Pfeedback=true のときだけ）
// package: iOS・Desktop・Web は呼ぶ側の main.kt と同じルート package のままでよい。
// Android は呼ぶ Activity と同じ package を先頭に書く（Kotlin は package 付きのファイルからルート package の宣言を参照できない）
// 起動時（setContent / ComposeUIViewController / application / ComposeViewport の前）に installFeedback() を 1 回呼ぶ
import com.example.app.debughooks.setupFeedback
import com.example.feedback.platformFeedbackConfig

internal fun installFeedback() = setupFeedback(
    platformFeedbackConfig(
        appName = "MyApp",
        // 送信先: Android は BuildConfig.FEEDBACK_ENDPOINT（debug の buildConfigField）、Desktop は System.getenv("FEEDBACK_ENDPOINT")、
        // iOS シミュレータ・Web は "http://127.0.0.1:7373/__feedback"（開発機の受け口）
        endpoint = TODO("送信先"),
        getScreen = { "" },
    ),
    // Android だけ: backHandler = { enabled, onBack -> androidx.activity.compose.BackHandler(enabled, onBack) },
)
