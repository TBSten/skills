#!/usr/bin/env python3
"""人間が読むドキュメント (README と skills/rules/prompts の詳細ドキュメント) を機械的に検査する。

scripts/docs-check/check.sh から呼ぶ。python3 の標準ライブラリだけで動く。

usage (リポジトリのどこからでも):
  docs_check.py structure            # 日英の組・言語リンク・構成のずれ・相対リンク・英語版の日本語
  docs_check.py readme               # README の一覧表が SSoT (SKILL.md / 詳細ドキュメントの frontmatter) と一致するか
  docs_check.py pairs-changed <base> # <base>...HEAD で日英の片方だけが変わった組 (PR 用)
  docs_check.py list-ja              # textlint に渡す日本語のファイル一覧

どのサブコマンドも、問題があれば「何が / なぜ / どう直すか」を出して終了コード 1 で終わる。
決まりの説明は .claude/rules/docs-writing.md。
"""
from __future__ import annotations

import fnmatch
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
KINDS = ("skills", "rules", "prompts")
STATUS_EMOJI = {
    "WIP": "🌱",
    "Experimental": "🧪",
    "Active": "✅",
    "Active-Prime": "💎",
    "Archived": "❌",
}
DESC_MAX = 80
SKIP_PAIR_TRAILER = "Docs-Translation-Not-Needed:"

# ---------------------------------------------------------------- 共通


class Problems:
    def __init__(self) -> None:
        self.items: list[tuple[str, str, str]] = []

    def add(self, what: str, why: str, fix: str) -> None:
        self.items.append((what, why, fix))

    def finish(self, ok_message: str) -> int:
        if not self.items:
            print(f"OK {ok_message}")
            return 0
        for what, why, fix in self.items:
            print(f"ERROR: {what}", file=sys.stderr)
            if why:
                print(f"  why: {why}", file=sys.stderr)
            if fix:
                print(f"  fix: {fix}", file=sys.stderr)
        print(f"NG {ok_message}: {len(self.items)} 件", file=sys.stderr)
        return 1


def rel(p: Path) -> str:
    return p.relative_to(ROOT).as_posix()


def doc_pairs() -> list[tuple[Path, Path]]:
    """(日本語の原本, 英語の訳) の組。片方しか無いものも返す (無い側は存在しないパス)。"""
    pairs = [(ROOT / "README.ja.md", ROOT / "README.md")]
    for kind in KINDS:
        d = ROOT / kind
        names = set()
        for p in d.glob("*.md"):
            names.add(p.name[: -len(".ja.md")] if p.name.endswith(".ja.md") else p.name[: -len(".md")])
        for n in sorted(names):
            pairs.append((d / f"{n}.ja.md", d / f"{n}.md"))
    return pairs


FENCE = re.compile(r"^(\s*)(`{3,}|~{3,})")


def split_frontmatter(text: str) -> tuple[str, str]:
    if text.startswith("---\n"):
        end = text.find("\n---\n", 4)
        if end != -1:
            return text[: end + 5], text[end + 5 :]
    return "", text


def frontmatter_value(text: str, key: str, indent: int = 0) -> str | None:
    fm, _ = split_frontmatter(text)
    pre = " " * indent + key + ":"
    for line in fm.splitlines():
        if line.startswith(pre) and (indent == len(line) - len(line.lstrip(" "))):
            return line[len(pre) :].strip().strip("\"'")
    return None


def prose_lines(text: str):
    """frontmatter とコードブロックを除いた (行番号, 行) を返す。HTML の <pre> も除く。"""
    fm, _ = split_frontmatter(text)
    skip_to = fm.count("\n")
    fence = None
    for i, line in enumerate(text.split("\n"), 1):
        if i <= skip_to:
            continue
        m = FENCE.match(line)
        if fence is None and m:
            fence = m.group(2)[0] * len(m.group(2))
            continue
        if fence is not None:
            if line.strip().startswith(fence) and set(line.strip()) <= {fence[0]}:
                fence = None
            continue
        yield i, line


