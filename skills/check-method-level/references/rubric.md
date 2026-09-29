# 検証スコア ルーブリック (詳細)

判定に迷ったときだけ読む。値の名前と数値は `scripts/calc-check-method-level.sh` が SSoT。
各軸は独立に、事実に基づいて判定する。各表は上ほど強い。
どの選択肢にも当てはまらず 2 つの間に当たる検証は、`unit..integration` のように 2 つのキーワードで指定する。

## Scope: どの範囲まで実際に通して検証したか

| 名前 | 定義 |
|---|---|
| e2e | E2E |
| integration | Integration Test |
| unit | Unit Test |
| temporary | 一時的なコード・Scratch・`main()` 等による実行確認 |
| static | 実行せず、コードや静的解析のみで確認 |
| none | 検証なし |

## Environment: 本番で発生する条件をどの程度再現しているか

端末、OS、ネットワーク、DB、外部 API、ユーザーデータなどの差を含む。

| 名前 | 定義 |
|---|---|
| real-user | 実際のユーザー環境 |
| prod-like | Production-like な環境 |
| test | Test / Staging / Emulator 等の検証環境 |
| mock | Fake / Mock / Stub 等を多く利用した環境 |
| none | 実行環境なし |

## Execution: 検証をどの程度自動・継続的に実行できるか

| 名前 | 定義 |
|---|---|
| ci | CI 等で継続的に自動実行 |
| automated | 自動実行可能 |
| manual | 人間または AI Agent による手動実行 |
| none | 実行なし |

## Evidence: 正誤判定の材料として、実際のプログラムから何を観測したか

「何を見たか」を表す。「誰が見たか」は Observer で評価する。

| 名前 | 定義 |
|---|---|
| perceived | ユーザーが実際に知覚する UI・音声等を直接確認 |
| media | Screenshot / Video / 録音等 |
| render-tree | Render Tree / DOM + computed style 等 |
| structured | Layout Tree / Accessibility Tree / 構造化された出力 |
| text | `println()` / stdout / log 等のテキスト出力 |
| debugger | Debugger 等による内部状態 |
| source | ソースコードのみ |

## Oracle: 観測結果を何を基準に正誤判定したか

| 名前 | 定義 |
|---|---|
| formal | Exhaustive / Formal verification |
| property | Property / Invariant |
| assertion | Explicit assertion / Golden / Snapshot / Diff |
| spec | 明示された仕様との比較 |
| comparison | Before / After や既存実装等との比較 |
| heuristic | 「見た感じ正しい」等のヒューリスティック判断 |
| none | 正誤判定なし |

Screenshot を取得しただけなら、Evidence は media でも Oracle は none。Screenshot を仕様書と比較したなら Oracle は spec。

## Observer: 実際に正誤を判定した主体の能力・安定性

AI はモデル名ではなく、その検証タスクに対する能力クラスで評価する。

| 名前 | 定義 |
|---|---|
| machine | Deterministic Machine |
| expert | 仕様・ドメインを十分理解した Expert Human |
| human | Human |
| ai-high | High-capability AI |
| ai-standard | Standard AI |
| ai-light | Lightweight AI |
| none | 判定者なし |

## AI Context Quality: AI が判定する場合に与えた判断材料の十分さ

Observer が AI のときだけ使う。「高性能 AI + Screenshot だけ」より「標準 AI + Screenshot + 仕様 + 期待結果」の方が強い検証になりうることを表す。

| 名前 | 定義 |
|---|---|
| full | 仕様・期待結果・実装・入力・出力等が十分に揃っている |
| spec | 仕様 + 実際の結果 |
| comparison | 比較対象 + 実際の結果 |
| result-only | 実際の結果のみ |
| none | AI を利用しない、または判断材料なし |

## Coverage: どれくらい多様な入力・状態・状況を検証したか

| 名前 | 定義 |
|---|---|
| exhaustive | Exhaustive |
| systematic | Property-based / Fuzz / Model-based 等による体系的な探索 |
| broad | 正常系・異常系・境界値等を広くカバー |
| multiple | 代表的な複数ケース |
| single | 単一ケース |
| unknown | Coverage 不明、または未考慮 |

## Repeatability: 同じ検証をもう一度再現できるか

| 名前 | 定義 |
|---|---|
| full | 条件・入力・手順が固定され完全に再現可能 |
| mostly | おおむね再現可能 |
| adhoc | Ad-hoc / 一時的な確認 |
| none | 再現可能な検証手順なし |

## 判定例

| 検証 | 特徴 |
|---|---|
| CI 上の本番相当 E2E + Screenshot Diff | Scope / Execution / Evidence / Oracle / Repeatability が高い |
| 人間が実機で手動確認 | Scope / Environment / Evidence は高いが、Execution / Repeatability は低い |
| AI が Screenshot + 仕様を確認 | Evidence と Oracle は高め。Observer と AI Context に依存 |
| AI が Layout Tree のみ確認 | UI 構造は確認できるが、実際の描画不具合を見逃しやすい |
| `println()` の結果を人間・AI が確認 | Evidence が限定的 |
| Unit Test + assertion | Scope は狭いが Oracle / Execution / Repeatability が高い |
| コードを読んで「問題なさそう」と判断 | Scope / Evidence / Oracle が低い |
| 未検証 | 全軸 none。スコアも 0 |
