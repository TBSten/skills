// FeedbackShot.swift — スクリーンショット（O1）と、OSLog を読む補助（Debug だけ）。
#if DEBUG
import OSLog
import UIKit

enum FeedbackShot {
    struct Result { var window: String?; var element: String?; var error: String? } // PNG の base64

    /// アプリの window だけを描く（オーバーレイは別の window なので写らない）。
    /// 全体に赤枠・周り 24pt の切り抜きの 2 枚。倍率は min(scale, 2)
    @MainActor
    static func capture(window: UIWindow, rect: CGRect) -> Result {
        let bounds = window.bounds
        guard bounds.width > 0, bounds.height > 0 else { return Result(error: "window の大きさが 0") }
        let format = UIGraphicsImageRendererFormat()
        format.scale = min(window.traitCollection.displayScale, 2)
        format.opaque = true
        var drew = false
        let full = UIGraphicsImageRenderer(bounds: bounds, format: format).image { _ in
            drew = window.drawHierarchy(in: bounds, afterScreenUpdates: true)
            UIColor.red.setStroke()
            let frame = UIBezierPath(rect: rect.insetBy(dx: -1.5, dy: -1.5))
            frame.lineWidth = 3
            frame.stroke()
        }
        guard drew else { return Result(error: "drawHierarchy が描けませんでした") }
        let crop = rect.insetBy(dx: -24, dy: -24).intersection(bounds)
        let element = crop.isNull || crop.isEmpty ? nil : UIGraphicsImageRenderer(size: crop.size, format: format).image { _ in
            full.draw(at: CGPoint(x: -crop.minX, y: -crop.minY))
        }
        return Result(window: full.pngData()?.base64EncodedString(), element: element?.pngData()?.base64EncodedString(),
                      error: element == nil ? "切り抜きの範囲が画面の外です" : nil)
    }
}

extension FeedbackMode {
    /// 既存のログが os.Logger / os_log なら、これを config.getRecentLogs に渡す（このプロセスの直近のログ。古い順）
    nonisolated static func osLogEntries(subsystem: String? = Bundle.main.bundleIdentifier, seconds: TimeInterval = 300, limit: Int = 200) -> [FeedbackLog] {
        guard let store = try? OSLogStore(scope: .currentProcessIdentifier),
              let entries = try? store.getEntries(at: store.position(date: Date().addingTimeInterval(-seconds)))
        else { return [] }
        let logs = entries.compactMap { $0 as? OSLogEntryLog }
            .filter { subsystem == nil || $0.subsystem == subsystem }
            .map { e -> FeedbackLog in
                let level: String = switch e.level {
                case .debug: "debug"
                case .error, .fault: "error" // os.Logger の .warning も error として記録される
                default: "info"
                }
                return FeedbackLog(ts: e.date, level: level, text: e.composedMessage)
            }
        return Array(logs.suffix(limit))
    }
}
#endif