def outline(text: str) -> tuple[list[int], int]:
    """見出しのレベルの並びと、コードブロックの数。"""
    levels = []
    for _, line in prose_lines(text):
        m = re.match(r"(#{1,6}) ", line)
        if m:
            levels.append(len(m.group(1)))
    blocks = 0
    in_fence = None
    for line in text.split("\n"):
        m = FENCE.match(line)
        if in_fence is None and m:
            in_fence = m.group(2)[0] * len(m.group(2))
            blocks += 1
        elif in_fence is not None and line.strip().startswith(in_fence) and set(line.strip()) <= {in_fence[0]}:
            in_fence = None
    return levels, blocks


# ---------------------------------------------------------------- structure

JA_CHARS = re.compile(r"[　-〿぀-ヿ一-鿿！-｠￠-￮]")
LINK = re.compile(r"""(?:!?\[[^\]]*\]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)|(?:href|src)="([^"]+)")""")


def load_en_allowlist() -> list[tuple[str, str, str]]:
    """en-allowlist.tsv: ファイル<TAB>許す文字列<TAB>理由。# で始まる行と空行は読まない。"""
    path = HERE / "en-allowlist.tsv"
    out = []
    if not path.exists():
        return out
    for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) != 3 or not all(c.strip() for c in cols):
            sys.exit(f"ERROR: en-allowlist.tsv:{n}: 列は「ファイル<TAB>許す文字列<TAB>理由」の 3 つ")
        out.append((cols[0].strip(), cols[1], cols[2].strip()))
    return out


