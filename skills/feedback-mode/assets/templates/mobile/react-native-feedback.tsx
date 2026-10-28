/**
 * React Native 用の最小フィードバックモード。【汎用化で追加・未検証（コンパイル・実機とも未確認）】
 *
 * 参照実装（Web の DOM）との対応:
 *   data-fb               → <FbTarget name="…">（名前を付けた部品を registry に登録する）
 *   elementsFromPoint     → registry の各部品を measureInWindow して、タップ位置を含む最も小さい矩形を選ぶ（RN には DOM が無いため）
 *   親を選ぶ（祖先）       → FbTarget の入れ子（parent 名の連結）
 *   全面オーバーレイ       → モード中だけ最前面に置く Pressable（下の画面へタップが届かない）
 *   スクリーンショット     → react-native-view-shot の captureScreen（オーバーレイ・吹き出しを隠してから）
 *   送信                  → transport（画面 → 受け口）を props で差し替える。既定は無し（URL を埋め込まない）。例は下の devTransport
 *                          受け口: assets/templates/web/feedback-dev-server.mjs（裏の置き場 = Sink は受け口側で差し替える）。
 *                          エミュレータ: Android 10.0.2.2:7373 / iOS シミュレータ localhost:7373 / 実機: adb reverse tcp:7373 tcp:7373 か LAN の IP + FEEDBACK_TOKEN
 * 依存: react-native-view-shot（devDependencies にする）。
 *
 * debug 限定（【追加】参照実装には無い）:
 *   - 呼び出し側は `__DEV__` で囲った require を使い、本番バンドルにこのファイルが入らないようにする:
 *       const FeedbackProvider = __DEV__ ? require('./dev/react-native-feedback').FeedbackProvider : ({ children }: any) => children;
 *     （Metro が __DEV__ を定数に置換して dead branch を消すか【未検証】要確認。必ず本番バンドルを grep する: scripts/check-no-feedback-in-prod.sh）
 *   - このファイル自身も `if (!__DEV__) return <>{children}</>` の保険を持つ。
 *   - 送信先の URL・トークンは .env（react-native-config 等）の開発用だけに置き、本番の設定に入れない。
 *   - Android は debug source set（src/debug/）、iOS は #if DEBUG のネイティブ側（view-shot の権限は不要だが、依存自体を debug だけにしたい場合は
 *     `react-native.config.js` で release の autolinking から除外する【未検証】）。
 */
import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Dimensions, Platform, Pressable, Text, TextInput, View } from 'react-native';
import { captureScreen } from 'react-native-view-shot';

type Rect = { x: number; y: number; width: number; height: number };
type Entry = { path: string; ref: React.RefObject<View> };

const Ctx = createContext<{ register(id: string, e: Entry): () => void; parentPath: string; active: boolean }>({ register: () => () => {}, parentPath: '', active: false });
type Transport = (body: unknown) => Promise<{ ok: boolean; id?: string; ref?: string; error?: string }>;
/** 例（開発用）。URL・トークンは呼び出し側が開発用の設定から渡す。ここには埋め込まない */
export const devTransport = (endpoint: string, token?: string): Transport => async (body) => {
  const res = await fetch(endpoint, { method: 'POST', headers: { 'content-type': 'application/json', ...(token ? { 'x-feedback-token': token } : {}) }, body: JSON.stringify(body) });
  return res.json();
};

/** data-fb 相当。画面の主な部品を包む。入れ子にすると道筋が「親/子」になる */
export function FbTarget({ name, children, style }: { name: string; children: React.ReactNode; style?: any }) {
  const { register, parentPath } = useContext(Ctx);
  const path = parentPath ? `${parentPath}/${name}` : name;
  const ref = useRef<View>(null);
  const id = useRef(`${path}#${Math.random().toString(36).slice(2)}`).current;
  React.useEffect(() => register(id, { path, ref }), [id, path, register]);
  return (
    <Ctx.Consumer>
      {(c) => (
        <Ctx.Provider value={{ ...c, parentPath: path }}>
          <View ref={ref} collapsable={false} style={style}>{children}</View>
        </Ctx.Provider>
      )}
    </Ctx.Consumer>
  );
}

function safeLogs(fn?: () => unknown[]): unknown[] {
  try {
    return fn?.() ?? [];
  } catch {
    return []; // ログが読めなくても送る
  }
}

const measure = (ref: React.RefObject<View>) => new Promise<Rect | null>((res) => (ref.current ? ref.current.measureInWindow((x, y, width, height) => res({ x, y, width, height })) : res(null)));

