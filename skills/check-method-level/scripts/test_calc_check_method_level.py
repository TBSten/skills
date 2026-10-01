"""calc-check-method-level.py のテスト。実行: python3 -m unittest discover -s <skill-dir>/scripts"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import unittest
from pathlib import Path

from method_level_axes import AXES, COST_AXES

SCRIPTS = Path(__file__).resolve().parent
CALC = SCRIPTS / "calc-check-method-level.py"
SKILL_MD = SCRIPTS.parent / "SKILL.md"
RUBRIC_MD = SCRIPTS.parent / "references" / "rubric.md"

BASE_UNIT = ["--scope=unit", "--env=mock", "--execution=ci", "--evidence=structured", "--oracle=assertion",
             "--observer=machine", "--coverage=broad", "--repeatability=full"]
COST = ["--build-cost=add-case", "--run-cost=minutes"]
ALL_NONE = ["--scope=none", "--env=none", "--execution=none", "--evidence=source", "--oracle=none",
            "--observer=none", "--coverage=unknown", "--repeatability=none"]


def run(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, str(CALC), *args], capture_output=True, text=True)


def score_of(*args: str) -> int:
    proc = run(*args, "--format=json")
    assert proc.returncode == 0, proc.stderr
    return json.loads(proc.stdout)["score"]


class ScoreTest(unittest.TestCase):
    def test_unit_test_と_assertion_の組み合わせ(self):
        # 60 + 4 + 6 + 9 + 16 + 12 + 9 + 6
        self.assertEqual(score_of(*BASE_UNIT), 122)

    def test_AI_が_screenshot_と仕様を比較した_E2E(self):
        # 100 + 8 + 6 + 15 + 12 + 6 + 6 + 4 + 3
        self.assertEqual(score_of("--scope=e2e", "--env=staging", "--execution=on-ci", "--evidence=screenshot",
                                  "--oracle=spec", "--observer=ai-high", "--ai-context=spec",
                                  "--coverage=multiple", "--repeatability=mostly"), 160)

    def test_全軸最強なら満点(self):
        proc = run(*[f"--{axis.key}={axis.choices[0].name}" for axis in AXES if axis.key != "ai-context"],
                   "--format=json")
        data = json.loads(proc.stdout)
        self.assertEqual((data["score"], data["max"], data["percent"], data["level"]), (197, 197, 100, "S"))

    def test_未検証は_0_点(self):
        proc = run(*ALL_NONE, "--format=json")
        self.assertEqual((json.loads(proc.stdout)["score"], json.loads(proc.stdout)["level"]), (0, "-"))

    def test_エイリアス_大文字_空白区切りを受け付ける(self):
        self.assertEqual(score_of("--scope", "unit", "--env", "fake", "--execution=CI", "--evidence=json",
                                  "--oracle=golden", "--observer=deterministic", "--coverage=wide",
                                  "--repeatability=fixed"), 122)

    def test_中間指定はキーワード_2_つの中間として計算する(self):
        # scope: unit(60) と integration(80) の中間 = 70
        args = [a for a in BASE_UNIT if not a.startswith("--scope")]
        self.assertEqual(score_of("--scope=unit..integration", *args), 132)
        self.assertEqual(score_of("--scope=integration..unit", *args), 132)

    def test_中間指定の小数は四捨五入する(self):
        # evidence: structured(9) と render-tree(12) の中間 = 10.5 → 122 - 9 + 10.5 = 123.5 → 124
        args = [a for a in BASE_UNIT if not a.startswith("--evidence")]
        self.assertEqual(score_of("--evidence=structured..render-tree", *args), 124)


class ValidationTest(unittest.TestCase):
    def assert_usage_error(self, proc: subprocess.CompletedProcess, fragment: str) -> None:
        self.assertEqual(proc.returncode, 2, proc.stdout)
        self.assertIn(fragment, proc.stderr)

    def test_引数なしは必須エラー(self):
        self.assert_usage_error(run(), "必須の軸が指定されていない")

    def test_未指定の軸だけを選択肢つきで示す(self):
        proc = run("--scope=e2e", "--env=staging", "--execution=ci")
        self.assert_usage_error(proc, "evidence oracle observer coverage repeatability")
        self.assertIn("--evidence: perceived > media", proc.stderr)
        self.assertNotIn("--scope:", proc.stderr)

    def test_空文字は未指定扱い(self):
        self.assert_usage_error(run("--scope=", *BASE_UNIT[1:]), "scope")

    def test_observer_が_AI_なら_ai_context_が必須(self):
        args = [a for a in BASE_UNIT if not a.startswith("--observer")]
        self.assert_usage_error(run(*args, "--observer=ai-standard"), "ai-context")

    def test_AI_が片側に入る中間指定でも_ai_context_が必須(self):
        args = [a for a in BASE_UNIT if not a.startswith("--observer")]
        self.assert_usage_error(run(*args, "--observer=human..ai-high"), "ai-context")

    def test_数値は受け付けない(self):
        self.assert_usage_error(run("--scope=5", *BASE_UNIT[1:]), "数値は指定できない")
        self.assert_usage_error(run("--scope=unit..4", *BASE_UNIT[1:]), "数値は指定できない")

    def test_未知のキーワード(self):
        self.assert_usage_error(run(*BASE_UNIT[:1], "--env=moon", *BASE_UNIT[2:]), "不明な値 'moon'")

    def test_同じキーワード同士や_3_つの中間指定は不可(self):
        self.assert_usage_error(run("--scope=unit..unit", *BASE_UNIT[1:]), "異なる 2 つ")
        self.assert_usage_error(run("--scope=unit..integration..e2e", *BASE_UNIT[1:]), "2 つまで")

    def test_未知の引数(self):
        self.assertEqual(run(*BASE_UNIT, "--foo=bar").returncode, 2)

    def test_AI_以外の_observer_に_ai_context(self):
        self.assert_usage_error(run(*BASE_UNIT, "--ai-context=full"), "observer が AI の時のみ")
        self.assertEqual(run(*BASE_UNIT, "--ai-context=none").returncode, 0)


class OutputTest(unittest.TestCase):
    def test_既定の出力はコンパクト(self):
        out = run(*BASE_UNIT, "--label=ViewModel").stdout.splitlines()
        self.assertEqual(out[0], "Method: ViewModel")
        self.assertEqual(out[1], "Score: 122 out of 197 (62%)")
        self.assertEqual(out[2], "Level: B (S > A > B > C > D > E > -), Scope: unit")
        self.assertTrue(out[3].startswith("Axes: scope=unit env=mock"))
        self.assertEqual(out[4], "Next: scope→integration, env→test, evidence→render-tree")

    def test_中間指定は_Note_に残り_Next_は上側の次の段階を示す(self):
        out = run("--scope=unit..integration", *BASE_UNIT[1:]).stdout
        self.assertIn("Note: scope=unit..integration は選択肢の中間として扱った", out)
        self.assertIn("scope→integration", out)

    def test_整合性の警告(self):
        args = [a for a in BASE_UNIT if not a.startswith("--execution")]
        self.assertIn("Warn: scope が実行を伴うのに execution=none", run(*args, "--execution=none").stdout)

    def test_verbose_は値の意味を表示(self):
        self.assertIn("Layout Tree / Accessibility Tree", run(*BASE_UNIT, "-v").stdout)

    def test_重みや段階の数値を出力に出さない(self):
        leak = re.compile(r"×|(^|[^A-Za-z])[Ww]eight([^A-Za-z]|$)|points|Points|\+\d+ [a-z]|\(\d+/\d+\)|\b\d = ?[a-z]")
        outputs = {
            "help": run("--help").stdout,
            "list": run("--list").stdout,
            "text": run(*BASE_UNIT).stdout,
            "verbose": run(*BASE_UNIT, "-v").stdout,
            "json": run(*BASE_UNIT, "--format=json").stdout,
            "missing": run().stderr,
            "cost_text": run(*BASE_UNIT, *COST).stdout,
            "cost_verbose": run(*BASE_UNIT, *COST, "-v").stdout,
            "cost_json": run(*BASE_UNIT, *COST, "--format=json").stdout,
        }
        for name, out in outputs.items():
            self.assertIsNone(leak.search(out), f"{name}: {out}")
        json_axes = json.loads(outputs["json"])["axes"]
        self.assertEqual(json_axes["scope"], {"value": "unit", "interpolated": False})


class CostTest(unittest.TestCase):
    def cost_of(self, *args: str) -> dict:
        proc = run(*BASE_UNIT, *args, "--format=json")
        self.assertEqual(proc.returncode, 0, proc.stderr)
        return json.loads(proc.stdout)["cost"]

    def test_両方指定すると_Axes_の直後に_Cost_行を出す(self):
        out = run(*BASE_UNIT, *COST).stdout.splitlines()
        self.assertTrue(out[2].startswith("Axes: "))
        self.assertEqual(out[3], "Cost: build=add-case run=minutes")

    def test_未指定なら_Cost_行を出さず_JSON_は_null(self):
        self.assertNotIn("Cost:", run(*BASE_UNIT).stdout)
        self.assertIsNone(json.loads(run(*BASE_UNIT, "--format=json").stdout)["cost"])

    def test_片方だけの指定はエラーで足りない方の選択肢を示す(self):
        proc = run(*BASE_UNIT, "--build-cost=add-case")
        self.assertEqual(proc.returncode, 2)
        self.assertIn("--run-cost が無い: seconds > minutes > tens-of-minutes > hours > days", proc.stderr)
        self.assertEqual(run(*BASE_UNIT, "--run-cost=minutes").returncode, 2)

    def test_cost_未指定でも必須軸不足のエラーは従来どおり(self):
        proc = run("--scope=unit")
        self.assertIn("必須の軸が指定されていない: env execution", proc.stderr)
        self.assertNotIn("cost", proc.stderr)

    def test_数値と未知の値は受け付けない(self):
        self.assertIn("数値は指定できない", run(*BASE_UNIT, "--build-cost=3", "--run-cost=minutes").stderr)
        self.assertIn("不明な値 'forever'", run(*BASE_UNIT, "--build-cost=existing", "--run-cost=forever").stderr)

    def test_エイリアスと大文字を受け付ける(self):
        self.assertEqual(self.cost_of("--build-cost=Harness", "--run-cost=min"), {"build": "new-harness", "run": "minutes"})

    def test_範囲指定を受け付け安い順に正規化する(self):
        self.assertEqual(self.cost_of("--build-cost=existing", "--run-cost=hours..minutes"),
                         {"build": "existing", "run": "minutes..hours"})
        self.assertEqual(self.cost_of("--build-cost=add-case..new-environment", "--run-cost=seconds..hours"),
                         {"build": "add-case..new-environment", "run": "seconds..hours"})

    def test_同じキーワードの範囲は単一値として扱う(self):
        self.assertEqual(self.cost_of("--build-cost=existing", "--run-cost=minutes..min")["run"], "minutes")

    def test_範囲に_3_つ以上のキーワードはエラー(self):
        proc = run(*BASE_UNIT, "--build-cost=existing", "--run-cost=seconds..minutes..hours")
        self.assertEqual(proc.returncode, 2)
        self.assertIn("範囲はキーワード 2 つまで", proc.stderr)

    def test_範囲指定では_Note_を出さない(self):
        self.assertNotIn("Note:", run(*BASE_UNIT, "--build-cost=existing..external", "--run-cost=seconds..days").stdout)

    def test_cost_はスコアと_Level_と_Next_に影響しない(self):
        base = json.loads(run(*BASE_UNIT, "--format=json").stdout)
        for build, run_cost in (("existing", "seconds"), ("external", "days"), ("existing..external", "seconds..days")):
            data = json.loads(run(*BASE_UNIT, f"--build-cost={build}", f"--run-cost={run_cost}", "--format=json").stdout)
            self.assertEqual((data["score"], data["level"], data["next"]), (base["score"], base["level"], base["next"]))

    def test_verbose_はスコア軸と区別して意味を表示(self):
        out = run(*BASE_UNIT, *COST, "-v").stdout
        self.assertIn("-- Cost (スコアに含まない) --", out)
        self.assertIn("1 回の実行が 1〜10 分", out)

    def test_list_と_help_に任意であることを示す(self):
        self.assertIn("--run-cost (任意。左ほど安い): seconds > minutes", run("--list").stdout)
        self.assertIn("コスト軸 (任意。スコアには含まない", run("--help").stdout)


class DocsTest(unittest.TestCase):
    def test_SKILL_md_の早見表がスクリプトの選択肢と同じ順番(self):
        text = SKILL_MD.read_text(encoding="utf-8")
        for axis in AXES + COST_AXES:
            row = re.search(rf"^\| {re.escape(axis.key)} \| (.+) \|$", text, re.M)
            self.assertIsNotNone(row, axis.key)
            names = [re.sub(r"\s*\(.*\)$", "", part).strip() for part in row.group(1).split(" > ")]
            self.assertEqual(names, [choice.name for choice in axis.choices], axis.key)

    def test_AI_が読むドキュメントに重みや段階の数値を書かない(self):
        for doc in (SKILL_MD, RUBRIC_MD):
            text = doc.read_text(encoding="utf-8")
            self.assertNotRegex(text, r"×\s*\d|重み|\| \d+ \|", doc.name)


if __name__ == "__main__":
    unittest.main()
