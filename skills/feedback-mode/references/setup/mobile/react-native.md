# mobile / react-native

検証状況: 未検証（コンパイル・実機とも未確認）。Fabric（新アーキテクチャ）での `measureInWindow` の挙動、Metro が `__DEV__` の分岐を消すかは要確認。
ひな形: `assets/templates/adapters/mobile/react-native/`（`react-native-feedback.tsx` の `FeedbackProvider` と `FbTarget`・`react-native-devmenu.ts` Dev Menu に 1 項目足す例）。受け口は開発機の `core/feedback-dev-server.mjs`。先に `common.md` を読む。

## 差し込み口

| 差し込み口 | 埋め方 |
|---|---|
| `entry` | Dev Menu に項目を足す（`react-native-devmenu.ts`）。無ければ FAB |
| `pick` / `name` | `<FbTarget name="画面/部品">` で包んだ部品を registry に登録し、タップ位置を含む最小の矩形を選ぶ |
| `block` | モード中だけ最前面に置く `Pressable` |
| `collect` | `getView`（store の状態）。`env` は `Platform` から自動 |
| `logs` | `getRecentLogs`（既存の logger のバッファ） |
| `capture` | `react-native-view-shot` の `captureScreen`（オーバーレイを隠してから）。devDependencies に入れる |
| `transport` | `devTransport(endpoint, token)`（ひな形の例）。endpoint・token は `react-native-config` の `.env.development` から |
| `pointer` | ひな形に無い。payload に足す |

`Modal` は別ウィンドウなので、Modal の中にも `FeedbackProvider` が要る。

## 本番から外す

- 呼び出し側は `__DEV__` で囲った require にする: `const FeedbackProvider = __DEV__ ? require('./dev/react-native-feedback').FeedbackProvider : ({ children }) => children`。ひな形も `if (!__DEV__)` の保険を持つ。
- 依存（view-shot）を release の autolinking から外すなら `react-native.config.js`（未検証）。
- 権限・cleartext・ATS は `common.md`。
- 確かめる: release の apk / ipa（JS バンドルを含む）を `check-no-feedback-in-prod.sh`。