def cmd_structure() -> int:
    pr = Problems()
    allow = load_en_allowlist()
    used_allow = set()

    for ja, en in doc_pairs():
        for a, b in ((ja, en), (en, ja)):
            if a.exists() and not b.exists():
                pr.add(
                    f"{rel(a)} に対になる {rel(b)} が無い",
                    "README と詳細ドキュメントは日本語 (原本) と英語 (訳) の組で持つ",
                    f"{rel(b)} を作る (英語なら日本語版を訳す)。片方が不要になったのなら両方消す",
                )
        if not (ja.exists() and en.exists()):
            continue
        tj, te = ja.read_text(encoding="utf-8"), en.read_text(encoding="utf-8")

        # 言語の切り替えリンク (本文の冒頭 15 行以内)
        for src, text, other, label in ((ja, tj, en, "[English]"), (en, te, ja, "[日本語]")):
            head = "\n".join(split_frontmatter(text)[1].split("\n")[:15])
            if f"]({'./' + other.name})" not in head:
                pr.add(
                    f"{rel(src)} の冒頭に {other.name} へのリンクが無い",
                    "GitHub で開いた読者がもう一方の言語へ移れるように",
                    f"H1 の直後に `{label}(./{other.name})` を置く",
                )

        # インストール / 実行の方法が書いてあるか (README は対象外)
        kind = ja.parent.name
        if ja.parent != ROOT and kind in KINDS:
            name = en.name[:-3]
            want = {
                "skills": f"gh skill install tbsten/skills {name}",
                "rules": f"bash -s -- {name}",
                "prompts": f"/prompts/{name}/PROMPT.md",
            }[kind]
            for p, t in ((ja, tj), (en, te)):
                if want not in re.sub(r"\\\s*\n\s*", "", t):
                    pr.add(
                        f"{rel(p)} にインストール / 実行の方法 ('{want}') が無い",
                        "詳細ドキュメントを開いた読者が、そのまま使い始められるように",
                        "冒頭近くにインストール (prompt なら実行) のコードブロックを置く",
                    )

        # 構成のずれ (見出しのレベルの並び・コードブロックの数)
        (lj, bj), (le, be) = outline(tj), outline(te)
        if lj != le:
            pr.add(
                f"{rel(ja)} と {rel(en)} の見出しの構成が違う (ja {len(lj)} 個 {lj} / en {len(le)} 個 {le})",
                "英語版は日本語版の訳なので、節の構成・順序は 1 対 1",
                "日本語版 (原本) に合わせて英語版の見出しを足す・消す・並べ直す",
            )
        if bj != be:
            pr.add(
                f"{rel(ja)} と {rel(en)} のコードブロックの数が違う (ja {bj} / en {be})",
                "コード例は日英で同じものを持つ",
                "日本語版に合わせてコードブロックをそろえる",
            )

        # 日本語版の見出しが英語だけになっていないか (H1 の名前・インラインコード・かっこ書きは見ない)
        for n, line in prose_lines(tj):
            m = re.match(r"(#{2,6}) (.+)", line)
            if not m:
                continue
            words = re.sub(r"`[^`]*`|\([^)]*\)|（[^）]*）", "", m.group(2))
            if re.search(r"[A-Za-z]{2,}", words) and not JA_CHARS.search(words):
                pr.add(
                    f"{rel(ja)}:{n}: 日本語版の見出しが英語になっている: {line.strip()}",
                    "日本語版は日本語の読者向け。英語版と見出しをそろえるのは構成 (数・順序) で、言語ではない",
                    "日本語の見出しにする。識別子・ファイル名なら `code` で囲む",
                )

        # 英語版に日本語・全角文字が残っていないか (frontmatter・コードブロック・インラインコードは見ない)
        for n, line in prose_lines(te):
            visible = re.sub(r"`[^`]*`", "", line)
            visible = re.sub(r"\[日本語\]\(\./[^)\s]+\.ja\.md\)", "", visible)  # 言語の切り替えリンク
            for f, text, _ in allow:
                if f == rel(en) and text in visible:
                    visible = visible.replace(text, "")
                    used_allow.add((f, text))
            m = JA_CHARS.search(visible)
            if m:
                pr.add(
                    f"{rel(en)}:{n}: 英語版に日本語・全角文字がある: {line.strip()[:100]}",
                    "英語版は英語の読者向けの訳",
                    "英語に訳し、全角の記号は半角にする。日本語の UI 文字列やプロンプト例をそのまま見せるならインラインコードかコードブロックに入れる。"
                    "それでも残す必要があれば scripts/docs-check/en-allowlist.tsv に理由つきで足す",
                )

    for f, text, _ in allow:
        if (f, text) not in used_allow:
            pr.add(
                f"en-allowlist.tsv の「{f}\t{text}」がどこにも使われていない",
                "直したあとに許可が残ると、次に同じ文字列が入ったときに見逃す",
                "en-allowlist.tsv からその行を消す",
            )

    # 相対リンクの切れ
    for ja, en in doc_pairs():
        for p in (ja, en):
            if not p.exists():
                continue
            for n, line in prose_lines(p.read_text(encoding="utf-8")):
                for m in LINK.finditer(re.sub(r"`[^`]*`", "", line)):
                    url = m.group(1) or m.group(2)
                    if re.match(r"[a-zA-Z][a-zA-Z0-9+.-]*:|#|//", url):
                        continue
                    target = (p.parent / url.split("#")[0].split("?")[0]).resolve()
                    if not target.exists():
                        pr.add(
                            f"{rel(p)}:{n}: リンク先 {url} が無い",
                            "相対リンクは .md の置き場所から解決される",
                            "パスを直す (skills/<name>.md からは ./<name>/... で同じ skill のファイルを指す)",
                        )
    return pr.finish("structure")


# ---------------------------------------------------------------- readme


