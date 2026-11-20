// フィードバックを入れないビルドのソースに置く no-op（Android: androidApp/src/release、ほか: …FeedbackOff/kotlin）。呼ぶ側をそろえるためだけ
internal fun installFeedback() = Unit
