---
name: check-method-level
description: >
  テスト・動作確認の方法がどれくらい強いか (検証方法レベル) を、Scope / Environment / Evidence / Oracle 等の
  9 軸で採点し、同梱 script で Verification Score と Level を算出してユーザに伝える。
  テスト・動作確認をした時、テスト・動作確認方法を検討・計画する時、不具合を検知できなかった理由を述べる時に使う。
  言語・フレームワークを問わない (python3 があれば動く)。
  Use when requested: "検証レベル", "検証方法レベル", "check method level", "この確認方法はどれくらい強い?",
  "なぜテストで見逃した?", "テスト方針の比較", "動作確認の強さを評価して".
metadata:
  status: Experimental
  group: テスト・検証
---

# check-method-level

検証方法ごとに 9 軸の値を判断してスクリプトに渡し、スコアを算出する。**スコアの計算はスクリプトが行う。自分で計算しないこと。**
各軸の値は事実だけを見て選ぶ。スコアを上げる目的で甘く付けない。

## 手順

1. 対象の検証方法 (実施済み / 計画中 / 見逃した方法) を 1 つずつ特定する
2. 下の早見表で各軸の値を選ぶ。**分からない軸を推測で埋めない。** 事実が不明ならユーザに確認するか、`none` / `unknown` を選んでその理由を添える
3. 同梱 script を実行する。script を読解・書き換え・再実装せず、そのまま実行する。
   `${CLAUDE_SKILL_DIR}` はこの SKILL.md があるディレクトリ。未定義の環境ではその絶対パスに置き換える
   ```sh
   python3 "${CLAUDE_SKILL_DIR}/scripts/calc-check-method-level.py" \
     --scope=<v> --env=<v> --execution=<v> --evidence=<v> --oracle=<v> \
     --observer=<v> --coverage=<v> --repeatability=<v> [--ai-context=<v>] --label="<検証方法名>"
   ```
   未指定の軸があると exit 2 になり、選択肢が表示される。軸の表 (`-v`) や JSON (`--format=json`) は必要なときだけ付ける。
   どの選択肢にも当てはまらず 2 つの間に当たる場合だけ、`--scope=unit..integration` のように中間を指定できる (出力に `Note:` が出るので報告に含める)
4. ユーザに報告する (下の「報告」を参照)

判定に迷ったときだけ `references/rubric.md` を読む。各値の定義と判定例がまとまっている。

## 早見表 (キーワードで指定する。左ほど強い)

| 軸 | 値 |
|---|---|
| scope | e2e > integration > unit > temporary (scratch・main()) > static (コード読み・静的解析) > none |
| env | real-user > prod-like > test (staging・emulator) > mock > none |
| execution | ci > automated (自動実行できる) > manual (人・AI が手で) > none |
| evidence | perceived (UI・音を直接) > media (screenshot・video) > render-tree (DOM+style) > structured (layout・a11y tree・構造化出力) > text (log・stdout) > debugger > source |
| oracle | formal > property > assertion (golden・snapshot・diff) > spec (仕様と比較) > comparison (before/after) > heuristic (見た感じ) > none |
| observer | machine > expert > human > ai-high > ai-standard > ai-light > none |
| coverage | exhaustive > systematic (PBT・fuzz) > broad (正常・異常・境界) > multiple > single > unknown |
| repeatability | full > mostly > adhoc > none |
| ai-context | full > spec > comparison > result-only > none |

ai-context は observer が ai-* のときだけ必須。

- evidence は「何を見たか」、observer は「誰が判定したか」を表す。混同しないこと
- AI の自分が screenshot を見て判断した場合は `observer=ai-high` とし、渡した判断材料に応じて ai-context を選ぶ

## 報告

ユーザには短く伝える。スクリプトの出力を全部貼らない。

```
検証レベル: B (122/197) — ViewModel の unit test
弱点: scope=unit・env=mock。次の一手は integration test 化、staging での実行
```

- 複数の方法を比較・計画する場合は、1 方法 1 行の表 (方法 / Level / Score / 主な弱点) にまとめる
- 不具合を見逃した理由を説明する場合は、見逃しの原因になった軸を指摘する (例: 「evidence=structured だったので描画崩れを観測できなかった」)
- `Warn:` が出たら、値の選び方を見直してから報告する
