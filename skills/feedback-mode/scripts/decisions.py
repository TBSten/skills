#!/usr/bin/env python3
"""判断の記録（decisions/*.md）を読む・照らす・書き足す。feedback-mode skill の script。読解・書き換え・再実装せず、そのまま実行する。

形と使い方の説明は references/review/decisions.md。指摘のファイルは Review の形（references/review/review.md:
`## [pN] 見出し` + `- file:` `- line:` `- quote:` …）。python3（3.10 以上）の標準ライブラリだけ。

使い方（リポジトリの直下で）:
  python3 decisions.py list [--decision 直さない]
      判断の一覧（ファイル・decision・場所・見出し）
  python3 decisions.py check [--root .]
      形の誤り・無いファイル（GONE）・quote が見つからない判断（LOST）・revisit の付いた判断（REVISIT）を出す。誤りがあれば 1 で終わる
  python3 decisions.py match <review.md> [--drop] [--out <path>]
      指摘のうち、過去の判断と同じもの（SAME）と、同じ箇所の別の指摘かもしれないもの（NEAR）を出す。
      --drop は SAME を外したファイルを書く（既定は上書き。--out で別の場所へ）
  python3 decisions.py record-review <review.md> --decided "<日付 回>" [--to decisions.md] [--by "<誰・どこで>"]
      Review で「直さない」になった指摘（status: resolved、note の頭が「直さない」）を判断として写す。
      人が足した選択肢のうち `- decision:` の付いたもの（review/_actions.md）で解決済みにした指摘も、その decision で写す。
      写した（または既に写してあった・方針の match に合った）指摘の id を `REMOVE <id>` で消す順に出す。
      そのあと出た順に review.mjs remove <id> で消す

共通: --dir <判断の置き場>（既定: $FEEDBACK_DECISIONS_DIR か <カレント>/.claude/feedback-decisions）
      --file-prefix <dir>（file がそのままで見つからないときに前に付けて探す。既定: $FEEDBACK_FILE_PREFIX。review.mjs と同じ）
"""
import argparse
import difflib
import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

sys.dont_write_bytecode = True

DECISIONS_DIR = Path(os.environ.get("FEEDBACK_DECISIONS_DIR") or ".claude/feedback-decisions")
FILE_PREFIX = os.environ.get("FEEDBACK_FILE_PREFIX", "")
DECISIONS = ("直さない", "このまま", "後で", "方針")
DECISION_KEYS = ("decision", "file", "quote", "match", "rule", "decided", "by", "revisit")
REVIEW_KEYS = ("file", "line", "priority", "quote", "impact", "recommend", "status", "note", "resolved-at", "ai-note", "id")
# Review の `- id:`（解決済みにしたときに書く、まとまりの名前を除いた id）。review.mjs の PINNED_ID と同じ形
PINNED_ID = re.compile(r"^[0-9a-z]{1,13}(?:-\d{1,4})?$")
# 人が足した選択肢の置き場（指摘のファイルと同じフォルダ）。review.mjs の ACTIONS_FILE
ACTIONS_FILE = "_actions.md"
FENCE = re.compile(r"^\s*(```|~~~)")
HEAD = re.compile(r"^## \S")
PRIORITY = re.compile(r"^\[p(\d{1,3})\]\s*(.*)$", re.I)
# 見出しの先頭の `<decision>: `。判断のファイルの見出しはこの形。
DECISION_PREFIX = re.compile(r"^(%s):\s*(.*)$" % "|".join(DECISIONS))


@dataclass
class Entry:
    """見出し 1 つぶん（判断でも指摘でも同じ形）。"""
    source: str  # 読んだファイルの名前（判断なら docs.md、指摘なら review のファイル名の stem）
    line: int  # 見出しの行（1 始まり）
    title: str
    meta: dict
    body: str
    span: tuple  # (見出しの行, 終わりの行) の 0 始まり・終わりは含まない
    priority: int | None = None
    errors: list = field(default_factory=list)

    @property
    def file(self):
        return normalize_file(self.meta.get("file", ""))

    @property
    def quote(self):
        return unquote(self.meta.get("quote", ""))


