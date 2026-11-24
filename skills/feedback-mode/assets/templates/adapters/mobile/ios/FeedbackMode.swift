// FeedbackMode.swift — SwiftUI 用フィードバックモード本体（Debug だけ）。
// 入る → 部品をタップ → コメント → 送る（開発機の受け口 core/feedback-dev-server.mjs に JSON を POST）。
// 送信 body の形: references/spec/data.md §4 / assets/templates/data/feedback.schema.json。
// ファイル全体が #if DEBUG。Release では FbTarget.swift の modifier が self を返すだけになる。
#if DEBUG
import SwiftUI
import UIKit

/// ログ 1 件（古い順に並べて渡す）
struct FeedbackLog {
    var ts: Date
    var level: String // debug / info / warn / error
    var text: String
}

@MainActor
final class FeedbackMode: ObservableObject {
    static let shared = FeedbackMode()

    /// 差し込み口。アプリの起動時に FeedbackMode.shared.config.xxx = … で埋める
    struct Config {
        var appName = Bundle.main.object(forInfoDictionaryKey: "CFBundleName") as? String ?? "app"
        /// 記録の冒頭に書く Agent への案内 1 行（ソースの場所・ログの形）
        var pointer = ""
        /// 送信先。既定は環境変数 FEEDBACK_ENDPOINT → Info.plist の FeedbackEndpoint（どちらも Debug だけに置く）。URL の既定値は持たない
        var endpoint: URL? = FeedbackMode.setting("FEEDBACK_ENDPOINT", plistKey: "FeedbackEndpoint").flatMap(URL.init(string:))
        /// 受け口の FEEDBACK_TOKEN（LAN に出すときだけ）
        var token: String? = FeedbackMode.setting("FEEDBACK_TOKEN", plistKey: "FeedbackToken")
        var getScreen: @MainActor () -> String = { "" }
        /// 画面の状態（モデルから作る。表示の文字・パスワードは入れない）。JSON にできる値だけ
        var getView: @MainActor () -> [String: Any] = { [:] }
        /// 部品の道筋 → その部品の状態（押せるか・値の字数など）
        var stateOf: @MainActor (String) -> [String: Any] = { _ in [:] }
        /// 既存のログ基盤を読むだけ（例: FeedbackMode.osLogEntries()）
        var getRecentLogs: @MainActor () -> [FeedbackLog] = { [] }
        /// 記録に入れる節（受け口の既定と同じ）
        var options: [String: Bool] = ["target": true, "log": true, "shots": true, "env": false, "screen": false, "view": false]
    }

    struct Selection {
        var candidates: [FbRegistry.Item] // タップ位置を含む部品。小さい順（= 内側から）
        var index = 0
        var click: CGPoint
        var current: FbRegistry.Item { candidates[index] }
        var hasParent: Bool { index + 1 < candidates.count }
    }

    var config = Config()
    @Published private(set) var active = false
    @Published private(set) var selection: Selection?
    @Published var comment = ""
    @Published private(set) var status = ""
    @Published private(set) var sending = false

    private var overlayWindow: UIWindow?
    private weak var appWindow: UIWindow?
    private weak var focusedBeforeEnter: UIResponder?
    private let openedAt = Date()

    /// 入口。既存の debug 画面の項目・シェイク・入口ボタンから呼ぶ
    func enter() {
        guard !active else { return }
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        guard let scene = scenes.first(where: { $0.activationState == .foregroundActive }) ?? scenes.first,
              let win = scene.windows.first(where: { $0.isKeyWindow && !($0 is FeedbackOverlayWindow) }) ?? scene.windows.first
        else { return }
        appWindow = win
        focusedBeforeEnter = Self.firstResponder(in: win)
        win.endEditing(true) // 入力中の欄を外す。出たときに focusedBeforeEnter へ戻す
        let host = UIHostingController(rootView: FeedbackOverlayView(mode: self))
        host.view.backgroundColor = .clear
        let overlay = FeedbackOverlayWindow(windowScene: scene)
        overlay.windowLevel = .alert + 1 // アプリの window より上。タップはここで止まり、アプリに届かない
        overlay.rootViewController = host
        overlay.makeKeyAndVisible()
        overlayWindow = overlay
        selection = nil; comment = ""; status = ""; active = true
    }

    func exit() {
        overlayWindow?.isHidden = true
        overlayWindow = nil
        appWindow?.makeKey()
        focusedBeforeEnter?.becomeFirstResponder(); focusedBeforeEnter = nil
        selection = nil; comment = ""; status = ""; sending = false; active = false
    }

    private static func firstResponder(in v: UIView) -> UIResponder? {
        if v.isFirstResponder { return v }
        for sub in v.subviews { if let r = firstResponder(in: sub) { return r } }
        return nil
    }

    /// タップ位置（window 座標）を含む最小の部品を選ぶ
    func pick(_ p: CGPoint) {
        guard !sending else { return }
        let found = FbRegistry.shared.items(containing: p)
        guard !found.isEmpty else { selection = nil; status = "名前の付いた部品がありません（.fbTarget を付ける）"; return }
        selection = Selection(candidates: found, click: p); status = ""
    }

