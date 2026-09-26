#!/usr/bin/env python3
"""Check (and optionally fix) NG words in a release note.

Usage:
    python3 ng_words.py <file.md>        # report only (exit code 1 if any NG word is found)
    python3 ng_words.py <file.md> --fix  # replace the words that have a replacement, write back

Add a word to NG_WORDS every time a review (subagent or human) points out an expression
that users do not understand. Use None as the replacement when the right wording depends
on context; those are only reported, to be rephrased by hand.
"""
import sys

# NG expression -> replacement (None = context dependent, rephrase by hand).
NG_WORDS = {
    # Implementer vocabulary: describe what changes for the user instead.
    "refactor": None,  # users do not care; describe the visible effect, or leave it to the PR list
    "internal API": None,  # say which public API changed, or drop the line
    "codepath": None,
    "code path": None,
    # Hype words: state what changed, calmly.
    "blazingly": None,
    "revolutionary": None,
    "目玉": None,  # 「主要な変更」「ハイライト」等にする
    # Vague words: say what exactly was fixed.
    "various fixes": None,
    "some fixes": None,
    "いろいろ修正": None,
}


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    path = sys.argv[1]
    fix = "--fix" in sys.argv[2:]

    with open(path, encoding="utf-8") as f:
        text = f.read()

    found = 0
    for word, replacement in NG_WORDS.items():
        lowered_word = word.lower()
        for lineno, line in enumerate(text.splitlines(), 1):
            if lowered_word in line.lower():
                found += 1
                if replacement is None:
                    action = "rephrase by hand"
                elif fix:
                    action = f"replaced with '{replacement}'"
                else:
                    action = f"replaceable with '{replacement}' (--fix)"
                print(f"{path}:{lineno}: NG '{word}' -> {action}")
        if fix and replacement is not None:
            text = text.replace(word, replacement)

    if fix:
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)

    if found == 0:
        print(f"OK {path}: no NG words")
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
