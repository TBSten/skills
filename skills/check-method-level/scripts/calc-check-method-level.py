#!/usr/bin/env python3
"""検証方法の強さ (Verification Score) を算出する。

軸と選択肢: method_level_axes.py (SSoT) / コスト軸: method_level_cost.py / 詳細な定義: ../references/rubric.md

重みと段階の数値は出力 (help / list / verbose / json / Next) に出さない。
呼び出し元 AI がそれらを知ると、点数の大きい軸・値を甘く見積もる方向に判断が偏るため。
値はキーワードで受け取る。選択肢の中間に当たる検証は "unit..integration" のように 2 つのキーワードで指定する。
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from method_level_axes import (AI_OBSERVERS, AXES, AXIS_BY_KEY, COST_AXES, LEVELS, Axis, Choice, is_number,
                               normalize_keyword)
from method_level_cost import (Cost, cost_help_lines, cost_json, cost_line, cost_list_lines, cost_verbose_lines,
                               resolve_costs)

EXIT_USAGE = 2
INTERPOLATION_SEP = ".."
REQUIRED = tuple(axis.key for axis in AXES if axis.key != "ai-context")


class UsageError(Exception):
    pass


@dataclass(frozen=True)
class Value:
    rank: float
    label: str
    desc: str
    endpoints: Tuple[Choice, ...]

    @property
    def interpolated(self) -> bool:
        return len(self.endpoints) == 2


@dataclass(frozen=True)
class Result:
    label: str
    score: int
    max_score: int
    level: str
    values: Dict[str, Value]
    shown_axes: Tuple[str, ...]
    next_steps: List[str]
    notes: List[str]
    warnings: List[str]
    costs: Optional[Dict[str, Cost]]


def resolve(axis: Axis, raw: str) -> Value:
    parts = [part.strip() for part in normalize_keyword(raw).split(INTERPOLATION_SEP)]
    if len(parts) > 2:
        raise UsageError(f"--{axis.key}: 中間指定はキーワード 2 つまで: '{raw}'")
    choices = []
    for part in parts:
        if is_number(part):
            raise UsageError(f"--{axis.key}: 数値は指定できない ('{raw}')。キーワードで指定する: {axis.keywords()}")
        choice = axis.find(part)
        if choice is None:
            raise UsageError(f"--{axis.key}: 不明な値 '{part}'。選択肢: {axis.keywords()}")
        choices.append(choice)
    if len(choices) == 1:
        choice = choices[0]
        return Value(axis.rank(choice), choice.name, choice.desc, (choice,))
    first, second = choices
    if first == second:
        raise UsageError(f"--{axis.key}: 中間指定には異なる 2 つのキーワードを使う: '{raw}'")
    return Value(
        (axis.rank(first) + axis.rank(second)) / 2,
        f"{first.name}{INTERPOLATION_SEP}{second.name}",
        f"{first.desc} と {second.desc} の中間",
        (first, second),
    )


def _missing_message(missing: List[str]) -> str:
    lines = [
        f"必須の軸が指定されていない: {' '.join(missing)}",
        "各軸の値を判断してキーワードで明示的に指定すること (デフォルト値は無い)。左ほど強い。",
    ]
    lines += [f"  --{key}: {AXIS_BY_KEY[key].keywords()}" for key in missing]
    lines.append("選択肢の中間に当たる場合は 'unit..integration' のように 2 つのキーワードで指定できる。")
    return "\n".join(lines)


def resolve_all(raw: Dict[str, Optional[str]]) -> Tuple[Dict[str, Value], bool, Optional[Dict[str, Cost]]]:
    """全軸を解決する。戻り値は (値, observer が AI か, cost)。不足・不正は UsageError。"""
    missing = [key for key in REQUIRED if not (raw[key] or "").strip()]
    errors: List[str] = []
    values: Dict[str, Value] = {}
    for key in REQUIRED:
        if key in missing:
            continue
        try:
            values[key] = resolve(AXIS_BY_KEY[key], raw[key] or "")
        except UsageError as e:
            errors.append(str(e))

    is_ai = False
    if "observer" in values:
        is_ai = any(choice.name in AI_OBSERVERS for choice in values["observer"].endpoints)
        ai_context = AXIS_BY_KEY["ai-context"]
        raw_context = (raw["ai-context"] or "").strip()
        if is_ai and not raw_context:
            missing.append("ai-context")
        elif is_ai:
            try:
                values["ai-context"] = resolve(ai_context, raw_context)
            except UsageError as e:
                errors.append(str(e))
        else:
            none = ai_context.find("none")
            if raw_context and raw_context.lower() not in (none.name,) + none.aliases:
                errors.append(f"--ai-context は observer が AI の時のみ指定できる (observer={values['observer'].label})")
            values["ai-context"] = Value(0, none.name, none.desc, (none,))

    costs, cost_errors = resolve_costs(raw)
    errors += cost_errors

    if missing or errors:
        message = "\n".join(([_missing_message(missing)] if missing else []) + errors)
        raise UsageError(message)
    return values, is_ai, costs


def _warnings(values: Dict[str, Value]) -> List[str]:
    rank = {key: value.rank for key, value in values.items()}
    result = []
    others = ("env", "execution", "evidence", "oracle", "observer", "coverage", "repeatability")
    if rank["scope"] == 0 and any(rank[key] > 0 for key in others):
        result.append("scope=none (検証なし) なのに他の軸が none 以外")
    if rank["scope"] >= 2 and rank["execution"] == 0:
        result.append("scope が実行を伴うのに execution=none")
    if rank["scope"] >= 2 and rank["env"] == 0:
        result.append("scope が実行を伴うのに env=none")
    if rank["scope"] == 1 and rank["execution"] >= 2:
        result.append("scope=static なのに execution が自動実行扱い (静的解析を CI で回しているなら問題なし)")
    if rank["oracle"] > 0 and rank["observer"] == 0:
        result.append("oracle があるのに observer=none (誰が判定したか?)")
    if values["observer"].label == "machine" and rank["oracle"] < AXIS_BY_KEY["oracle"].rank(AXIS_BY_KEY["oracle"].find("assertion")):
        result.append("observer=machine だが oracle が assertion より弱い (機械判定なら通常 assertion 以上)")
    return result


def evaluate(raw: Dict[str, Optional[str]], label: str = "") -> Result:
    values, is_ai, costs = resolve_all(raw)
    shown = tuple(axis.key for axis in AXES if axis.key != "ai-context" or is_ai)
    score_float = sum(values[axis.key].rank * axis.weight for axis in AXES)
    score = math.floor(score_float + 0.5)
    max_score = sum(axis.max_rank * axis.weight for axis in AXES if axis.key != "ai-context")
    level = next((name for name, threshold in LEVELS if score >= threshold), "-")

    # 伸ばせる点数の大きい順に 3 軸。点数そのものは出さない (重みを推測させないため)
    gaps = []
    for key in shown:
        axis = AXIS_BY_KEY[key]
        rank = values[key].rank
        if rank < axis.max_rank:
            gaps.append(((axis.max_rank - rank) * axis.weight, f"{key}→{axis.choice_at(math.floor(rank) + 1).name}"))
    gaps.sort(key=lambda gap: -gap[0])

    notes = [f"{key}={values[key].label} は選択肢の中間として扱った" for key in shown if values[key].interpolated]
    next_steps = [g[1] for g in gaps[:3]]
    return Result(label, score, max_score, level, values, shown, next_steps, notes, _warnings(values), costs)


LEVEL_SCALE = " > ".join([name for name, _ in LEVELS] + ["-"])


def _percent(result: Result) -> int:
    return math.floor(result.score * 100 / result.max_score + 0.5)


def render_text(result: Result, verbose: bool) -> str:
    lines = []
    if result.label:
        lines.append(f"Method: {result.label}")
    # 何点満点か・Level が全体のどこかを出力だけで読み取れるようにする (Level の閾値は出さない)
    lines.append(f"Score: {result.score} out of {result.max_score} ({_percent(result)}%)")
    lines.append(f"Level: {result.level} ({LEVEL_SCALE}), Scope: {result.values['scope'].label}")
    if verbose:
        for key in result.shown_axes:
            value = result.values[key]
            lines.append(f"  {AXIS_BY_KEY[key].title:<19} {value.label:<22} {value.desc}")
        if result.costs:
            lines += cost_verbose_lines(result.costs)
    else:
        lines.append("Axes: " + " ".join(f"{key}={result.values[key].label}" for key in result.shown_axes))
        if result.costs:
            lines.append(cost_line(result.costs))
    if result.next_steps:
        lines.append("Next: " + ", ".join(result.next_steps))
    lines += [f"Note: {note}" for note in result.notes]
    lines += [f"Warn: {warning}" for warning in result.warnings]
    return "\n".join(lines)


def render_json(result: Result) -> str:
    return json.dumps({
        "label": result.label,
        "score": result.score,
        "max": result.max_score,
        "percent": _percent(result),
        "level": result.level,
        "axes": {key: {"value": result.values[key].label, "interpolated": result.values[key].interpolated}
                 for key in result.shown_axes},
        "next": result.next_steps,
        "notes": result.notes,
        "warnings": result.warnings,
        "cost": cost_json(result.costs),
    }, ensure_ascii=False, indent=2)


def _choices_help() -> str:
    lines = ["各軸の選択肢 (上ほど強い):"]
    for axis in AXES:
        lines.append(f"  --{axis.key} ({axis.title})")
        for choice in axis.choices:
            alias = f"  (alias: {', '.join(choice.aliases)})" if choice.aliases else ""
            lines.append(f"      {choice.name:<12} {choice.desc}{alias}")
    lines += [
        "",
        "中間指定: 選択肢のどれにも当てはまらず 2 つの間に当たる場合は '--scope=unit..integration' のように書く。",
        "--ai-context は --observer が AI (ai-high / ai-standard / ai-light) の時のみ必須。",
    ]
    lines += cost_help_lines()
    return "\n".join(lines)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="calc-check-method-level.py",
        description="検証方法の強さ (Verification Score) を算出する。各軸の値は事実に基づいてキーワードで指定する。"
                    "デフォルト値は無く、未指定の軸があると exit 2。",
        epilog=_choices_help(),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        allow_abbrev=False,
    )
    parser.add_argument("--scope", metavar="KEYWORD")
    parser.add_argument("--env", "--environment", dest="env", metavar="KEYWORD")
    parser.add_argument("--execution", "--automation", "--continuous-level", dest="execution", metavar="KEYWORD")
    for key in ("evidence", "oracle", "observer", "coverage", "repeatability"):
        parser.add_argument(f"--{key}", metavar="KEYWORD")
    parser.add_argument("--ai-context", "--ai-context-quality", dest="ai_context", metavar="KEYWORD")
    parser.add_argument("--build-cost", dest="build_cost", metavar="KEYWORD", help="構築コスト (任意。--run-cost とセット)")
    parser.add_argument("--run-cost", dest="run_cost", metavar="KEYWORD", help="1 回の実行コスト (任意。--build-cost とセット)")
    parser.add_argument("--label", default="", help="検証方法の名前 (任意。出力に表示するだけ)")
    parser.add_argument("--format", choices=("text", "json"), default="text")
    parser.add_argument("-v", "--verbose", action="store_true", help="軸ごとの値と意味を表で表示")
    parser.add_argument("--list", action="store_true", help="各軸の選択肢 (キーワード) だけを表示")
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)
    if args.list:
        print("\n".join([f"--{axis.key}: {axis.keywords()}" for axis in AXES] + cost_list_lines()))
        return 0
    raw = {axis.key: getattr(args, axis.key.replace("-", "_")) for axis in AXES + COST_AXES}
    try:
        result = evaluate(raw, args.label)
    except UsageError as e:
        print(f"error: {e}", file=sys.stderr)
        return EXIT_USAGE
    print(render_json(result) if args.format == "json" else render_text(result, args.verbose))
    return 0


if __name__ == "__main__":
    sys.exit(main())
