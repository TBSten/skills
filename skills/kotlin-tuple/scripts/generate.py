#!/usr/bin/env python3
"""Parametric generator for the kotlin-tuple skill.

Generates Tuple.kt / TupleFactory.kt / TupleToList.kt / AbstractTupleSerializer.kt /
TupleSerializer.kt / AwaitAll.kt / AllNotNullOrNull.kt for any max Tuple size N,
byte-identical to example/ when run with --package com.example.tuple --max 20
(verified by --self-test).

Standard library only. Do NOT edit generated output by hand — rerun with new args.
"""
import argparse
import json
import os
import re
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
EXAMPLE_DIR = os.path.normpath(os.path.join(
    SCRIPT_DIR, "..", "example", "src", "commonMain", "kotlin", "com", "example", "tuple",
))
EXAMPLE_PACKAGE = "com.example.tuple"
EXAMPLE_MAX = 20
MIN_MAX, MAX_MAX = 4, 99

sys.path.insert(0, SCRIPT_DIR)  # allow running from any cwd
from tuple_templates import ALL_PARTS, REQUIRED_PARTS, render  # noqa: E402

# ---------------------------------------------------------------- preflight

def scan_existing(search_root, out_dir):
    """Finds pre-existing Tuple definitions / tuple directories under search_root."""
    matches = []
    decl_re = re.compile(r"(?:data class|data object|typealias)\s+Tuple\d")
    if not os.path.isdir(search_root):
        return matches
    for root, dirs, files in os.walk(search_root):
        dirs[:] = [d for d in dirs if d not in (".git", "build", ".gradle")]
        for d in dirs:
            if d.lower().startswith("tuple"):
                matches.append({"path": os.path.join(root, d), "kind": "directory"})
        for f in files:
            if not f.endswith(".kt"):
                continue
            path = os.path.join(root, f)
            try:
                with open(path, encoding="utf-8", errors="replace") as fh:
                    if decl_re.search(fh.read()):
                        matches.append({"path": path, "kind": "tuple-definition"})
            except OSError:
                continue
    out_abs = os.path.abspath(out_dir)
    for m in matches:
        m["insideOut"] = os.path.abspath(m["path"]).startswith(out_abs + os.sep) \
            or os.path.abspath(m["path"]) == out_abs
    return matches


def default_search_root(out_dir):
    """The nearest ancestor `src` directory of out_dir, else out_dir itself."""
    parts = os.path.abspath(out_dir).split(os.sep)
    for i in range(len(parts) - 1, 0, -1):
        if parts[i] == "src":
            return os.sep.join(parts[: i + 1])
    return out_dir


# ---------------------------------------------------------------- self-test

def self_test():
    if not os.path.isdir(EXAMPLE_DIR):
        fail("example directory not found: %s" % EXAMPLE_DIR,
             "the skill's example/ tree is the golden reference for --self-test",
             "run this script from its original location inside the kotlin-tuple skill")
    generated = render(EXAMPLE_PACKAGE, EXAMPLE_MAX, ALL_PARTS)
    mismatches = []
    for filename, content in generated.items():
        golden_path = os.path.join(EXAMPLE_DIR, filename)
        if not os.path.isfile(golden_path):
            mismatches.append((filename, "golden file missing: %s" % golden_path))
            continue
        with open(golden_path, encoding="utf-8", newline="") as fh:
            golden = fh.read()
        if content != golden:
            detail = "content differs"
            gen_lines, gold_lines = content.splitlines(), golden.splitlines()
            for i, (g1, g2) in enumerate(zip(gen_lines, gold_lines), start=1):
                if g1 != g2:
                    detail = "first diff at line %d:\n  generated: %r\n  golden:    %r" % (i, g1, g2)
                    break
            else:
                detail = ("line count differs: generated=%d golden=%d"
                          % (len(gen_lines), len(gold_lines)))
            mismatches.append((filename, detail))
    if mismatches:
        for filename, detail in mismatches:
            sys.stderr.write("SELF-TEST MISMATCH: %s\n%s\n" % (filename, detail))
        sys.stderr.write(
            "WHY: generator output is not byte-identical to example/ (the golden SSoT)\n"
            "FIX: fix the templates in tuple_templates.py (never edit example/) until --self-test passes\n")
        print(json.dumps({"ok": False, "status": "self-test-failed",
                          "mismatches": [m[0] for m in mismatches]}))
        return 4
    print(json.dumps({"ok": True, "status": "self-test-passed",
                      "files": sorted(generated.keys())}))
    return 0


# ---------------------------------------------------------------- cli

