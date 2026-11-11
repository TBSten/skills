#!/usr/bin/env python3
"""decisions.py のテスト。python3 -m unittest discover -s <この skill>/scripts -p 'test_*.py'（review.mjs との id の一致は node があれば確かめる）"""
import contextlib
import json
import shutil
import subprocess
import io
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import decisions as D  # noqa: E402

REVIEW = textwrap.dedent("""\
    ---
    title: テスト
    ---

    ## [p2] 指示語「これらは」
    - file: ja/get-started/comparison-with-other-tools.mdx
    - line: 16
    - quote: これらは **katachi と排他的とは限りません**
    - status: resolved
    - note: 直さない: 前の文で並べているので通じる

    - 破っている項目: D3-3（「これら」ではなく指すものを書く）
    - 直し方の案: 「Konsist や ArchUnit などは、…」

    ## [p1] Role に summary が無い（要確認）
    - file: ja/get-started/motivation.mdx
    - line: 66-74
    - quote: .role {
    - status: resolved
    - note: 直さない

    本文。

    ```md
    ## コードフェンスの中の見出しは見出しにしない
    ```

    ## [p1] Role に summary が無い（要確認）
    - file: ja/get-started/motivation.mdx
    - line: 307-327
    - quote: .role {
    - status: resolved
    - note: Feedback に送った: 261110-009

    ## [p3] Role 名が PascalCase でない
    - file: ja/get-started/motivation.mdx
    - line: 70
    - quote: .role {

    本文。
    """)


def run(*argv):
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = D.main(list(argv))
    return code, out.getvalue()


class DecisionsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.dir = self.root / "decisions"
        self.dir.mkdir()
        (self.dir / "README.md").write_text("# 読まない\n\n## 見出しがあっても判断ではない\n", encoding="utf-8")
        self.review = self.root / "prerelease-test.md"
        self.review.write_text(REVIEW, encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def test_parse_skips_headings_in_fences(self):
        entries, _ = D.parse(REVIEW, "x", D.REVIEW_KEYS)
        self.assertEqual([e.title for e in entries], ["指示語「これらは」", "Role に summary が無い（要確認）", "Role に summary が無い（要確認）", "Role 名が PascalCase でない"])
        self.assertEqual(entries[0].priority, 2)
        self.assertEqual(entries[0].file, "ja/get-started/comparison-with-other-tools.mdx")
        self.assertIn("```md", entries[1].body)

    def test_impact_and_recommend_are_meta(self):
        # `- impact:` `- recommend:` の後ろに status・note を書き足しても、メタとして読む
        text = "## 指摘\n- file: ja/a.mdx\n- impact: あり\n- recommend: 直さない 7\n- status: resolved\n- note: 直さない\n\n本文\n"
        entries, _ = D.parse(text, "x", D.REVIEW_KEYS)
        self.assertEqual(entries[0].meta.get("status"), "resolved")
        self.assertEqual(entries[0].meta.get("note"), "直さない")
        self.assertEqual(entries[0].body, "本文")

    def test_review_ids_match_the_review_cli(self):
        # 参照実装の cli が出した id（同じ規則なので値も同じ）
        entries, _ = D.parse(REVIEW, "prerelease-261109-01-step11", D.REVIEW_KEYS)
        ids = D.review_ids(entries, "prerelease-261109-01-step11")
        self.assertEqual(ids[0], "prerelease-261109-01-step11/1wgoff5")
        self.assertEqual(ids[1], "prerelease-261109-01-step11/18umevq")
        self.assertEqual(ids[2], "prerelease-261109-01-step11/18umevq-2")

    @unittest.skipUnless(shutil.which("node"), "node が無い")
    def test_review_ids_match_review_mjs(self):
        # 同じ skill の review.mjs と id が一致する（record-review の REMOVE <id> をそのまま review.mjs remove に渡すため）
        mjs = Path(__file__).resolve().parent / "review.mjs"
        code = "import { parseReview } from %s; let t=''; process.stdin.on('data', d => t += d).on('end', () => console.log(JSON.stringify(parseReview('s', t).items.map(i => i.id))))" % json.dumps(mjs.as_uri())
        out = subprocess.run(["node", "--input-type=module", "-e", code], input=REVIEW, capture_output=True, text=True, check=True).stdout
        self.assertEqual(json.loads(out), D.review_ids(D.parse(REVIEW, "s", D.REVIEW_KEYS)[0], "s"))

    def test_record_review_copies_only_wont_fix_and_is_idempotent(self):
        code, out = run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "2026-11-10 t #1")
        self.assertEqual(code, 0)
        self.assertIn("写した 2 件", out)
        self.assertEqual(out.count("REMOVE "), 2)
        # 後ろから消す順（-2 が付いた id を先に消さないと、先頭を消したときに id がずれる）
        removes = [l.split()[1] for l in out.splitlines() if l.startswith("REMOVE ")]
        self.assertEqual(removes, list(reversed(D.review_ids(D.parse(REVIEW, "prerelease-test", D.REVIEW_KEYS)[0], "prerelease-test")[:2])))
        text = (self.dir / "decisions.md").read_text(encoding="utf-8")
        self.assertIn("## 直さない: 指示語「これらは」", text)
        self.assertIn("人の一言: 前の文で並べているので通じる", text)
        self.assertIn("- rule: D3-3", text)
        self.assertNotIn("261110-009", text)  # 「直してもらう」は判断ではない
        code, out = run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "2026-11-10 t #1")
        self.assertIn("写した 0 件 / 写してあった 2 件", out)
        self.assertEqual((self.dir / "decisions.md").read_text(encoding="utf-8"), text)

    def test_record_review_also_copies_added_choices_with_a_decision(self):
        # 人が足した選択肢（指摘のファイルの隣の _actions.md）。decision の付いたものだけを写す
        (self.root / "_actions.md").write_text(
            "# Review の選択肢\n\n## 後で\n- agent: v0.5 に積む\n- decision: 後で\n\n## 誤検知\n- decision: 直さない\n\n## 聞きたい\n- agent: ai-note で答えて reopen\n",
            encoding="utf-8",
        )
        extra = textwrap.dedent("""\

            ## [p2] 後で直す指摘
            - file: ja/get-started/faq.mdx
            - quote: 後で
            - status: resolved
            - note: 後で: v0.5 の docs 整理で

            ## [p2] 誤検知の指摘
            - file: ja/get-started/faq.mdx
            - quote: 誤検知
            - status: resolved
            - note: 誤検知

            ## [p2] 聞きたい指摘
            - file: ja/get-started/faq.mdx
            - status: resolved
            - note: 聞きたい: なぜ？
            """)
        self.review.write_text(REVIEW + extra, encoding="utf-8")
        code, out = run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "2026-11-10 t #2")
        self.assertEqual(code, 0)
        self.assertIn("写した 4 件", out)
        text = (self.dir / "decisions.md").read_text(encoding="utf-8")
        self.assertIn("## 後で: 後で直す指摘\n- decision: 後で", text)
        self.assertIn("- revisit: v0.5 の docs 整理で", text)
        self.assertIn("## 直さない: 誤検知の指摘\n- decision: 直さない", text)
        self.assertIn("（選択肢「誤検知」）", text)
        self.assertNotIn("聞きたい指摘", text)  # decision の無い選択肢は判断ではない
        # 写した判断は形の検査を通る（「後で」の revisit も入っている）
        self.assertEqual([e.errors for e in D.load_decisions(self.dir) if e.errors], [])

    def test_actions_popup_does_not_show_are_not_read(self):
        # popup に出ない選択肢（初めからのボタンと同じ名前・同じ名前の 2 つめ・9 つめから）は読まない。
        # 「## 直さない」に decision: 後で と書かれても、直さない の判断を上書きしない
        heads = ["直さない", "後で", "後で", "[p3] 印", "名前: 区切り"] + [f"選択肢{i}" for i in range(1, 9)]
        (self.root / "_actions.md").write_text(
            "# 選択肢\n\n" + "".join(f"## {h}\n- decision: 後で\n\n" for h in heads), encoding="utf-8"
        )
        got = D.load_actions(self.review)
        self.assertEqual(list(got), ["後で"] + [f"選択肢{i}" for i in range(1, 8)])
        self.assertNotIn("直さない", got)

    def test_note_with_reason_in_parentheses_or_after_a_space(self):
        # 手で付けた note（「直さない（理由）」「直さない 理由」）も理由付きの「直さない」として受ける（参照実装で 10 件漏れた形）
        self.assertEqual(D.note_choice("直さない（前の文で通じる）", ["直さない"]), ("直さない", "前の文で通じる"))
        self.assertEqual(D.note_choice("直さない (ok)", ["直さない"]), ("直さない", "ok"))
        self.assertEqual(D.note_choice("直さない 誤検知", ["直さない"]), ("直さない", "誤検知"))
        self.assertEqual(D.note_choice("直さない：誤検知", ["直さない"]), ("直さない", "誤検知"))
        self.assertEqual(D.note_choice("直さない", ["直さない"]), ("直さない", ""))
        self.assertEqual(D.note_choice("直さないでほしい", ["直さない"]), (None, ""))
        self.assertEqual(D.note_choice("PR #65 待ち 消えたら remove", ["PR", "PR #65 待ち"]), ("PR #65 待ち", "消えたら remove"))
        self.review.write_text(REVIEW.replace("- note: 直さない: 前の文で並べているので通じる", "- note: 直さない（前の文で並べているので通じる）"), encoding="utf-8")
        code, out = run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "2026-11-10 t #2")
        self.assertIn("写した 2 件", out)
        self.assertIn("人の一言: 前の文で並べているので通じる", (self.dir / "decisions.md").read_text(encoding="utf-8"))

    def test_review_ids_honor_pinned_id(self):
        # 解決済みにした指摘は `- id:` で固定され、前の同じ見出しを消しても id がずれない（review.mjs と同じ）
        entries, _ = D.parse(REVIEW, "s", D.REVIEW_KEYS)
        base = D.review_ids(entries, "s")[1]
        text = "## 同じ\n- file: x\n- id: zzz-2\n\n## 同じ\n- file: x\n\n## 同じ\n- file: x\n"
        ids = D.review_ids(D.parse(text, "s", D.REVIEW_KEYS)[0], "s")
        self.assertEqual(ids[0], "s/zzz-2")
        self.assertEqual(ids[2], ids[1] + "-2")
        h = D.review_ids(D.parse("## 同じ\n- file: x\n", "s", D.REVIEW_KEYS)[0], "s")[0]
        pinned = f"## 同じ\n- file: x\n\n## 同じ\n- file: x\n- id: {h[2:]}\n"
        self.assertEqual(D.review_ids(D.parse(pinned, "s", D.REVIEW_KEYS)[0], "s"), [h + "-2", h])
        self.assertTrue(base.startswith("s/"))

    def test_match_same_near_and_drop(self):
        run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "2026-11-10 t #1")
        out_path = self.root / "kept.md"
        code, out = run("--dir", str(self.dir), "match", str(self.review), "--drop", "--out", str(out_path))
        self.assertEqual(code, 0)
        # 見出しが同じ 3 件は同じ。quote（.role {）だけが重なる別の指摘は外さない
        self.assertEqual(out.count("SAME  "), 3)
        self.assertIn("NEAR  ja/get-started/motivation.mdx:70\tRole 名が PascalCase でない", out)
        kept, _ = D.parse(out_path.read_text(encoding="utf-8"), "kept", D.REVIEW_KEYS)
        self.assertEqual([e.title for e in kept], ["Role 名が PascalCase でない"])
        self.assertTrue(out_path.read_text(encoding="utf-8").startswith("---\ntitle: テスト\n---"))

    def test_quote_overlap_with_similar_title_is_same(self):
        d, _ = D.parse("## 直さない: 指示語「これら」\n- decision: 直さない\n- file: ja/a.mdx\n- quote: これらは katachi と\n- decided: x\n", "d", D.DECISION_KEYS)
        f, _ = D.parse("## [p2] 指示語「これらは」を使っている\n- file: ja/a.mdx\n- quote: `これらは` katachi と排他的\n", "f", D.REVIEW_KEYS)
        self.assertEqual(D.compare(f[0], d[0]), "same")
        other, _ = D.parse("## [p2] 指示語「これらは」\n- file: ja/b.mdx\n- quote: これらは katachi と\n", "f", D.REVIEW_KEYS)
        self.assertIsNone(D.compare(other[0], d[0]))

    def test_policy_match_covers_titles_worded_differently(self):
        policy = textwrap.dedent("""\
            ## 方針: 「このページの目的」のコメント（D1-1）
            - decision: 方針
            - file: ja/**
            - match: 目的の?コメント
            - decided: 2026-11-10 t #1
            """)
        d, _ = D.parse(policy, "docs.md", D.DECISION_KEYS)
        for title in ("「このページの目的」のコメントが無い（D1-1）", "目的コメントが import より後（D1-1）", "目的のコメントが import の後"):
            f, _ = D.parse(f"## [p2] {title}\n- file: ja/guides/a.mdx\n", "f", D.REVIEW_KEYS)
            self.assertEqual(D.compare(f[0], d[0]), "same", title)
        other, _ = D.parse("## [p3] description と本文が合っていない\n- file: ja/roadmap.mdx\n- quote: 目的コメント\n", "f", D.REVIEW_KEYS)
        self.assertIsNone(D.compare(other[0], d[0]))
        outside, _ = D.parse("## [p2] 目的のコメントが無い\n- file: README.md\n", "f", D.REVIEW_KEYS)
        self.assertIsNone(D.compare(outside[0], d[0]))

        # record-review は方針に合う「直さない」を 1 件ずつ写さず、id だけ出す
        (self.dir / "docs.md").write_text("# docs\n\n" + policy, encoding="utf-8")
        self.review.write_text("## [p2] 目的のコメントが無い（D1-1）\n- file: ja/guides/a.mdx\n- status: resolved\n- note: 直さない\n", encoding="utf-8")
        code, out = run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "x")
        self.assertEqual(code, 0)
        self.assertIn("写した 0 件 / 写してあった 1 件", out)
        self.assertEqual(out.count("REMOVE "), 1)

    def test_bad_match_pattern_is_an_error(self):
        (self.dir / "docs.md").write_text("## 方針: x\n- decision: 方針\n- match: (\n- decided: x\n", encoding="utf-8")
        errors = [e for d in D.load_decisions(self.dir) for e in d.errors]
        self.assertTrue(any("正規表現" in e for e in errors))
        self.assertTrue(any("file がありません" in e for e in errors))

    def test_glob(self):
        self.assertTrue(D.file_matches("docs/src/content/docs/ja/get-started/*", "docs/src/content/docs/ja/get-started/faq.mdx"))
        self.assertFalse(D.file_matches("docs/src/content/docs/ja/*", "docs/src/content/docs/ja/get-started/faq.mdx"))
        self.assertTrue(D.file_matches("katachi/src/**", "katachi/src/main/kotlin/A.kt"))
        self.assertTrue(D.file_matches(D.normalize_file("./ja/get-started/*"), "ja/get-started/faq.mdx"))

    def test_check_reports_errors_gone_and_lost(self):
        (self.root / "a.md").write_text("一行目\n残っている文\n", encoding="utf-8")
        (self.dir / "docs.md").write_text(textwrap.dedent("""\
            # docs

            ## 直さない: 残っている
            - decision: 直さない
            - file: a.md
            - quote: 残っている文
            - decided: 2026-11-10

            ## 直さない: 書き換わった
            - decision: 直さない
            - file: a.md
            - quote: もう無い文
            - decided: 2026-11-10

            ## 直さない: 消えたファイル
            - decision: 直さない
            - file: b.md
            - decided: 2026-11-10

            ## 後で: 条件が無い
            - decision: 後で
            - decided: 2026-11-10

            ## このまま: 見出しと違う
            - decision: 直さない
            - decided: 2026-11-10
            """), encoding="utf-8")
        code, out = run("--dir", str(self.dir), "check", "--root", str(self.root))
        self.assertEqual(code, 1)
        self.assertNotIn("残っている）", out)
        self.assertIn("LOST  docs.md:9", out)
        self.assertIn("GONE  docs.md:15", out)
        self.assertIn("revisit", out)
        self.assertIn("見出しの「このまま」と decision の「直さない」が違います", out)
        self.assertIn("NG 判断", out)
        self.assertIn("問題 4 件", out)
        self.assertIn("判断 5 件（直さない 4・後で 1）", out)

    def test_dir_can_follow_the_subcommand(self):
        run("--dir", str(self.dir), "record-review", str(self.review), "--decided", "2026-11-10 t #1")
        _, before = run("--dir", str(self.dir), "list")
        _, after = run("list", "--dir", str(self.dir))
        self.assertEqual(before, after)
        self.assertIn("計 2 件（直さない 2）", after)

    def test_check_finds_files_under_the_prefix(self):
        (self.root / "docs").mkdir()
        (self.root / "docs" / "a.md").write_text("残っている文\n", encoding="utf-8")
        (self.dir / "decisions.md").write_text("## 直さない: x\n- decision: 直さない\n- file: a.md\n- quote: 残っている文\n- decided: d\n", encoding="utf-8")
        code, out = run("--dir", str(self.dir), "check", "--root", str(self.root))
        self.assertEqual(code, 1)
        self.assertIn("GONE", out)
        code, out = run("--dir", str(self.dir), "check", "--root", str(self.root), "--file-prefix", "docs")
        self.assertEqual(code, 0, out)
        self.assertIn("OK 判断 1 件", out)

    def test_record_review_creates_the_directory(self):
        target = self.root / "new" / "decisions"
        code, out = run("record-review", str(self.review), "--decided", "d", "--dir", str(target), "--to", "docs.md")
        self.assertEqual(code, 0)
        self.assertTrue((target / "docs.md").is_file())


if __name__ == "__main__":
    unittest.main()
