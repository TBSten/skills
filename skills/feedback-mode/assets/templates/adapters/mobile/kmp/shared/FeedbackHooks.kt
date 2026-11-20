package com.example.app.debughooks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 画面のある共有モジュール（sharedUI など）の commonMain に置く差し込み口だけのファイル。
// フィードバックのコード・URL は持たない。debug のときだけ :feedback 側の setupFeedback() が中身を入れる。
// release ではこのまま（fbTarget は何もしない、FeedbackWrap は content を出すだけ）
object FeedbackHooks {
    var target: (Modifier, String) -> Modifier = { modifier, _ -> modifier }
    var wrap: @Composable (content: @Composable () -> Unit) -> Unit = { content -> content() }
}

// 部品の名前（R5）。"画面/部品" の道筋をそのまま書く
fun Modifier.fbTarget(name: String): Modifier = FeedbackHooks.target(this, name)

// アプリの根を包む（App() の一番外側）
@Composable
fun FeedbackWrap(content: @Composable () -> Unit) = FeedbackHooks.wrap(content)
