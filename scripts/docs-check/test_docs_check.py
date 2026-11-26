#!/usr/bin/env python3
"""docs_check.py のテスト。小さなリポジトリを一時ディレクトリに作り、壊した状態を検出できるかを見る。

usage: python3 scripts/docs-check/test_docs_check.py
"""
import contextlib
import io
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import docs_check  # noqa: E402

CLAUDE_MD = """\
# CLAUDE.md

## Group

| 絵文字 | グループ (ja) | Group (en) |
|---|---|---|
| 🔴 | タスク管理 | Task Management |
| 🟡 | テスト・検証 | Testing / Verification |

## Next
"""


def row(group, kind, name, ext, install, status, desc):
    g = f"<td>{group}</td>\n" if group is not None else ""
    return f"""<tr>
{g}<td><a href="./{kind}/{name}{ext}">{name}</a></td>
<td>

```sh
{install}
```

</td>
<td>{status}</td>
<td>{desc}</td>
</tr>"""


def readme(lang, rows_skills, rows_rules, rows_archived=""):
    head = "<tr>\n<th>a</th>\n</tr>"
    other = "[日本語](./README.ja.md)" if lang == "en" else "[English](./README.md)"
    s, r, c = ("Skills", "Rules", "Contribute") if lang == "en" else ("スキル", "ルール", "コントリビュート")
    return f"""# Skills

{other}

## ⭐️ {s}

<table>
{head}
{rows_skills}
</table>

## 📝 {r}

<table>
{head}
{rows_rules}
</table>

## 🤝 {c}

<table>
{head}
</table>

<details>

<summary> ❌ Archived </summary>

<table>
{head}
{rows_archived}
</table>

</details>
"""


def detail(lang, name, install, extra=""):
    other = f"[日本語](./{name}.ja.md)" if lang == "en" else f"[English](./{name}.md)"
    sec = "Usage" if lang == "en" else "使い方"
    body = "It does a thing." if lang == "en" else "何かをする。"
    return f"# {name}\n\n{other}\n\n{body}\n\n## {sec}\n\n```sh\n{install}\n```\n{extra}"


class Repo:
    def __init__(self, root: Path):
        self.root = root

    def write(self, rel, text):
        p = self.root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(textwrap.dedent(text) if text.startswith("\n") else text, encoding="utf-8")

    def read(self, rel):
        return (self.root / rel).read_text(encoding="utf-8")

    def replace(self, rel, old, new):
        t = self.read(rel)
        assert old in t, (rel, old)
        self.write(rel, t.replace(old, new))


def make_repo(root: Path) -> Repo:
    r = Repo(root)
    r.write("CLAUDE.md", CLAUDE_MD)
    for name, group, status in (("alpha", "タスク管理", "Active"), ("beta", "テスト・検証", "Experimental"), ("old", "タスク管理", "Archived")):
        r.write(f"skills/{name}/SKILL.md", f"---\nname: {name}\nmetadata:\n  status: {status}\n  group: {group}\n---\n\nbody\n")
        inst = f"gh skill install tbsten/skills {name}"
        r.write(f"skills/{name}.md", detail("en", name, inst))
        r.write(f"skills/{name}.ja.md", detail("ja", name, inst))
    r.write("rules/gamma/RULE.md", "rule\n")
    rinst = "curl -fsSL https://rules.tbsten.me/i | bash -s -- gamma"
    r.write("rules/gamma.md", "---\nstatus: Experimental\ngroup: テスト・検証\n---\n\n" + detail("en", "gamma", rinst))
    r.write("rules/gamma.ja.md", detail("ja", "gamma", rinst))

    def rows(lang):
        ext = ".ja.md" if lang == "ja" else ".md"
        g1 = "🔴 タスク管理" if lang == "ja" else "🔴 Task Management"
        g2 = "🟡 テスト・検証" if lang == "ja" else "🟡 Testing / Verification"

        def d(n):
            return f"{n} の説明" if lang == "ja" else f"About {n}"
        skills = "\n".join([
            row(g1, "skills", "alpha", ext, "gh skill install tbsten/skills alpha", "✅ Active", d("alpha")),
            row(g2, "skills", "beta", ext, "gh skill install tbsten/skills beta", "🧪 Experimental", d("beta")),
        ])
        rules = row(g2, "rules", "gamma", ext, "curl -fsSL https://rules.tbsten.me/i | \\\n  bash -s -- gamma", "🧪 Experimental", d("gamma"))
        archived = row(None, "skills", "old", ext, "gh skill install tbsten/skills old", "❌ Archived", d("old"))
        return skills, rules, archived

    r.write("README.ja.md", readme("ja", *rows("ja")))
    r.write("README.md", readme("en", *rows("en")))
    return r


