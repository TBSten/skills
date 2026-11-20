// FeedbackOverlay.swift — モード中の画面（全面オーバーレイ・枠・吹き出し）と、部品の registry（Debug だけ）。
#if DEBUG
import SwiftUI

/// .fbTarget で名前を付けた部品の矩形（window 座標の pt）を覚える
/// 画面が動く（キーボードを避ける・スクロール）と矩形も更新されるので、選んだ部品は id で引き直す
@MainActor
final class FbRegistry: ObservableObject {
    static let shared = FbRegistry()
    struct Item: Equatable { var id: UUID; var path: String; var rect: CGRect }
    @Published private var items: [UUID: Item] = [:]

    func set(_ id: UUID, _ item: Item?) { if items[id] != item { items[id] = item } }
    func item(_ id: UUID) -> Item? { items[id] }

    /// 位置を含む部品を小さい順（= 内側から）に返す
    func items(containing p: CGPoint) -> [Item] {
        items.values.filter { $0.rect.contains(p) && $0.rect.width > 0 && $0.rect.height > 0 }
            .sorted { $0.rect.width * $0.rect.height < $1.rect.width * $1.rect.height }
    }
}

extension EnvironmentValues {
    @Entry var fbPath: String = "" // 外側の .fbTarget の道筋
}

struct FbTargetModifier: ViewModifier {
    let name: String
    @Environment(\.fbPath) private var parent
    @State private var id = UUID()

    func body(content: Content) -> some View {
        let path = parent.isEmpty ? name : "\(parent)/\(name)"
        content
            .environment(\.fbPath, path)
            .background {
                GeometryReader { g in
                    let r = g.frame(in: .global) // .global = window 座標
                    Color.clear
                        .onAppear { FbRegistry.shared.set(id, .init(id: id, path: path, rect: r)) }
                        .onChange(of: r) { _, new in FbRegistry.shared.set(id, .init(id: id, path: path, rect: new)) }
                        .onDisappear { FbRegistry.shared.set(id, nil) }
                }
            }
    }
}

/// 入口ボタン（既存の debug 画面が無いときだけ使う）
struct FeedbackEntryButton: View {
    @ObservedObject private var mode = FeedbackMode.shared
    var body: some View {
        if !mode.active {
            Button { mode.enter() } label: {
                Text("FB").font(.caption.bold()).foregroundStyle(.white)
                    .frame(width: 36, height: 36).background(Circle().fill(.blue.opacity(0.8)))
            }
            .accessibilityLabel("フィードバックモード")
            .padding(12)
        }
    }
}

struct FeedbackOverlayView: View {
    @ObservedObject var mode: FeedbackMode
    @ObservedObject private var registry = FbRegistry.shared // 枠を今の位置に追従させる

    var body: some View {
        ZStack {
            // 全面: すべてのタップをここで受ける（下のアプリには届かない）
            Color.black.opacity(0.06)
                .contentShape(Rectangle())
                .ignoresSafeArea()
                .gesture(SpatialTapGesture(coordinateSpace: .global).onEnded { mode.pick($0.location) })
            if let r = mode.selection.map({ registry.item($0.current.id)?.rect ?? $0.current.rect }) {
                Color.clear
                    .overlay(alignment: .topLeading) {
                        Rectangle().stroke(.blue, lineWidth: 2)
                            .frame(width: r.width, height: r.height)
                            .offset(x: r.minX, y: r.minY)
                    }
                    .ignoresSafeArea() // 左上 = window の左上にそろえる
                    .allowsHitTesting(false)
            }
            VStack { Spacer(); panel }
        }
    }

    private var panel: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Text(mode.selection?.current.path ?? "部品をタップして選ぶ").font(.headline).lineLimit(2)
                Spacer()
                Button("やめる") { mode.exit() }
            }
            if let s = mode.selection {
                Button("親を選ぶ") { mode.selectParent() }.disabled(!s.hasParent)
                TextField("この部品へのフィードバック", text: $mode.comment, axis: .vertical)
                    .lineLimit(2...6)
                    .textFieldStyle(.roundedBorder)
                    .accessibilityIdentifier("feedback.comment")
                Button("送る") { Task { await mode.send() } }
                    .buttonStyle(.borderedProminent)
                    .disabled(mode.sending)
            }
            if !mode.status.isEmpty { Text(mode.status).font(.footnote).foregroundStyle(.red) }
        }
        .padding(14)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
        .padding(12)
    }
}
#endif