def load_groups() -> list[tuple[str, str, str]]:
    """CLAUDE.md の Group 表 (絵文字 | グループ (ja) | Group (en)) を読む。"""
    text = (ROOT / "CLAUDE.md").read_text(encoding="utf-8")
    sec = text.split("\n## Group\n", 1)
    if len(sec) != 2:
        sys.exit("ERROR: CLAUDE.md に '## Group' の節が無い\n  fix: グループの表の場所を変えたら docs_check.py の load_groups も直す")
    groups = []
    for line in sec[1].split("\n## ", 1)[0].splitlines():
        cols = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cols) == 3 and cols[0] not in ("絵文字",) and not set(cols[0]) <= {"-"}:
            groups.append((cols[0], cols[1], cols[2]))
    if not groups:
        sys.exit("ERROR: CLAUDE.md の Group 表が読めない")
    return groups


def load_items() -> list[dict]:
    items = []
    for f in sorted((ROOT / "skills").glob("*/SKILL.md")):
        t = f.read_text(encoding="utf-8")
        items.append(
            {
                "kind": "skills",
                "name": f.parent.name,
                "status": frontmatter_value(t, "status", 2),
                "group": frontmatter_value(t, "group", 2),
            }
        )
    for kind in ("rules", "prompts"):
        for f in sorted((ROOT / kind).glob("*.md")):
            if f.name.endswith(".ja.md"):
                continue
            t = f.read_text(encoding="utf-8")
            items.append(
                {
                    "kind": kind,
                    "name": f.name[:-3],
                    "status": frontmatter_value(t, "status"),
                    "group": frontmatter_value(t, "group"),
                }
            )
    return items


SECTIONS = {  # README の見出しの絵文字 → 表の種類
    "⭐️": "skills",
    "📝": "rules",
    "💬": "prompts",
    "🤝": "contribute",
}


def parse_readme(path: Path) -> dict[str, list[dict]]:
    """表の種類 → 行 ({line, cells}) の一覧。Archived は <details> の ❌ の表。"""
    text = path.read_text(encoding="utf-8")
    tables: dict[str, list[dict]] = {}
    current = None
    for m in re.finditer(r"^## (\S+)|<summary>[^<]*❌[^<]*</summary>|<tr>(.*?)</tr>", text, re.S | re.M):
        if m.group(1) is not None:
            current = SECTIONS.get(m.group(1))
        elif m.group(0).startswith("<summary>"):
            current = "archived"
        elif current:
            cells = re.findall(r"<td>(.*?)</td>", m.group(2), re.S)
            if cells:  # 見出し行 (<th>) は飛ばす
                tables.setdefault(current, []).append(
                    {"line": text.count("\n", 0, m.start()) + 1, "cells": [c.strip() for c in cells]}
                )
    return tables


def visible_text(html: str) -> str:
    t = re.sub(r"<[^>]+>", "", html)
    return re.sub(r"\s+", " ", t).strip()


