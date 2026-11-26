// フィードバックを入れないビルドのソースに置く no-op（Android: androidApp/src/release、ほか: …FeedbackOff/kotlin）。呼ぶ側をそろえるためだけ
// package は on のファイルと同じにする（Android は呼ぶ Activity と同じ package）
internal fun installFeedback() = Unit
