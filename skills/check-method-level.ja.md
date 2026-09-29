# check-method-level

**テスト・動作確認の方法がどれくらい強いか**を 9 つの軸で採点し、Verification Score と Level で伝えるスキル。
「unit test を書いた」「画面を目で見た」「コードを読んで大丈夫そうだった」のような検証を同じ物差しで比べられるようにする。

AI は各軸の値を**キーワードで選ぶだけ**で、計算は同梱の Python script が行う。

## インストール

```sh
gh skill install tbsten/skills check-method-level
```

前提: `python3` (3.9 以上。標準ライブラリのみ使用)

## いつ使うか

- テスト・動作確認をした後に、その検証がどれくらい信頼できるかを伝える時
- テスト・動作確認の方法を検討・計画し、複数の案を比較する時
- 不具合をなぜ検知できなかったかを説明する時 (どの軸が弱かったかを指摘する)

CLAUDE.md に「テスト・動作確認の際は check-method-level skill で検証方法レベルを伝える」と書いておくと、毎回自動で使われる。

## 9 つの軸

| 軸 | 何を表すか | 選択肢 (左ほど強い) |
|---|---|---|
| Scope | どの範囲まで実際に通して検証したか | e2e > integration > unit > temporary > static > none |
| Environment | 本番の条件をどこまで再現しているか | real-user > prod-like > test > mock > none |
| Execution | どの程度自動・継続的に実行できるか | ci > automated > manual > none |
| Evidence | 実際のプログラムから何を観測したか | perceived > media > render-tree > structured > text > debugger > source |
| Oracle | 何を基準に正誤を判定したか | formal > property > assertion > spec > comparison > heuristic > none |
| Observer | 誰が判定したか | machine > expert > human > ai-high > ai-standard > ai-light > none |
| Coverage | どれくらい多様な入力・状態を検証したか | exhaustive > systematic > broad > multiple > single > unknown |
| Repeatability | 同じ検証を再現できるか | full > mostly > adhoc > none |
| AI Context Quality | AI が判定する場合に与えた判断材料の十分さ (Observer が AI の時のみ) | full > spec > comparison > result-only > none |

各値の定義は [`check-method-level/references/rubric.md`](./check-method-level/references/rubric.md) を参照。

## スコア式

```text
Verification Score =
    Scope × 20 + Environment × 4 + Execution × 2 + Evidence × 3 + Oracle × 4
  + Observer × 2 + Coverage × 3 + Repeatability × 2 + AI Context Quality
```

- 各軸の段階の値は、選択肢を強い順に並べたときの位置で決まる。最後の選択肢が 0 (例: Scope は e2e=5 … none=0)
- Scope を最も重くしている。`E2E > Integration > Unit > Temporary > Static > None` の序列が、他の軸だけでは簡単に逆転しないようにするため
- 到達可能な最大値は 197 (Observer=machine のとき。AI Context は 0 になる)
- Level: S ≥160 / A ≥130 / B ≥100 / C ≥60 / D ≥20 / E >0 / - =0

### 重みと段階の数値を AI に見せない理由

重み・段階の数値は script (`scripts/method_level_axes.py`) の中だけに置き、SKILL.md・rubric.md・script の出力には出さない。
重みを知った AI は、点数の大きい軸や値を甘く見積もる方向に判断が偏りやすいため。
このスコア式の説明もスキルのディレクトリ外 (このドキュメント) に置いており、スキル実行時の context には載らない。

## コスト (build-cost / run-cost)

検証方法を検討・計画・比較する時は、スコアとは別にコストも示す (実施後の報告や見逃しの分析では任意)。

| 軸 | 何を表すか | 選択肢 (左ほど安い) |
|---|---|---|
| build-cost | この検証のために追加で必要な構築 | existing > add-case > new-harness > new-environment > external |
| run-cost | 1 回の実行時間 (待ち時間・人の作業時間を含む) | seconds > minutes > tens-of-minutes > hours > days |

