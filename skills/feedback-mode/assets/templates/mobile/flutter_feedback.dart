// Flutter 用の最小フィードバックモード。【汎用化で追加・未検証（コンパイル・実機とも未確認）】
//
// 参照実装（Web の DOM）との対応:
//   data-fb            → FbTarget(name: '…', child: …)（GlobalKey で RenderBox を取れるようにして registry に登録）
//   elementsFromPoint  → registry の各 RenderBox を globalToLocal で当たり判定し、最も小さい矩形を選ぶ
//   全面オーバーレイ    → モード中だけ Stack の最前面に置く GestureDetector（下の画面にタップが届かない）
//   スクリーンショット  → アプリ全体を RepaintBoundary(key: FeedbackRoot.boundaryKey) で包み、toImage → PNG（オーバーレイを隠してから）
//   送信               → transport（画面 → 受け口）を引数で差し替える。既定は無し（URL を埋め込まない）。例は下の devTransport
//                        受け口: assets/templates/web/feedback-dev-server.mjs（裏の置き場 = Sink は受け口側で差し替える）。
//                        Android エミュレータ 10.0.2.2:7373 / iOS シミュレータ localhost:7373 / 実機は LAN の IP + FEEDBACK_TOKEN か adb reverse
// 依存: pubspec.yaml に http（lib から使うので dependencies。release でも依存自体は残るが、import されなければバイナリには入らない【未検証】）。
//       debug 限定にするには、別 entrypoint（lib/main_dev.dart）だけがこのファイルを import する構成にし、本番の lib/main.dart からは import しない
//       （`flutter run -t lib/main_dev.dart`）。【追加】参照実装には無い。kDebugMode の保険も入れてある。本番（release）の成果物を grep して確認する: scripts/check-no-feedback-in-prod.sh【未検証】
// 送信先の URL・トークンは --dart-define-from-file=env.development.json 等の開発用だけに置き、本番の定義に入れない。
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:http/http.dart' as http; // devTransport だけが使う

/// 画面 → 受け口。body（JSON 文字列）を渡し、{ok, id, ref, error} を返す。
typedef FeedbackTransport = Future<Map<String, dynamic>> Function(String body);

/// 例（開発用）。endpoint・token は呼び出し側が開発用の定義から渡す
FeedbackTransport devTransport(String endpoint, {String? token}) => (body) async {
      final res = await http.post(Uri.parse(endpoint), headers: {'content-type': 'application/json', if (token != null) 'x-feedback-token': token}, body: body);
      return jsonDecode(res.body) as Map<String, dynamic>;
    };

class _Entry {
  _Entry(this.path, this.key);
  final String path;
  final GlobalKey key;
}

final Map<String, _Entry> _registry = {};

/// data-fb 相当。画面の主な部品を包む。入れ子にすると道筋は呼び出し側で「親/子」の名前にする（ここでは name をそのまま使う）。
class FbTarget extends StatefulWidget {
  const FbTarget({super.key, required this.name, required this.child});
  final String name;
  final Widget child;
  @override
  State<FbTarget> createState() => _FbTargetState();
}

class _FbTargetState extends State<FbTarget> {
  final GlobalKey _k = GlobalKey();
  late final String _id = '${widget.name}#${identityHashCode(this)}';
  @override
  void initState() {
    super.initState();
    _registry[_id] = _Entry(widget.name, _k);
  }

  @override
  void dispose() {
    _registry.remove(_id);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => KeyedSubtree(key: _k, child: widget.child);
}

class FeedbackRoot extends StatefulWidget {
  const FeedbackRoot({super.key, required this.child, required this.appName, required this.transport, this.getRecentLogs});
  final Widget child;
  final String appName;
  final FeedbackTransport transport;
  /// ログの差し込み口（アプリのログ基盤から読む。ログの実装は別タスク）。[{ts, level, text}] を古い順に
  final List<Map<String, String>> Function()? getRecentLogs;
  @override
  State<FeedbackRoot> createState() => _FeedbackRootState();
}

class _FeedbackRootState extends State<FeedbackRoot> {
  final GlobalKey _boundary = GlobalKey();
  bool _active = false, _capturing = false;
  String? _path;
  Rect? _rect;
  Offset _click = Offset.zero;
  final _comment = TextEditingController();
  String _status = '';