    /// 「親を選ぶ」: 1 つ外側の部品へ広げる
    func selectParent() {
        guard var s = selection, s.hasParent else { return }
        s.index += 1; selection = s
    }

    func send() async {
        guard var sel = selection, let win = appWindow, !sending else { return }
        guard let url = config.endpoint else { status = "送信先がありません（環境変数 FEEDBACK_ENDPOINT か Info.plist の FeedbackEndpoint）"; return }
        sending = true; status = "送信しています…"
        // キーボードを閉じ、アプリ側の「キーボードを避ける」配置が戻るのを待ってから、選んだ部品の矩形を引き直す
        overlayWindow?.endEditing(true)
        try? await Task.sleep(for: .milliseconds(500))
        if let now = FbRegistry.shared.item(sel.current.id) { sel.candidates[sel.index] = now }
        let shot = config.options["shots"] == false ? FeedbackShot.Result() : FeedbackShot.capture(window: win, rect: sel.current.rect)
        do {
            let body = try JSONSerialization.data(withJSONObject: makeBody(sel, win: win, shot: shot))
            let r = try await FeedbackTransport.post(body, to: url, token: config.token)
            if r.ok { exit(); return }
            status = "送れませんでした: \(r.error ?? "不明")" // 失敗したらモードを続ける
        } catch {
            status = "送れませんでした: \(error.localizedDescription)"
        }
        sending = false
    }

    private func makeBody(_ sel: Selection, win: UIWindow, shot: FeedbackShot.Result) -> [String: Any] {
        let item = sel.current, r = item.rect, size = win.bounds.size, scale = win.traitCollection.displayScale
        let screenId = config.getScreen()
        let info = Bundle.main.infoDictionary ?? [:]
        var env: [String: Any] = [
            "os": "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)",
            "device": UIDevice.current.model,
            "app": "\(info["CFBundleShortVersionString"] ?? "?") (\(info["CFBundleVersion"] ?? "?"))",
            "bundleId": Bundle.main.bundleIdentifier ?? "",
        ]
        #if targetEnvironment(simulator)
        env["simulator"] = true
        #endif
        var view = config.getView()
        if !JSONSerialization.isValidJSONObject(view) { view = ["error": "getView の値が JSON にできません"] }
        var state = config.stateOf(item.path)
        if !JSONSerialization.isValidJSONObject(state) { state = [:] }
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return [
            "payload": [
                "appName": config.appName, "pointer": config.pointer, "kind": "element",
                "pageOpenedAt": openedAt.timeIntervalSince1970 * 1000, "comment": comment,
                "target": [
                    "screen": screenId, "path": item.path, "element": item.path.split(separator: "/").last.map(String.init) ?? item.path,
                    "rect": ["x": r.minX, "y": r.minY, "width": r.width, "height": r.height], // pt。window の左上から
                    "click": ["x": sel.click.x, "y": sel.click.y], "state": state,
                ],
                "viewport": ["width": size.width, "height": size.height, "dpr": scale],
                "env": env,
                "screen": ["screen": screenId, "viewport": "\(Int(size.width)) × \(Int(size.height)) pt（scale \(scale)）"],
                "view": view,
                "logs": config.getRecentLogs().map { ["ts": iso.string(from: $0.ts), "level": $0.level, "text": $0.text] },
            ] as [String: Any],
            "options": config.options,
            "windowPng": shot.window ?? NSNull(), "elementPng": shot.element ?? NSNull(), "shotError": shot.error ?? NSNull(),
        ]
    }

    /// 環境変数 → Info.plist の順に読む（空文字は無し扱い）
    nonisolated static func setting(_ env: String, plistKey: String) -> String? {
        // 環境変数が空文字なら Info.plist に戻る（SIMCTL_CHILD_… を空で渡しても隠れない）
        let e = ProcessInfo.processInfo.environment[env]
        let v = (e?.isEmpty == false) ? e : Bundle.main.object(forInfoDictionaryKey: plistKey) as? String
        return (v?.isEmpty ?? true) ? nil : v
    }
}

/// モード中だけ最前面に出す window。シェイクの判定でも除く
final class FeedbackOverlayWindow: UIWindow {}

/// 入口: シェイク（シミュレータは Device > Shake）。不要なら消す
extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake, !(self is FeedbackOverlayWindow) { FeedbackMode.shared.enter() }
        super.motionEnded(motion, with: event)
    }
}

enum FeedbackTransport {
    struct Result { var ok: Bool; var id: String?; var ref: String?; var error: String? }

    /// 画面 → 受け口（POST /__feedback）。返りは { ok, id, ref, error }
    static func post(_ body: Data, to url: URL, token: String?) async throws -> Result {
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "content-type")
        if let token { req.setValue(token, forHTTPHeaderField: "x-feedback-token") }
        req.httpBody = body
        let (data, resp) = try await URLSession.shared.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        let j = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
        let ok = code == 200 && j["ok"] as? Bool == true
        return Result(ok: ok, id: j["id"] as? String, ref: j["ref"] as? String, error: ok ? nil : (j["error"] as? String ?? "HTTP \(code)"))
    }
}
#endif