def unquote(v):
    return re.sub(r"^`+|`+$", "", v or "").strip()


def normalize_file(f):
    """書かれた file を比べる形にする（前後の ` と ./ を落とす）。判断と指摘は同じ書き方をする前提。"""
    return unquote(f).removeprefix("./")


def existing_path(root, f, prefix=None):
    """file の実体のパス。そのままで無ければ --file-prefix を前に付けて探す（review.mjs の resolveFile と同じ）。無ければ None。"""
    prefix = FILE_PREFIX if prefix is None else prefix
    for cand in [f] + ([prefix.rstrip("/") + "/" + f] if prefix else []):
        path = Path(root) / cand
        if path.is_file():
            return path
    return None


def parse(text, source, keys):
    """`## 見出し` ごとに分ける。コードフェンスの中の `## ` は見出しにしない（review.mjs の parseReview と同じ）。"""
    lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    start = 0
    if lines and lines[0].strip() == "---":
        for i in range(1, len(lines)):
            if lines[i].strip() == "---":
                start = i + 1
                break
    heads, in_fence = [], False
    for i in range(start, len(lines)):
        if FENCE.match(lines[i]):
            in_fence = not in_fence
        elif not in_fence and HEAD.match(lines[i]):
            heads.append(i)
    meta_line = re.compile(r"^- (%s):(?:\s+(.*))?$" % "|".join(re.escape(k) for k in keys))
    out = []
    for k, h in enumerate(heads):
        end = heads[k + 1] if k + 1 < len(heads) else len(lines)
        title = lines[h][3:].strip()
        priority = None
        m = PRIORITY.match(title)
        if m:
            priority, title = int(m.group(1)), m.group(2).strip()
        meta, meta_end = {}, h + 1
        for i in range(h + 1, end):
            mm = meta_line.match(lines[i])
            if not mm:
                break
            meta[mm.group(1)] = (mm.group(2) or "").strip()
            meta_end = i + 1
        body = "\n".join(lines[meta_end:end]).strip()
        out.append(Entry(source, h + 1, title, meta, body, (h, end), priority))
    return out, lines


def norm(s):
    return re.sub(r"\s+", " ", s.replace("`", "")).strip()


def quote_parts(q):
    return [p for p in re.split(r"\s*(?:…|\.\.\.)\s*", norm(q)) if p]


def quote_in(text, q):
    """text が quote を含むか。quote の `…` は省略（review.mjs の quoteMatches と同じ）。"""
    parts = quote_parts(q)
    if not parts:
        return False
    t, pos = norm(text), 0
    for p in parts:
        i = t.find(p, pos)
        if i < 0:
            return False
        pos = i + len(p)
    return True


def fnv1a36(s):
    """review.mjs の id と同じ 32bit FNV-1a（UTF-16 の単位で）を 36 進 7 桁で。"""
    h = 0x811C9DC5
    data = s.encode("utf-16-le")
    for i in range(0, len(data), 2):
        h ^= data[i] | (data[i + 1] << 8)
        h = (h * 0x01000193) & 0xFFFFFFFF
    digits = "0123456789abcdefghijklmnopqrstuvwxyz"
    out = ""
    while True:
        h, r = divmod(h, 36)
        out = digits[r] + out
        if h == 0:
            break
    return out.rjust(7, "0")