def cmd_readme() -> int:
    pr = Problems()
    groups = load_groups()
    group_order = [g[1] for g in groups]
    items = load_items()
    by_key = {(i["kind"], i["name"]): i for i in items}

    for it in items:
        where = "skills/{0}/SKILL.md の metadata".format(it["name"]) if it["kind"] == "skills" else f"{it['kind']}/{it['name']}.md の frontmatter"
        if it["status"] not in STATUS_EMOJI:
            pr.add(f"{where}.status '{it['status']}' が不正", "CLAUDE.md の Status 表の値だけを使う", "WIP / Experimental / Active / Active-Prime / Archived のどれかにする")
        if it["group"] is None and not it["name"].startswith("contribute-"):
            pr.add(f"{where} に group が無い", "README のグループ列の SSoT", "CLAUDE.md の Group 表から選んで書く")
        elif it["group"] is not None and it["group"] not in group_order:
            pr.add(f"{where}.group '{it['group']}' が CLAUDE.md の Group 表に無い", "README のグループ列は Group 表から作る", "表の日本語名に合わせる (新グループなら CLAUDE.md の表に足す)")

    for lang, path in (("ja", ROOT / "README.ja.md"), ("en", ROOT / "README.md")):
        name_col = {"ja": 1, "en": 2}[lang]
        ext = ".ja.md" if lang == "ja" else ".md"
        tables = parse_readme(path)
        seen: dict[tuple[str, str], str] = {}

        for table, rows in tables.items():
            has_group = table in KINDS
            current_group = None
            order_seen: list[str] = []
            for row in rows:
                cells = row["cells"]
                loc = f"{path.name}:{row['line']}"
                want_cols = 5 if has_group else 4
                if len(cells) != want_cols:
                    pr.add(f"{loc}: 列の数が {len(cells)} (期待は {want_cols})", "", "テンプレート (.claude/skills/contribute-*.md) の行の形に合わせる")
                    continue
                name_cell = cells[1] if has_group else cells[0]
                m = re.search(r'<a href="\./(skills|rules|prompts)/([^"/]+?)(\.ja)?\.md">([^<]+)</a>', name_cell)
                if not m:
                    pr.add(f"{loc}: 名前のセルにリンクが無い: {name_cell[:80]}", "", '<a href="./<kind>/<name>' + ext + '"><name></a> の形にする')
                    continue
                kind, fname, is_ja, label = m.group(1), m.group(2), m.group(3), m.group(4)
                if label != fname:
                    pr.add(f"{loc}: リンクの文字 '{label}' とリンク先 '{fname}' が違う", "", "どちらも skill / rule / prompt の名前にする")
                if bool(is_ja) != (lang == "ja"):
                    pr.add(f"{loc}: {fname} のリンク先が{'英語' if lang == 'ja' else '日本語'}版になっている", "README は自分と同じ言語の詳細ドキュメントを指す", f"./{kind}/{fname}{ext} にする")
                key = (kind, fname)
                it = by_key.get(key)
                if it is None:
                    pr.add(f"{loc}: {kind}/{fname} が README にあるが実体が無い", "", "行を消すか、名前を直す")
                    continue
                if key in seen:
                    pr.add(f"{loc}: {fname} が 2 回載っている (前は {seen[key]})", "", "どちらかの行を消す")
                seen[key] = loc

                # 置き場所の表
                expected_table = "archived" if it["status"] == "Archived" else ("contribute" if fname.startswith("contribute-") else kind)
                if table != expected_table:
                    pr.add(f"{loc}: {fname} が「{table}」の表にある (正しくは「{expected_table}」)", "Archived は Contribute の下の <details>、contribute-* は Contribute の表に置く", "行を移す")

                # ステータス
                status_cell = visible_text(cells[3] if has_group else cells[2])
                want_status = f"{STATUS_EMOJI.get(it['status'], '?')} {it['status']}"
                if status_cell != want_status:
                    pr.add(f"{loc}: {fname} のステータスが '{status_cell}' (SSoT は '{want_status}')", "README のステータス列は SSoT のミラー", f"'{want_status}' にする (status を変えるなら .claude/skills/change-status.md の手順)")

                # インストール / 実行
                install = cells[2] if has_group else cells[1]
                want_install = {
                    "skills": f"gh skill install tbsten/skills {fname}",
                    "rules": f"bash -s -- {fname}",
                    "prompts": f"/prompts/{fname}/PROMPT.md",
                }[kind]
                if want_install not in re.sub(r"\\\s*\n\s*", "", install):
                    pr.add(f"{loc}: {fname} のインストール / 実行の欄に '{want_install}' が無い", "", "テンプレートどおりのコマンドにする")

                # 説明の長さ
                desc = visible_text(cells[4] if has_group else cells[3])
                if not desc:
                    pr.add(f"{loc}: {fname} の説明が空", "", "1 行の説明を書く")
                elif len(desc) > DESC_MAX:
                    pr.add(f"{loc}: {fname} の説明が {len(desc)} 文字 (最大 {DESC_MAX})", "README は一覧。詳しい説明は詳細ドキュメントに書く", f"{DESC_MAX} 文字以内に縮める: {desc}")

                # グループ
                if has_group:
                    g_cell = visible_text(cells[0])
                    gm = next((g for g in groups if it["group"] == g[1]), None)
                    if gm is None:
                        continue
                    want_label = f"{gm[0]} {gm[1] if lang == 'ja' else gm[2]}"
                    if it["group"] != current_group:
                        if g_cell != want_label:
                            pr.add(f"{loc}: {fname} はグループの先頭行なので、グループのセルは '{want_label}' (今は '{g_cell}')", "グループのセルは各グループの先頭行だけに書く", f"<td>{want_label}</td> にする")
                        if it["group"] in order_seen:
                            pr.add(f"{loc}: グループ '{it['group']}' の行が離れている", "同じグループの行は隣り合わせる", "行をそのグループのまとまりの中へ移す")
                        order_seen.append(it["group"])
                        current_group = it["group"]
                    elif g_cell:
                        pr.add(f"{loc}: {fname} はグループの 2 行目以降なので、グループのセルは空にする (今は '{g_cell}')", "", "<td></td> にする")
            idx = [group_order.index(g) for g in order_seen if g in group_order]
            if idx != sorted(idx):
                pr.add(f"{path.name}: 「{table}」の表のグループの並びが CLAUDE.md の Group 表の順と違う ({' → '.join(order_seen)})", "", "Group 表の順に並べ替える")

        for it in items:
            key = (it["kind"], it["name"])
            if key not in seen:
                pr.add(f"{path.name}: {it['kind']}/{it['name']} が README に載っていない", "README は全 skill / rule / prompt の一覧", f".claude/skills/contribute-{it['kind'][:-1]}.md のテンプレートで行を足す")
    return pr.finish("readme")