def run(cmd, *args):
    out, err = io.StringIO(), io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        code = docs_check.main([cmd, *args])
    return code, out.getvalue() + err.getvalue()


class DocsCheckTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.repo = make_repo(self.root)
        self._root = docs_check.ROOT
        docs_check.ROOT = self.root

    def tearDown(self):
        docs_check.ROOT = self._root
        self.tmp.cleanup()

    def assertOk(self, cmd, *args):
        code, out = run(cmd, *args)
        self.assertEqual(code, 0, out)

    def assertNg(self, cmd, needle, *args):
        code, out = run(cmd, *args)
        self.assertEqual(code, 1, out)
        self.assertIn(needle, out)

    # ---- 正常系
    def test_clean_repo_passes(self):
        self.assertOk("structure")
        self.assertOk("readme")

    # ---- structure
    def test_missing_translation(self):
        (self.root / "skills/beta.md").unlink()
        self.assertNg("structure", "skills/beta.ja.md に対になる skills/beta.md が無い")

    def test_missing_language_link(self):
        self.repo.replace("skills/alpha.ja.md", "[English](./alpha.md)", "")
        self.assertNg("structure", "skills/alpha.ja.md の冒頭に alpha.md へのリンクが無い")

    def test_heading_drift(self):
        self.repo.write("skills/alpha.md", self.repo.read("skills/alpha.md") + "\n## Extra\n\ntext\n")
        self.assertNg("structure", "見出しの構成が違う")

    def test_code_block_drift(self):
        self.repo.write("skills/alpha.md", self.repo.read("skills/alpha.md") + "\n```\nx\n```\n")
        self.assertNg("structure", "コードブロックの数が違う")

    def test_japanese_in_english_doc(self):
        self.repo.replace("skills/alpha.md", "It does a thing.", "It does a thing。")
        self.assertNg("structure", "英語版に日本語・全角文字がある")

    def test_japanese_in_inline_code_and_lang_link_is_allowed(self):
        self.repo.replace("skills/alpha.md", "It does a thing.", "Press the `カード` button.")
        self.assertOk("structure")

    def test_allowlist(self):
        self.repo.replace("skills/alpha.md", "It does a thing.", "Say 「はい」.")
        self.assertNg("structure", "英語版に日本語")
        allow = docs_check.HERE / "en-allowlist.tsv"
        orig = allow.read_text(encoding="utf-8")
        try:
            allow.write_text(orig + "skills/alpha.md\t「はい」\tテスト\n", encoding="utf-8")
            self.assertOk("structure")
            self.repo.replace("skills/alpha.md", "Say 「はい」.", "Say yes.")
            self.assertNg("structure", "どこにも使われていない")
        finally:
            allow.write_text(orig, encoding="utf-8")

    def test_english_heading_in_japanese_doc(self):
        self.repo.replace("skills/alpha.ja.md", "## 使い方", "## Usage")
        self.assertNg("structure", "日本語版の見出しが英語になっている")

    def test_code_heading_in_japanese_doc_is_allowed(self):
        self.repo.replace("skills/alpha.ja.md", "## 使い方", "## `alpha` (CLI)")
        self.assertOk("structure")

    def test_broken_relative_link(self):
        self.repo.replace("skills/alpha.ja.md", "何かをする。", "[例](./alpha/example/Missing.kt) を見る。")
        self.assertNg("structure", "リンク先 ./alpha/example/Missing.kt が無い")

    def test_link_inside_code_block_is_ignored(self):
        self.repo.replace("skills/alpha.ja.md", "```sh\n", "```sh\n[x](./nope.md)\n")
        self.assertOk("structure")

    def test_missing_install_command(self):
        self.repo.replace("skills/beta.md", "gh skill install tbsten/skills beta", "echo hi")
        self.assertNg("structure", "skills/beta.md にインストール / 実行の方法")

    # ---- readme
    def test_status_mismatch(self):
        self.repo.replace("skills/beta/SKILL.md", "status: Experimental", "status: Active")
        self.assertNg("readme", "beta のステータスが '🧪 Experimental' (SSoT は '✅ Active')")

    def test_missing_row(self):
        self.repo.replace("skills/alpha/SKILL.md", "status: Active", "status: Active")
        self.repo.write("skills/delta/SKILL.md", "---\nname: delta\nmetadata:\n  status: WIP\n  group: タスク管理\n---\n")
        self.assertNg("readme", "skills/delta が README に載っていない")

    def test_archived_must_be_in_details(self):
        self.repo.replace("skills/beta/SKILL.md", "status: Experimental", "status: Archived")
        self.assertNg("readme", "beta が「skills」の表にある (正しくは「archived」)")

    def test_description_too_long(self):
        self.repo.replace("README.md", "<td>About beta</td>", "<td>" + "x" * 81 + "</td>")
        self.assertNg("readme", "beta の説明が 81 文字")

    def test_group_cell_only_on_first_row(self):
        self.repo.replace("skills/beta/SKILL.md", "group: テスト・検証", "group: タスク管理")
        self.assertNg("readme", "グループのセルは空にする")

    def test_group_label_language(self):
        self.repo.replace("README.md", "🔴 Task Management", "🔴 タスク管理")
        self.assertNg("readme", "グループのセルは '🔴 Task Management'")

    def test_link_language(self):
        self.repo.replace("README.ja.md", './skills/alpha.ja.md"', './skills/alpha.md"')
        self.assertNg("readme", "alpha のリンク先が英語版になっている")

    # ---- pairs-changed
    def git(self, *args):
        subprocess.run(["git", "-C", str(self.root), *args], check=True, capture_output=True)

    def init_git(self):
        self.git("init", "-q", "-b", "main")
        self.git("-c", "user.name=t", "-c", "user.email=t@example.com", "add", "-A")
        self.git("-c", "user.name=t", "-c", "user.email=t@example.com", "commit", "-qm", "init")

    def commit(self, msg):
        self.git("-c", "user.name=t", "-c", "user.email=t@example.com", "commit", "-qam", msg)

    def test_pairs_changed(self):
        self.init_git()
        self.repo.replace("skills/alpha.ja.md", "何かをする。", "何かをもっとする。")
        self.commit("ja only")
        self.assertNg("pairs-changed", "skills/alpha.ja.md だけが変わり", "HEAD~1")
        self.repo.replace("skills/alpha.md", "It does a thing.", "It does more.")
        self.commit("translate")
        self.assertOk("pairs-changed", "HEAD~2")

    def test_pairs_changed_english_only(self):
        self.init_git()
        self.repo.replace("skills/alpha.md", "It does a thing.", "It does more.")
        self.commit("en only")
        self.assertNg("pairs-changed", "skills/alpha.md だけが変わり、原本の", "HEAD~1")

    def test_pairs_changed_trailer(self):
        self.init_git()
        self.repo.replace("skills/alpha.ja.md", "何かをする。", "何かを、する。")
        self.commit("typo\n\nDocs-Translation-Not-Needed: skills/*.ja.md")
        self.assertOk("pairs-changed", "HEAD~1")


if __name__ == "__main__":
    unittest.main(verbosity=1)