export function FeedbackProvider({ children, appName, transport, getView, getRecentLogs }: { children: React.ReactNode; appName: string; transport: Transport; getView?: () => unknown; /** ログの差し込み口（アプリのログ基盤から読む。ログの実装は別タスク） */ getRecentLogs?: () => { ts: string; level: string; text: string }[] }) {
  const registry = useRef(new Map<string, Entry>()).current;
  const [active, setActive] = useState(false);
  const [sel, setSel] = useState<{ path: string; rect: Rect; click: { x: number; y: number } } | null>(null);
  const [comment, setComment] = useState('');
  const [capturing, setCapturing] = useState(false);
  const [status, setStatus] = useState('');
  const register = useCallback((id: string, e: Entry) => { registry.set(id, e); return () => { registry.delete(id); }; }, [registry]);

  const pick = async (x: number, y: number) => {
    let best: { path: string; rect: Rect } | null = null;
    for (const e of registry.values()) {
      const r = await measure(e.ref);
      if (!r || x < r.x || x > r.x + r.width || y < r.y || y > r.y + r.height) continue;
      if (!best || r.width * r.height < best.rect.width * best.rect.height) best = { path: e.path, rect: r }; // 最も小さい矩形 = いちばん内側
    }
    if (best) setSel({ ...best, click: { x, y } });
  };

  const send = async () => {
    if (!sel) return;
    setStatus('送信しています…');
    setCapturing(true); // 吹き出し・オーバーレイを隠す
    await new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(() => r(null))));
    let windowPng: string | null = null;
    try { windowPng = await captureScreen({ format: 'png', result: 'base64' }); } catch { /* 撮れなくても送る */ }
    setCapturing(false);
    const { width, height } = Dimensions.get('window');
    const body = {
      payload: {
        appName, comment, viewport: { width, height, dpr: 1 },
        target: { screen: '', path: sel.path, element: sel.path, rect: sel.rect, click: sel.click, state: {} },
        env: { platform: Platform.OS, version: String(Platform.Version) }, view: getView?.() ?? {}, logs: safeLogs(getRecentLogs),
      },
      options: { target: true, log: true, shots: true, env: false, screen: false, view: false },
      windowPng, elementPng: null, shotError: windowPng ? null : '撮れませんでした',
    };
    try {
      const j = await transport(body); // 画面 → 受け口。置き場（Sink）は受け口の裏で差し替える
      if (!j.ok) throw new Error(j.error);
      setActive(false); setSel(null); setComment(''); setStatus('');
    } catch (err: any) { setStatus(`追加できませんでした: ${err?.message ?? err}`); } // 失敗したらモードを続ける
  };

  if (!__DEV__) return <>{children}</>;
  return (
    <Ctx.Provider value={{ register, parentPath: '', active }}>
      {children}
      {!active && <Pressable onPress={() => setActive(true)} style={{ position: 'absolute', right: 8, bottom: 80, padding: 8, backgroundColor: '#2f6feb', borderRadius: 16 }}><Text style={{ color: '#fff' }}>FB</Text></Pressable>}
      {active && !capturing && (
        <View style={{ position: 'absolute', left: 0, right: 0, top: 0, bottom: 0 }}>
          {/* 全面のオーバーレイ: ここがタップを受け、下の画面には届かない */}
          <Pressable style={{ flex: 1 }} onPress={(e) => void pick(e.nativeEvent.pageX, e.nativeEvent.pageY)} />
          {sel && <View pointerEvents="none" style={{ position: 'absolute', left: sel.rect.x, top: sel.rect.y, width: sel.rect.width, height: sel.rect.height, borderWidth: 2, borderColor: '#2f6feb' }} />}
          <View style={{ position: 'absolute', left: 8, right: 8, bottom: 24, backgroundColor: '#fff', padding: 10, borderRadius: 8 }}>
            <Text>{sel ? sel.path : '部品をタップしてコメント'}</Text>
            {sel && <TextInput multiline value={comment} onChangeText={setComment} placeholder="この部品へのフィードバック" style={{ minHeight: 60, borderWidth: 1, borderColor: '#ccc' }} />}
            <View style={{ flexDirection: 'row', gap: 12 }}>
              {sel && <Pressable onPress={() => void send()}><Text>送信</Text></Pressable>}
              <Pressable onPress={() => { setActive(false); setSel(null); }}><Text>閉じる</Text></Pressable>
            </View>
            <Text>{status}</Text>
          </View>
        </View>
      )}
    </Ctx.Provider>
  );
}