def review_ids(entries, set_name):
    """Review の id（review.mjs の parseReview と同じ規則）。

    `- id:` が書いてあればそれ（解決済みにしたときに書く。前の指摘を消してもずれない）。
    無ければ `<まとまり>/<hash>` で、同じ file と見出しが続けば `-2`…（書いてある id とは重ねない）。file は書かれたまま（省略形のまま）で数える。
    """
    pins, taken = [], set()
    for e in entries:
        v = e.meta.get("id", "")
        pin = f"{set_name}/{v}" if PINNED_ID.match(v) else None
        if pin in taken:
            pin = None
        if pin:
            taken.add(pin)
        pins.append(pin)
    ids = []
    for e, pin in zip(entries, pins):
        if not pin:
            h = f"{set_name}/{fnv1a36(unquote(e.meta.get('file', '')).removeprefix('./') + chr(10) + e.title)}"
            n = 1
            while (f"{h}-{n}" if n > 1 else h) in taken:
                n += 1
            pin = f"{h}-{n}" if n > 1 else h
            taken.add(pin)
        ids.append(pin)
    return ids


# review.mjs の labelProblem・MAX_ACTIONS と同じ決まり。
ACTION_LABEL_CHARS = 20
MAX_ACTIONS = 8
RESERVED_ACTION_LABELS = ("直さない", "直してもらう", "解決済みにする", "Feedback に送った")


def action_label_ok(label):
    return (
        0 < len(label) <= ACTION_LABEL_CHARS
        and not re.search(r"[:：\n\r]", label)
        and not re.match(r"\[p\d", label, re.I)
        and label not in RESERVED_ACTION_LABELS
    )


def load_actions(review_path):
    """指摘のファイルの隣の `_actions.md` から、`- decision:` の付いた選択肢を {名前: decision} で返す（review.mjs の parseActions と同じ読み方）。"""
    path = Path(review_path).parent / ACTIONS_FILE
    if not path.exists():
        return {}
    entries, _ = parse(path.read_text(encoding="utf-8"), path.stem, ("agent", "decision"))
    # popup に出ない選択肢（名前が読めない・同じ名前の 2 つめ・9 つめから）は、popup と同じく読まない。
    shown, out = [], {}
    for e in entries:
        # parse は `[pN]` を優先度として外すので、付いていたものは名前として読めないものに数える
        if e.priority is None and action_label_ok(e.title) and e.title not in shown and len(shown) < MAX_ACTIONS:
            shown.append(e.title)
            if e.meta.get("decision") in DECISIONS:
                out[e.title] = e.meta["decision"]
    return out


def note_choice(note, names):
    """note がどの名前で始まるかと、その後ろの理由（review.mjs の matchNote と同じ読み方）。

    popup は `名前` / `名前: 理由` と書く。手で付けた `名前（理由）`・`名前 (理由)`・`名前 理由` も理由付きとして受ける。
    名前は長いものから試す（`PR #65 待ち` と `PR` のように頭が重なっても、長い方に合わせる）。合わなければ (None, "")。
    """
    note = (note or "").strip()
    for name in sorted(names, key=len, reverse=True):
        m = re.match(r"^%s(?:\s*[:：]\s*(.*)|\s*[（(](.*)[）)]|\s+(.*))?$" % re.escape(name), note, re.S)
        if m:
            return name, next((g for g in m.groups() if g), "").strip()
    return None, ""


# ---- 判断を読む ----

def decision_title(e):
    """`## 直さない: 見出し` の「見出し」。"""
    m = DECISION_PREFIX.match(e.title)
    return m.group(2).strip() if m else e.title


def load_decisions(directory=DECISIONS_DIR):
    out = []
    for path in sorted(Path(directory).glob("*.md")):
        if path.name == "README.md":
            continue
        entries, _ = parse(path.read_text(encoding="utf-8"), path.name, DECISION_KEYS)
        for e in entries:
            d = e.meta.get("decision", "")
            if not d:
                e.errors.append("「- decision:」がありません")
            elif d not in DECISIONS:
                e.errors.append(f"decision は {' / '.join(DECISIONS)} のどれかです: {d}")
            m = DECISION_PREFIX.match(e.title)
            if m and d and m.group(1) != d:
                e.errors.append(f"見出しの「{m.group(1)}」と decision の「{d}」が違います")
            if not e.meta.get("decided"):
                e.errors.append("「- decided:」がありません（日付・回・チケット）")
            if d == "後で" and not e.meta.get("revisit"):
                e.errors.append("decision が「後で」なら「- revisit:」（見直す条件）が要ります")
            if e.meta.get("quote") and not e.meta.get("file"):
                e.errors.append("quote があるのに file がありません")
            if e.meta.get("match"):
                if not e.meta.get("file"):
                    e.errors.append("match があるのに file がありません（範囲をグロブで書く）")
                try:
                    re.compile(match_pattern(e))
                except re.error as err:
                    e.errors.append(f"match の正規表現が読めません: {err}")
            out.append(e)
    return out


