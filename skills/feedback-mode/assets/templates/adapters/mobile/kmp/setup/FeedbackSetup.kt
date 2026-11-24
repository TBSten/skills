package com.example.app.debughooks

import androidx.compose.runtime.Composable
import com.example.feedback.FeedbackConfig
import com.example.feedback.FeedbackController
import com.example.feedback.FeedbackRoot
import com.example.feedback.fbTarget as feedbackTarget

// debug のビルドにだけ入るソース（Android: androidApp/src/debug、iOS / Desktop / Web: feedback スイッチが on のときだけ足すディレクトリ）に置く。
// 起動時（setContent / ComposeUIViewController / application / ComposeViewport の前）に 1 回呼ぶ
fun setupFeedback(
    config: FeedbackConfig,
    // 既存の debug 画面から入るなら、同じ controller を渡して enter() を呼ぶ（config.showEntryButton = false）
    controller: FeedbackController = FeedbackController(),
    backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> },
) {
    FeedbackHooks.target = { modifier, name -> modifier.feedbackTarget(name) }
    FeedbackHooks.wrap = { content -> FeedbackRoot(config, controller = controller, backHandler = backHandler, content = content) }
}
