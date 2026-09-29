"""検証コスト (build-cost / run-cost) の解決と表示。

コストはスコアに含めない。混ぜると「強いけど高い検証」と「弱い検証」を区別できなくなるため。
値はカテゴリ (キーワード) で受け取る。見積もりに幅がある場合は "minutes..hours" のような範囲で受け取り、
平均は取らずに安い順へ正規化した範囲のまま扱う。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from method_level_axes import COST_AXES, Axis, Choice, is_number, normalize_keyword

RANGE_SEP = ".."


@dataclass(frozen=True)
class Cost:
    choices: Tuple[Choice, ...]  # 安い順。単一値なら 1 要素、範囲なら 2 要素

    @property
    def label(self) -> str:
        return RANGE_SEP.join(choice.name for choice in self.choices)

    @property
    def desc(self) -> str:
        return " 〜 ".join(choice.desc for choice in self.choices)


def resolve_cost(axis: Axis, raw: str) -> Cost:
    """キーワード 1 つ、または "a..b" の範囲を解決する。不正なら ValueError。"""
    parts = [part.strip() for part in normalize_keyword(raw).split(RANGE_SEP)]
    if len(parts) > 2:
        raise ValueError(f"--{axis.key}: 範囲はキーワード 2 つまで: '{raw}'")
    choices = []
    for part in parts:
        if is_number(part):
            raise ValueError(f"--{axis.key}: 数値は指定できない ('{raw}')。キーワードで指定する: {axis.keywords()}")
        choice = axis.find(part)
        if choice is None:
            raise ValueError(f"--{axis.key}: 不明な値 '{part}'。選択肢: {axis.keywords()}")
        if choice not in choices:
            choices.append(choice)
    return Cost(tuple(sorted(choices, key=axis.choices.index)))


def resolve_costs(raw: Dict[str, Optional[str]]) -> Tuple[Optional[Dict[str, Cost]], List[str]]:
    """cost 軸を解決する。戻り値は (cost または未指定なら None, エラー)。2 軸はセットで指定させる。"""
    given = {axis.key: raw.get(axis.key) or "" for axis in COST_AXES if (raw.get(axis.key) or "").strip()}
    if not given:
        return None, []
    errors = [
        f"--build-cost と --run-cost はセットで指定する。--{axis.key} が無い: {axis.keywords()}"
        for axis in COST_AXES if axis.key not in given
    ]
    costs: Dict[str, Cost] = {}
    for axis in COST_AXES:
        if axis.key in given:
            try:
                costs[axis.key] = resolve_cost(axis, given[axis.key])
            except ValueError as e:
                errors.append(str(e))
    return (None if errors else costs), errors


def cost_line(costs: Dict[str, Cost]) -> str:
    return f"Cost: build={costs['build-cost'].label} run={costs['run-cost'].label}"


def cost_verbose_lines(costs: Dict[str, Cost]) -> List[str]:
    lines = ["  -- Cost (スコアに含まない) --"]
    for axis in COST_AXES:
        cost = costs[axis.key]
        lines.append(f"  {axis.title:<19} {cost.label:<22} {cost.desc}")
    return lines


def cost_json(costs: Optional[Dict[str, Cost]]) -> Optional[Dict[str, str]]:
    if costs is None:
        return None
    return {"build": costs["build-cost"].label, "run": costs["run-cost"].label}


def cost_list_lines() -> List[str]:
    return [f"--{axis.key} (任意。左ほど安い): {axis.keywords()}" for axis in COST_AXES]


def cost_help_lines() -> List[str]:
    lines = ["", "コスト軸 (任意。スコアには含まない。上ほど安い。2 つはセットで指定する):"]
    for axis in COST_AXES:
        lines.append(f"  --{axis.key} ({axis.title})")
        for choice in axis.choices:
            alias = f"  (alias: {', '.join(choice.aliases)})" if choice.aliases else ""
            lines.append(f"      {choice.name:<16} {choice.desc}{alias}")
    lines.append("見積もりに幅がある場合は '--run-cost=minutes..tens-of-minutes' のように範囲で指定する。")
    return lines