def file_matches(pattern, f):
    if not pattern:
        return False
    if pattern == f:
        return True
    if any(c in pattern for c in "*?["):
        # `*` は 1 階層、`**` は階層をまたぐ（fnmatch の `*` は `/` もまたぐので使わない）
        rx = re.escape(pattern).replace(r"\*\*", "\0").replace(r"\*", "[^/]*").replace("\0", ".*").replace(r"\?", "[^/]")
        return re.fullmatch(rx, f) is not None
    return False


def match_pattern(e):
    return unquote(e.meta.get("match", ""))


def title_key(t):
    """見出しを比べる形（「（要確認）」などの添え書きと空白・記号を落とす）。"""
    t = re.sub(r"[（(](要確認|要検討)[）)]", "", norm(t))
    return re.sub(r"[\s「」『』`*_、。・:：]", "", t)


def compare(finding, decision):
    """指摘と判断を比べる。"same"（同じ）/ "near"（同じ箇所だが別の指摘かもしれない）/ None。

    - same: file が該当し、見出しが同じ。または quote が重なり、見出しも似ている（半分以上）
    - near: file が該当し、quote は重なるが見出しが似ていない（`.role {` のような短い quote で別の指摘を消さないため、
      外さずにエージェントに見せる）
    - match（`方針` で使う、見出しの正規表現）があれば、file が該当し見出しが match に合うものは same。
      見出しの言い方が回ごとに揺れる「ある種類の指摘」をまとめて外すため
    `方針` の本文の条件はここでは見ない（file・見出し・match だけ）。該当するかはエージェントが本文で判断する。
    """
    if not file_matches(decision.file, finding.file):
        return None
    pattern = match_pattern(decision)
    if pattern:
        try:
            if re.search(pattern, norm(finding.title)) or re.search(pattern, title_key(finding.title)):
                return "same"
        except re.error:
            pass
    dt, ft = title_key(decision_title(decision)), title_key(finding.title)
    if dt and dt == ft:
        return "same"
    dq, fq = decision.quote, finding.quote
    overlap = bool(dq and fq and (quote_in(fq, dq) or quote_in(dq, fq))) or bool(dq and not fq and quote_in(finding.body, dq))
    if not overlap:
        return None
    return "same" if difflib.SequenceMatcher(None, dt, ft).ratio() >= 0.5 else "near"


def same(finding, decision):
    return compare(finding, decision) == "same"


# ---- コマンド ----

def cmd_list(args):
    rows = [d for d in load_decisions(args.dir) if not args.decision or d.meta.get("decision") == args.decision]
    for d in rows:
        where = d.meta.get("file", "") or "（場所なし）"
        print(f"{d.source}:{d.line}\t{d.meta.get('decision', '?')}\t{where}\t{decision_title(d)}")
    print(f"計 {len(rows)} 件（{counts_text(rows)}）" if rows else "判断はまだありません")
    return 0


