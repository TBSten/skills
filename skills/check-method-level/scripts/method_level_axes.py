"""検証スコアの軸と選択肢の定義 (SSoT)。

スコア軸 (AXES) の選択肢は強い順に並べる。段階 (rank) は並び順から決まり、最後の選択肢が 0。
コスト軸 (COST_AXES) はスコアに含めない (weight=0)。選択肢は安い順に並べる。
重み (weight) と rank はこのディレクトリの script の中だけで使い、出力には出さない (理由は calc-check-method-level.py の docstring)。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional, Tuple


@dataclass(frozen=True)
class Choice:
    name: str
    desc: str
    aliases: Tuple[str, ...] = ()


@dataclass(frozen=True)
class Axis:
    key: str
    title: str
    weight: int
    choices: Tuple[Choice, ...]  # スコア軸は強い順、コスト軸は安い順

    @property
    def max_rank(self) -> int:
        return len(self.choices) - 1

    def rank(self, choice: Choice) -> int:
        return self.max_rank - self.choices.index(choice)

    def choice_at(self, rank: int) -> Choice:
        return self.choices[self.max_rank - rank]

    def find(self, keyword: str) -> Optional[Choice]:
        for choice in self.choices:
            if keyword == choice.name or keyword in choice.aliases:
                return choice
        return None

    def keywords(self) -> str:
        return " > ".join(choice.name for choice in self.choices)


def normalize_keyword(text: str) -> str:
    return text.strip().lower().replace("_", "-")


def is_number(text: str) -> bool:
    try:
        float(text)
        return True
    except ValueError:
        return False


def _c(name: str, desc: str, *aliases: str) -> Choice:
    return Choice(name, desc, aliases)


AXES: Tuple[Axis, ...] = (
    Axis("scope", "Scope", 20, (
        _c("e2e", "E2E", "end-to-end"),
        _c("integration", "Integration Test", "integration-test", "it"),
        _c("unit", "Unit Test", "unit-test", "ut"),
        _c("temporary", "一時的なコード・Scratch・main() 等による実行確認", "temp", "scratch", "main", "adhoc-run"),
        _c("static", "Static Verification (実行せずコード・静的解析のみ)", "static-analysis", "code-read", "review"),
        _c("none", "検証なし", "no"),
    )),
    Axis("env", "Environment", 4, (
        _c("real-user", "実際のユーザー環境", "real", "user", "production", "prod"),
        _c("prod-like", "Production-like な環境", "production-like"),
        _c("test", "Test / Staging / Emulator 等の検証環境", "staging", "emulator", "simulator", "test-env"),
        _c("mock", "Fake / Mock / Stub 等を多く利用した環境", "fake", "stub"),
        _c("none", "実行環境なし", "no"),
    )),
    Axis("execution", "Execution", 2, (
        _c("ci", "CI 等で継続的に自動実行", "on-ci", "continuous"),
        _c("automated", "自動実行可能", "automatable", "auto"),
        _c("manual", "人間または AI Agent による手動実行", "by-hand", "by-agent"),
        _c("none", "実行なし", "no"),
    )),
    Axis("evidence", "Evidence", 3, (
        _c("perceived", "ユーザーが実際に知覚する UI・音声等を直接確認", "direct", "real-ui"),
        _c("media", "Screenshot / Video / 録音等", "screenshot", "video", "recording"),
        _c("render-tree", "Render Tree / DOM + computed style 等", "dom", "dom-style", "computed-style"),
        _c("structured", "Layout Tree / Accessibility Tree / 構造化された出力",
           "layout-tree", "a11y-tree", "accessibility-tree", "json"),
        _c("text", "println() / stdout / log 等のテキスト出力", "log", "stdout", "println"),
        _c("debugger", "Debugger 等による内部状態", "internal-state"),
        _c("source", "ソースコードのみ", "source-only", "code"),
    )),
    Axis("oracle", "Oracle", 4, (
        _c("formal", "Exhaustive / Formal verification", "exhaustive"),
        _c("property", "Property / Invariant", "invariant"),
        _c("assertion", "Explicit assertion / Golden / Snapshot / Diff", "golden", "snapshot", "diff"),
        _c("spec", "明示された仕様との比較", "specification"),
        _c("comparison", "Before / After や既存実装等との比較", "before-after", "existing-impl"),
        _c("heuristic", "「見た感じ正しい」等のヒューリスティック判断", "looks-good", "eyeball"),
        _c("none", "正誤判定なし", "no"),
    )),
    Axis("observer", "Observer", 2, (
        _c("machine", "Deterministic Machine", "deterministic"),
        _c("expert", "仕様・ドメインを十分理解した Expert Human", "expert-human"),
        _c("human", "Human"),
        _c("ai-high", "High-capability AI", "high-ai"),
        _c("ai-standard", "Standard AI", "standard-ai"),
        _c("ai-light", "Lightweight AI", "lightweight-ai", "light-ai"),
        _c("none", "判定者なし", "no"),
    )),
    Axis("coverage", "Coverage", 3, (
        _c("exhaustive", "Exhaustive"),
        _c("systematic", "Property-based / Fuzz / Model-based 等による体系的な探索",
           "property-based", "pbt", "fuzz", "model-based"),
        _c("broad", "正常系・異常系・境界値等を広くカバー", "wide"),
        _c("multiple", "代表的な複数ケース", "representative"),
        _c("single", "単一ケース", "one"),
        _c("unknown", "Coverage 不明、または未考慮", "none"),
    )),
    Axis("repeatability", "Repeatability", 2, (
        _c("full", "条件・入力・手順が固定され完全に再現可能", "fixed"),
        _c("mostly", "おおむね再現可能", "almost"),
        _c("adhoc", "Ad-hoc / 一時的な確認", "ad-hoc", "temporary"),
        _c("none", "再現可能な検証手順なし", "no"),
    )),
    Axis("ai-context", "AI Context Quality", 1, (
        _c("full", "仕様・期待結果・実装・入力・出力等が十分に揃っている", "complete"),
        _c("spec", "仕様 + 実際の結果", "spec-and-result"),
        _c("comparison", "比較対象 + 実際の結果", "reference-and-result"),
        _c("result-only", "実際の結果のみ", "result"),
        _c("none", "AI を利用しない、または判断材料なし", "no"),
    )),
)

AXIS_BY_KEY = {axis.key: axis for axis in AXES}

COST_AXES: Tuple[Axis, ...] = (
    Axis("build-cost", "Build Cost", 0, (
        _c("existing", "既存の検証をそのまま実行できる。追加の構築なし", "none", "reuse"),
        _c("add-case", "既存の基盤・ヘルパーの範囲、または使い捨てのコードでケース・手順を追加する", "case", "scratch"),
        _c("new-harness", "新しいテスト用ライブラリ・fixture・fake・script 等の導入・作成が必要", "harness"),
        _c("new-environment", "新しい実行環境 (staging・emulator・実機・DB・外部サービスの sandbox 等) の用意が必要",
           "environment", "new-env"),
        _c("external", "プロジェクト外の調達・契約・権限・他者の協力が必要", "procurement"),
    )),
    Axis("run-cost", "Run Cost", 0, (
        _c("seconds", "1 回の実行が 1 分未満", "instant", "sec"),
        _c("minutes", "1 回の実行が 1〜10 分", "min"),
        _c("tens-of-minutes", "1 回の実行が 10〜60 分", "under-hour"),
        _c("hours", "1 回の実行が 1 時間〜1 日", "hour"),
        _c("days", "1 回の実行が 1 日以上", "day"),
    )),
)
COST_AXIS_BY_KEY = {axis.key: axis for axis in COST_AXES}
AI_OBSERVERS = ("ai-high", "ai-standard", "ai-light")

# Level の閾値 (score 以上)。強い順。
LEVELS: Tuple[Tuple[str, int], ...] = (("S", 160), ("A", 130), ("B", 100), ("C", 60), ("D", 20), ("E", 1))