- **スコアに混ぜない理由**: スコアは「その検証でどれだけ確信を持てるか」を表す。コストを引くと「弱い検証」と「強いけど高い検証」が区別できなくなり、安くて弱い検証を選ぶ方向に判断が偏る。見逃しの分析でも原因の軸がぼやける
- **2 軸に分けた理由**: 構築コストと実行コストは性質が違う。CI の E2E は構築が高く実行は安い。手動 QA は構築が安く実行が高い
- **事実ベースで定義した理由**: `low` / `medium` / `high` のような段階は何を medium とするかの判断が AI に委ねられ、ぶれやすい。「何を新しく用意するか」「1 回に何分かかるか」という確認できる事実で選ばせる
- 見積もりに幅がある場合は `--run-cost=minutes..tens-of-minutes` のように範囲で指定する。スコア軸の中間指定と違い平均は取らず、安い順に並べた幅のまま表示する
- 2 軸はセットで指定する (片方だけだと exit 2)。金額・保守の手間 (flaky 対応等) は軸に含めていないので、必要なら文章で添える

```text
$ ... --build-cost=add-case --run-cost=hours..minutes
Score: 122/197 Level B (Scope: unit)
Axes: scope=unit env=mock ...
Cost: build=add-case run=minutes..hours
```

## 使い方

```sh
python3 "${CLAUDE_SKILL_DIR}/scripts/calc-check-method-level.py" \
  --scope=unit --env=mock --execution=ci --evidence=structured --oracle=assertion \
  --observer=machine --coverage=broad --repeatability=full --label="ViewModel の unit test"
```

```text
Method: ViewModel の unit test
Score: 122/197 Level B (Scope: unit)
Axes: scope=unit env=mock execution=ci evidence=structured oracle=assertion observer=machine coverage=broad repeatability=full
Next: scope→integration, env→test, evidence→render-tree
```

- **デフォルト値は無い**。未指定の軸があると exit 2 で止まり、足りない軸の選択肢だけを表示する
- 値はキーワード (とエイリアス) だけで指定する。数値はスコアと誤認されやすいので受け付けない
- **中間指定**: どの選択肢にも当てはまらず 2 つの間に当たる検証は `--scope=unit..integration` のように書く。出力に `Note:` が出る
- `--ai-context` は Observer が AI (ai-high / ai-standard / ai-light) の時のみ必須
- 組み合わせが矛盾していると `Warn:` を出す (例: 実行を伴う Scope なのに execution=none)
- `-v` で軸ごとの意味を表示、`--format=json` で JSON 出力

<details>
<summary>その他の実行例</summary>

AI が Playwright 等で画面を見て仕様と比較した E2E:

```text
$ ... --scope=e2e --env=staging --execution=manual --evidence=screenshot --oracle=spec \
      --observer=ai-high --ai-context=spec --coverage=multiple --repeatability=mostly
Score: 156/197 Level A (Scope: e2e)
Next: oracle→assertion, coverage→broad, env→prod-like
```

コードを読んで「問題なさそう」と判断:

```text
$ ... --scope=static --env=none --execution=manual --evidence=source --oracle=heuristic \
      --observer=ai-high --ai-context=result-only --coverage=unknown --repeatability=none
Score: 33/197 Level D (Scope: static)
```

JVM 上の screenshot test (中間指定):

```text
$ ... --scope=unit..integration --env=emulator..mock --execution=ci --evidence=screenshot \
      --oracle=snapshot --observer=machine --coverage=multiple --repeatability=full
Score: 137/197 Level A (Scope: unit..integration)
Note: scope=unit..integration は選択肢の中間として扱った
Note: env=test..mock は選択肢の中間として扱った
```

</details>

## ファイル構成

```
check-method-level/
├── SKILL.md                               # 手順・早見表・報告の書き方
├── references/rubric.md                   # 各値の定義と判定例 (判定に迷った時だけ読む)
└── scripts/
    ├── calc-check-method-level.py         # スコア算出 script
    ├── method_level_axes.py               # 軸 (スコア・コスト)・選択肢・重み・Level の定義 (SSoT)
    ├── method_level_cost.py               # コスト軸の解決と出力
    └── test_calc_check_method_level.py    # テスト
```

テスト: `python3 -m unittest discover -s skills/check-method-level/scripts`

重みを変えた場合は、テストの期待スコアも更新する。