def inspect(root, directory=DECISIONS_DIR, prefix=None):
    """(判断の一覧, 問題の行, 見直す条件の行)。check が使う（導入先の準備の script からも import して使える）。"""
    root = Path(root)
    decisions = load_decisions(directory)
    problems, revisits, cache = [], [], {}
    for d in decisions:
        where = f"{d.source}:{d.line}"
        problems += [f"ERROR {where}: {err}（{d.title}）" for err in d.errors]
        f = d.file
        if f and not any(c in f for c in "*?["):
            path = existing_path(root, f, prefix)
            if path is None:
                problems.append(f"GONE  {where}: ファイルがありません: {f}（{d.title}）— 消えたなら判断も消す")
            elif d.quote:
                if f not in cache:
                    cache[f] = path.read_text(encoding="utf-8", errors="replace")
                if not quote_in(cache[f], d.quote):
                    problems.append(f"LOST  {where}: quote が見つかりません: {d.quote}（{d.title}）— 箇所が書き換わった。判断がまだ該当するか見直す")
        if d.meta.get("revisit"):
            revisits.append(f"REVISIT {where}: {d.meta['revisit']}（{d.title}）")
    return decisions, problems, revisits


def counts_text(decisions):
    counts = {}
    for d in decisions:
        k = d.meta.get("decision", "?")
        counts[k] = counts.get(k, 0) + 1
    return "・".join(f"{k} {counts[k]}" for k in (*DECISIONS, "?") if k in counts)


def cmd_check(args):
    decisions, problems, revisits = inspect(args.root, args.dir, args.file_prefix)
    for line in problems + revisits:
        print(line)
    print(f"{'NG' if problems else 'OK'} 判断 {len(decisions)} 件（{counts_text(decisions)}）/ 問題 {len(problems)} 件 / 見直す条件の付いたもの {len(revisits)} 件")
    return 1 if problems else 0


def cmd_match(args):
    path = Path(args.review)
    findings, lines = parse(path.read_text(encoding="utf-8"), path.stem, REVIEW_KEYS)
    decisions = [d for d in load_decisions(args.dir) if not d.errors]
    matched, near = [], []
    for f in findings:
        results = [(compare(f, d), d) for d in decisions]
        hit = next((d for r, d in results if r == "same"), None)
        where = f"{f.meta.get('file', '')}:{f.meta.get('line', '')}"
        if hit:
            matched.append((f, hit))
            print(f"SAME  {where}\t{f.title}\t← {hit.source}:{hit.line} {hit.meta.get('decision')}: {decision_title(hit)}")
            continue
        close = next((d for r, d in results if r == "near"), None)
        if close:
            near.append((f, close))
            print(f"NEAR  {where}\t{f.title}\t← {close.source}:{close.line} {close.meta.get('decision')}: {decision_title(close)}（同じ箇所の別の指摘かもしれない。外さない）")
    print(f"指摘 {len(findings)} 件のうち、過去の判断と同じもの {len(matched)} 件・同じ箇所の別の指摘かもしれないもの {len(near)} 件")
    if args.drop and matched:
        drop = {f.span for f, _ in matched}
        keep, i = [], 0
        for f in findings:
            if f.span in drop:
                keep.extend(lines[i:f.span[0]])
                i = f.span[1]
        keep.extend(lines[i:])
        out = Path(args.out) if args.out else path
        out.write_text("\n".join(keep).rstrip("\n") + "\n", encoding="utf-8")
        print(f"外したファイルを書きました: {out}（{len(findings) - len(matched)} 件）")
    return 0


def render_decision(f, decided, by, decision="直さない", label="直さない"):
    _, reason = note_choice(f.meta.get("note", ""), [label])
    lines = [f"## {decision}: {f.title}", f"- decision: {decision}", f"- file: {unquote(f.meta['file'])}"]
    if f.quote:
        lines.append(f"- quote: {f.quote}")
    rule = re.search(r"^- (?:rule|破っている項目|ルール):\s*(.+)$", f.body, re.M)
    if rule:
        lines.append(f"- rule: {rule.group(1).strip()}")
    lines += [f"- decided: {decided}", f"- by: {by}" + (f"（選択肢「{label}」）" if label and label != decision else "")]
    if decision == "後で":
        # 「後で」は見直す条件が要る（load_decisions が確かめる）。人のひとことをそのまま条件にし、無ければ次の回に聞く
        lines.append(f"- revisit: {reason or '次の回で人に聞く'}")
    lines.append("")
    lines.append(f"人の一言: {reason}" if reason else "人の一言: なし")
    summary = [l for l in f.body.split("\n") if l.strip() and not re.match(r"^- (?:rule|破っている項目|ルール):", l)]
    if summary:
        lines += ["", "元の指摘:", ""] + summary
    return "\n".join(lines)