# ---------------------------------------------------------------- pairs-changed


def git(*args: str) -> str:
    return subprocess.run(["git", "-C", str(ROOT), *args], check=True, capture_output=True, text=True).stdout


def cmd_pairs_changed(base: str) -> int:
    pr = Problems()
    changed = set(git("diff", "--name-only", f"{base}...HEAD").split())
    changed |= set(git("diff", "--name-only", "HEAD").split())  # 未コミットの変更も見る
    log = git("log", "--format=%B", f"{base}..HEAD")
    skipped = {l.split(":", 1)[1].strip() for l in log.splitlines() if l.startswith(SKIP_PAIR_TRAILER)}
    for ja, en in doc_pairs():
        rj, re_ = rel(ja), rel(en)
        if not (ja.exists() and en.exists()):
            continue
        def skip(path: str) -> bool:
            return any(fnmatch.fnmatch(path, pat) for pat in skipped)

        if rj in changed and re_ not in changed and not skip(rj):
            pr.add(
                f"{rj} だけが変わり、訳の {re_} が変わっていない",
                "原本を直したら、その場で訳も直す (溜めるとどちらが新しいか分からなくなる)",
                f"{re_} に同じ変更を訳して入れる。表記だけの修正などで訳が要らないなら、コミットメッセージに `{SKIP_PAIR_TRAILER} {rj}` の行を入れる",
            )
        if re_ in changed and rj not in changed and not skip(re_):
            pr.add(
                f"{re_} だけが変わり、原本の {rj} が変わっていない",
                "英語版は日本語版 (原本) の訳。英語側だけに内容を足さない",
                f"先に {rj} を直してから訳す。訳の誤りを直しただけなら、コミットメッセージに `{SKIP_PAIR_TRAILER} {re_}` の行を入れる",
            )
    return pr.finish(f"pairs-changed (base={base})")


# ---------------------------------------------------------------- main


def main(argv: list[str]) -> int:
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 0
    cmd = argv[0]
    if cmd == "structure":
        return cmd_structure()
    if cmd == "readme":
        return cmd_readme()
    if cmd == "pairs-changed" and len(argv) == 2:
        return cmd_pairs_changed(argv[1])
    if cmd == "list-ja":
        for ja, _ in doc_pairs():
            if ja.exists():
                print(rel(ja))
        return 0
    print(f"ERROR: 未知の引数 {' '.join(argv)}\n  fix: --help で usage を確認する", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