def fail(what, why, fix, code=1):
    sys.stderr.write("ERROR: %s\nWHY: %s\nFIX: %s\n" % (what, why, fix))
    sys.exit(code)


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Generate kotlin-tuple utility files for any max Tuple size N.",
        epilog="Example: generate.py --package com.foo.tuple --max 12 "
               "--out shared/src/commonMain/kotlin/com/foo/tuple")
    parser.add_argument("--package", dest="package", help="Kotlin package name (e.g. com.foo.tuple)")
    parser.add_argument("--max", dest="max_n", type=int,
                        help="max Tuple size N (%d-%d, default in SKILL.md: 20)" % (MIN_MAX, MAX_MAX))
    parser.add_argument("--out", dest="out",
                        help="output package directory (e.g. shared/src/commonMain/kotlin/com/foo/tuple)")
    parser.add_argument("--parts", default=",".join(ALL_PARTS),
                        help="comma-separated: %s (default: all; tuple,factory are always included)"
                             % ",".join(ALL_PARTS))
    parser.add_argument("--search-root", dest="search_root",
                        help="directory scanned for pre-existing Tuple definitions "
                             "(default: nearest 'src' ancestor of --out)")
    parser.add_argument("--force", action="store_true",
                        help="overwrite existing output files")
    parser.add_argument("--ignore-existing", action="store_true",
                        help="proceed even if pre-existing Tuple definitions are detected elsewhere")
    parser.add_argument("--dry-run", action="store_true",
                        help="print the file list JSON without writing anything")
    parser.add_argument("--self-test", action="store_true",
                        help="verify generated output is byte-identical to example/ (golden test)")
    args = parser.parse_args(argv)

    if args.self_test:
        sys.exit(self_test())

    if not args.package or args.max_n is None or not args.out:
        fail("--package, --max and --out are required (unless --self-test)",
             "the generator cannot infer the target project layout",
             "pass e.g. --package com.foo.tuple --max 20 --out shared/src/commonMain/kotlin/com/foo/tuple")
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*", args.package):
        fail("invalid package name: %r" % args.package,
             "it must be dot-separated Kotlin identifiers",
             "use something like com.example.tuple")
    if not (MIN_MAX <= args.max_n <= MAX_MAX):
        fail("--max %d is out of range" % args.max_n,
             "supported range is %d-%d (Tuple0-Tuple3 are always emitted; "
             "ordinal names are defined up to %d)" % (MIN_MAX, MAX_MAX, MAX_MAX),
             "pass --max between %d and %d" % (MIN_MAX, MAX_MAX))

    requested = [p.strip() for p in args.parts.split(",") if p.strip()]
    unknown = [p for p in requested if p not in ALL_PARTS]
    if unknown:
        fail("unknown part(s): %s" % ", ".join(unknown),
             "valid parts are: %s" % ", ".join(ALL_PARTS),
             "fix the --parts value")
    missing_required = [p for p in REQUIRED_PARTS if p not in requested]
    if missing_required:
        sys.stderr.write("note: %s are always generated (required by the other parts)\n"
                         % " and ".join(missing_required))
    parts = [p for p in ALL_PARTS if p in set(requested) | set(REQUIRED_PARTS)]

    out_dir = args.out
    pkg_path = args.package.replace(".", "/")
    if not os.path.abspath(out_dir).replace(os.sep, "/").endswith("/" + pkg_path):
        sys.stderr.write("warning: --out %r does not end with the package path %r; "
                         "double-check the output directory\n" % (out_dir, pkg_path))

    search_root = args.search_root or default_search_root(out_dir)
    existing = scan_existing(search_root, out_dir)
    blocking = [m for m in existing if not m["insideOut"]]
    if blocking and not args.ignore_existing:
        sys.stderr.write(
            "ACTION_REQUIRED: pre-existing Tuple definitions detected under %s\n"
            "WHY: generating another Tuple set may cause duplicate/conflicting declarations\n"
            "FIX: ask the user whether to (a) keep both and rerun with --ignore-existing, "
            "(b) use a different package/--out, or (c) cancel\n" % search_root)
        for m in blocking:
            sys.stderr.write("  - %s (%s)\n" % (m["path"], m["kind"]))
        print(json.dumps({"ok": False, "status": "ACTION_REQUIRED",
                          "reason": "existing-tuple-definitions",
                          "searchRoot": os.path.abspath(search_root),
                          "existing": existing}))
        sys.exit(3)

    files = render(args.package, args.max_n, parts)
    result = {
        "ok": True,
        "status": "dry-run" if args.dry_run else "generated",
        "package": args.package,
        "max": args.max_n,
        "out": os.path.abspath(out_dir),
        "parts": parts,
        "files": [os.path.abspath(os.path.join(out_dir, f)) for f in files],
        "existing": existing,
    }
    if args.dry_run:
        print(json.dumps(result))
        return

    conflicts = [f for f in files if os.path.exists(os.path.join(out_dir, f))]
    if conflicts and not args.force:
        fail("output files already exist: %s" % ", ".join(conflicts),
             "refusing to overwrite without --force (idempotency guard)",
             "rerun with --force to overwrite, or choose another --out",
             code=2)
    os.makedirs(out_dir, exist_ok=True)
    for filename, content in files.items():
        with open(os.path.join(out_dir, filename), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(content)
    print(json.dumps(result))


if __name__ == "__main__":
    main()