def cmd_record_review(args):
    path = Path(args.review)
    findings, _ = parse(path.read_text(encoding="utf-8"), path.stem, REVIEW_KEYS)
    ids = review_ids(findings, path.stem)
    args.dir.mkdir(parents=True, exist_ok=True)
    target = args.dir / args.to
    existing = load_decisions(args.dir)
    # 「直さない」と、人が popup に足した選択肢のうち decision の付いたもの（{名前: decision}）
    choices = {"直さない": "直さない", **load_actions(path)}
    added, already, to_remove = [], 0, []
    for f, rid in zip(findings, ids):
        label, _ = note_choice(f.meta.get("note", ""), choices)
        decision = choices.get(label)
        if f.meta.get("status") != "resolved" or not decision:
            continue
        if any(same(f, d) and d.meta.get("decision") == decision and norm(decision_title(d)) == norm(f.title) for d in existing):
            already += 1
        elif any(d.meta.get("decision") == "方針" and match_pattern(d) and same(f, d) for d in existing):
            already += 1  # 方針がまとめて扱っている（1 件ずつ写さない）
        else:
            added.append(render_decision(f, args.decided, args.by, decision, label))
        to_remove.append(rid)
    if added:
        text = target.read_text(encoding="utf-8") if target.exists() else f"# {target.stem}\n"
        target.write_text(text.rstrip("\n") + "\n\n" + "\n\n".join(added) + "\n", encoding="utf-8")
    names = "・".join(f"「{k}」" for k in choices)
    print(f"{names} {len(to_remove)} 件: 写した {len(added)} 件 / 写してあった {already} 件 → {target}")
    # 後ろから消す順に出す。同じ file と見出しの指摘は `x`・`x-2` と並び順で id が振られ、`x` を先に消すと `x-2` が `x` にずれるため
    # （review.mjs で解決済みにした同じ見出しの指摘は `- id:` で固定されているのでずれないが、手で書いた解決済みは固定されていない）
    for rid in reversed(to_remove):
        print(f"REMOVE {rid}")
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--dir", type=Path, default=argparse.SUPPRESS, help="判断の置き場（既定: $FEEDBACK_DECISIONS_DIR か .claude/feedback-decisions）")
    common.add_argument("--file-prefix", default=argparse.SUPPRESS, help="file がそのままで無ければ前に付けて探すフォルダ")
    ap.add_argument("--dir", type=Path, default=DECISIONS_DIR, help=argparse.SUPPRESS)
    ap.add_argument("--file-prefix", default=FILE_PREFIX, help=argparse.SUPPRESS)
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("list", parents=[common])
    p.add_argument("--decision", choices=DECISIONS)
    p.set_defaults(func=cmd_list)
    p = sub.add_parser("check", parents=[common])
    p.add_argument("--root", default=".", help="リポジトリの直下（既定: いまの場所）")
    p.set_defaults(func=cmd_check)
    p = sub.add_parser("match", parents=[common])
    p.add_argument("review")
    p.add_argument("--drop", action="store_true")
    p.add_argument("--out")
    p.set_defaults(func=cmd_match)
    p = sub.add_parser("record-review", parents=[common])
    p.add_argument("review")
    p.add_argument("--decided", required=True, help="判断した日と回（例: 2026-11-10 release-check-01 #123）")
    p.add_argument("--by", default="人（Review）")
    p.add_argument("--to", default="decisions.md", help="書き足すファイル（--dir の中。既定: decisions.md）")
    p.set_defaults(func=cmd_record_review)
    args = ap.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
