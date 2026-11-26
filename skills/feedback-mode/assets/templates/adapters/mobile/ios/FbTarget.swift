// FbTarget.swift — フィードバックモードの「名前」と「入口ボタン」の modifier。
// Debug / Release の両方でビルドする（FeedbackMode.swift などと同じ target に入れる）。
// Release は Self をそのまま返す。`some View` を返すと、型の名前（fbTarget）が Release のシンボルに残るため。
import SwiftUI

extension View {
    #if DEBUG
    /// 部品に名前を付ける（Web の data-fb 相当）。入れ子にすると道筋が「親/子」になる。
    /// 名前は「画面/部品」の形で、行・項目は番号でなく内容で付ける。
    func fbTarget(_ name: String) -> some View { modifier(FbTargetModifier(name: name)) }

    /// 既存の debug 画面が無いときだけ使う、画面隅の入口ボタン。既存の debug 画面があればそこから FeedbackMode.shared.enter() を呼ぶ。
    func feedbackEntryButton() -> some View { overlay(alignment: .bottomTrailing) { FeedbackEntryButton() } }
    #else
    @inline(__always) func fbTarget(_ name: String) -> Self { self }
    @inline(__always) func feedbackEntryButton() -> Self { self }
    #endif
}
