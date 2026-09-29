"""calc-check-method-level.py のテスト。実行: python3 -m unittest discover -s <skill-dir>/scripts"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import unittest
from pathlib import Path

from method_level_axes import AXES

SCRIPTS = Path(__file__).resolve().parent
CALC = SCRIPTS / "calc-check-method-level.py"
SKILL_MD = SCRIPTS.parent / "SKILL.md"
RUBRIC_MD = SCRIPTS.parent / "references" / "rubric.md"

BASE_UNIT = ["--scope=unit", "--env=mock", "--execution=ci", "--evidence=structured", "--oracle=assertion",
             "--observer=machine", "--coverage=broad", "--repeatability=full"]
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
        self.assertEqual((data["score"], data["max"], data["level"]), (197, 197, "S"))

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
        self.assertEqual(out[1], "Score: 122/197 Level B (Scope: unit)")
        self.assertTrue(out[2].startswith("Axes: scope=unit env=mock"))
        self.assertEqual(out[3], "Next: scope→integration, env→test, evidence→render-tree")

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
        }
        for name, out in outputs.items():
            self.assertIsNone(leak.search(out), f"{name}: {out}")
        json_axes = json.loads(outputs["json"])["axes"]
        self.assertEqual(json_axes["scope"], {"value": "unit", "interpolated": False})


class DocsTest(unittest.TestCase):
    def test_SKILL_md_の早見表がスクリプトの選択肢と同じ順番(self):
        text = SKILL_MD.read_text(encoding="utf-8")
        for axis in AXES:
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
