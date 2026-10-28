/**
 * 既存の React Native Dev Menu に「フィードバックモードへ」を 1 項目足すだけの最小例。【追加】【未検証: 実アプリ・実機で未検証】
 *
 * 新しい入口（FAB 等）も新しいガードも作らない。Dev Menu は __DEV__ のときだけ存在するので、本番除外は基盤に任せる。
 * （react-native-feedback.tsx の FeedbackProvider を持つ前提。FAB を出さず、メニューから enter する形にする場合は
 *  FeedbackProvider に `externalTrigger`（enter を呼ぶ関数を渡す口）を足す。下は「ref 経由で呼ぶ」最小形）
 *
 * 呼び出しは、基盤と同じ条件の中だけで（静的 import しない）:
 *   if (__DEV__) require('./dev/react-native-devmenu').register(feedbackRef);
 * 確認: release バンドルを scripts/check-no-feedback-in-prod.sh で grep（'DevSettings' 自体は RN 本体に残るので、フィードバック固有の文字列で見る）
 */
import { DevSettings } from 'react-native';

export function register(feedbackRef: { current: { enter(): void } | null }) {
  if (!__DEV__) return; // 保険。基盤（DevSettings）が __DEV__ でしか動かないことは release バンドルで確認する
  DevSettings.addMenuItem('フィードバックモードへ', () => feedbackRef.current?.enter());
}