  void _pick(Offset p) {
    _Entry? best;
    Rect? bestRect;
    for (final e in _registry.values) {
      final ro = e.key.currentContext?.findRenderObject();
      if (ro is! RenderBox || !ro.attached) continue;
      final r = ro.localToGlobal(Offset.zero) & ro.size;
      if (!r.contains(p)) continue;
      if (bestRect == null || r.width * r.height < bestRect.width * bestRect.height) {
        best = e;
        bestRect = r;
      }
    }
    if (best != null) setState(() { _path = best!.path; _rect = bestRect; _click = p; });
  }

  Future<void> _send() async {
    setState(() { _status = '送信しています…'; _capturing = true; });
    await WidgetsBinding.instance.endOfFrame;
    await WidgetsBinding.instance.endOfFrame;
    String? png;
    try {
      final b = _boundary.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final img = await b.toImage(pixelRatio: MediaQuery.of(context).devicePixelRatio);
      final data = await img.toByteData(format: ui.ImageByteFormat.png);
      png = base64Encode(data!.buffer.asUint8List());
    } catch (_) {/* 撮れなくても送る */}
    if (mounted) setState(() => _capturing = false);
    final size = MediaQuery.of(context).size;
    final r = _rect!;
    List<Map<String, String>> getLogs() => widget.getRecentLogs?.call() ?? const [];
    final body = jsonEncode({
      'payload': {
        'appName': widget.appName, 'comment': _comment.text,
        'viewport': {'width': size.width, 'height': size.height, 'dpr': MediaQuery.of(context).devicePixelRatio},
        'target': {'screen': '', 'path': _path, 'element': _path, 'rect': {'x': r.left, 'y': r.top, 'width': r.width, 'height': r.height}, 'click': {'x': _click.dx, 'y': _click.dy}, 'state': {}},
        'env': {'platform': defaultTargetPlatform.name}, 'logs': (() { try { return getLogs(); } catch (_) { return <Map<String, String>>[]; } })(),
      },
      'options': {'target': true, 'log': true, 'shots': true, 'env': false, 'screen': false, 'view': false},
      'windowPng': png, 'elementPng': null, 'shotError': png == null ? '撮れませんでした' : null,
    });
    try {
      final j = await widget.transport(body); // 画面 → 受け口。置き場（Sink）は受け口の裏で差し替える
      if (j['ok'] != true) throw Exception(j['error']);
      setState(() { _active = false; _path = null; _comment.clear(); _status = ''; });
    } catch (e) {
      setState(() => _status = '追加できませんでした: $e'); // 失敗したらモードを続ける
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!kDebugMode) return widget.child;
    return Stack(textDirection: TextDirection.ltr, children: [
      RepaintBoundary(key: _boundary, child: widget.child),
      if (!_active)
        Positioned(right: 8, bottom: 80, child: GestureDetector(onTap: () => setState(() => _active = true), child: Container(padding: const EdgeInsets.all(8), color: const Color(0xFF2F6FEB), child: const Text('FB', style: TextStyle(color: Colors.white, decoration: TextDecoration.none))))),
      if (_active && !_capturing) ...[
        Positioned.fill(child: GestureDetector(behavior: HitTestBehavior.opaque, onTapUp: (d) => _pick(d.globalPosition), child: const SizedBox.expand())),
        if (_rect != null) Positioned.fromRect(rect: _rect!, child: IgnorePointer(child: Container(decoration: BoxDecoration(border: Border.all(color: const Color(0xFF2F6FEB), width: 2))))),
        Positioned(left: 8, right: 8, bottom: 24, child: Material(color: Colors.white, child: Padding(padding: const EdgeInsets.all(10), child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(_path ?? '部品をタップしてコメント'),
          if (_path != null) TextField(controller: _comment, maxLines: 3, decoration: const InputDecoration(hintText: 'この部品へのフィードバック')),
          Row(children: [
            if (_path != null) TextButton(onPressed: _send, child: const Text('送信')),
            TextButton(onPressed: () => setState(() { _active = false; _path = null; }), child: const Text('閉じる')),
          ]),
          Text(_status),
        ])))),
      ],
    ]);
  }
}
