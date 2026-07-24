#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from pathlib import Path

ISSUE_URL = "https://github.com/TBSten/actions-test/issues/new"
ATTACHMENT_URL_RE = re.compile(
    r"https://(?:github\.com/user-attachments|"
    r"private-user-images\.githubusercontent\.com)/[^\s)]+"
)


def output(payload: dict) -> None:
    print(json.dumps(payload, ensure_ascii=False, sort_keys=True))


def fail(status: str, message: str, *, diagnostic: str | None = None) -> int:
    payload = {
        "ok": False,
        "status": status,
        "error": message,
        "issue_created": False,
    }
    if diagnostic:
        payload["diagnostic"] = diagnostic
    output(payload)
    return 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Upload files to a GitHub issue draft and print attachment URLs."
    )
    parser.add_argument(
        "--format",
        choices=("direct_url", "markdown"),
        default="direct_url",
        dest="output_format",
    )
    parser.add_argument(
        "--login-timeout",
        type=int,
        default=300,
        help="Seconds to wait for manual GitHub login.",
    )
    parser.add_argument(
        "--upload-timeout",
        type=int,
        default=90,
        help="Seconds to wait for each upload.",
    )
    parser.add_argument("--preflight", action="store_true", help=argparse.SUPPRESS)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("files", nargs="*")
    return parser.parse_args()


def visible_first(locator):
    for index in range(min(locator.count(), 20)):
        candidate = locator.nth(index)
        try:
            if candidate.is_visible():
                return candidate
        except Exception:
            continue
    return None


def find_body(page):
    selectors = (
        'textarea[name="issue[body]"]',
        'textarea[aria-label*="Markdown"]',
        'textarea[placeholder*="description" i]',
        "textarea",
    )
    for selector in selectors:
        candidate = visible_first(page.locator(selector))
        if candidate is not None:
            return candidate
    return None


def open_blank_issue_if_needed(page) -> bool:
    patterns = (
        re.compile(r"open a blank issue", re.I),
        re.compile(r"blank issue", re.I),
        re.compile(r"空の.*issue", re.I),
    )
    for pattern in patterns:
        candidate = visible_first(page.get_by_text(pattern))
        if candidate is None:
            continue
        candidate.click()
        page.wait_for_load_state("domcontentloaded")
        return True
    return False


def wait_for_body(page, timeout_seconds: int):
    deadline = time.monotonic() + timeout_seconds
    login_notice_sent = False
    last_template_attempt = 0.0

    while time.monotonic() < deadline:
        body = find_body(page)
        if body is not None:
            return body

        now = time.monotonic()
        if now - last_template_attempt >= 2:
            try:
                open_blank_issue_if_needed(page)
            except Exception:
                pass
            last_template_attempt = now

        if not login_notice_sent:
            print(
                "ACTION_REQUIRED: Playwright のブラウザーで GitHub にサインインしてください。",
                file=sys.stderr,
                flush=True,
            )
            login_notice_sent = True

        page.wait_for_timeout(1000)
    return None


def set_file(page, body, file_path: str) -> None:
    roots = []
    form = body.locator("xpath=ancestor::form[1]")
    if form.count() > 0:
        roots.append(form)
    roots.append(page)

    last_error = None
    for root in roots:
        inputs = root.locator('input[type="file"]')
        for index in reversed(range(inputs.count())):
            try:
                inputs.nth(index).set_input_files(file_path, timeout=5000)
                return
            except Exception as exc:
                last_error = exc

    attach_patterns = (
        re.compile(r"paste, drop, or click to add files", re.I),
        re.compile(r"attach files", re.I),
        re.compile(r"ファイル.*添付", re.I),
    )
    for pattern in attach_patterns:
        target = visible_first(page.get_by_text(pattern))
        if target is None:
            continue
        try:
            with page.expect_file_chooser(timeout=5000) as chooser_info:
                target.click()
            chooser_info.value.set_files(file_path)
            return
        except Exception as exc:
            last_error = exc

    detail = f": {last_error}" if last_error else ""
    raise RuntimeError(f"GitHub のファイル添付入力を見つけられません{detail}")


def added_markdown(before: str, after: str) -> str:
    if after.startswith(before):
        return after[len(before) :].strip()
    if before and before in after:
        return after.replace(before, "", 1).strip()
    return after.strip()


def wait_for_upload(body, before: str, timeout_seconds: int) -> str:
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        latest = body.input_value()
        uploading = "<!-- Uploading " in latest
        if latest != before and not uploading:
            return latest
        time.sleep(0.5)
    raise TimeoutError(f"アップロードが {timeout_seconds} 秒以内に完了しませんでした")


def runtime_root() -> Path:
    configured = os.environ.get("GITHUB_ATTACHMENT_RUNTIME_ROOT")
    if configured:
        return Path(configured).expanduser().resolve()
    cache_home = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache"))
    return cache_home / "github-get-attachment-url"


def save_diagnostic(page, name: str) -> str | None:
    try:
        diagnostic_dir = runtime_root() / "diagnostics"
        diagnostic_dir.mkdir(parents=True, exist_ok=True)
        path = diagnostic_dir / f"{int(time.time())}-{name}.png"
        page.screenshot(path=str(path), full_page=True)
        return str(path)
    except Exception:
        return None


def run_self_test() -> int:
    before = "existing"
    after = (
        "existing\n"
        "[demo.txt](https://github.com/user-attachments/files/123/demo.txt)"
    )
    markdown = added_markdown(before, after)
    urls = ATTACHMENT_URL_RE.findall(markdown)
    expected = "https://github.com/user-attachments/files/123/demo.txt"
    if markdown.startswith("[demo.txt]") and urls == [expected]:
        output({"ok": True, "status": "self_test_passed"})
        return 0
    return fail("self_test_failed", "Markdown 差分または URL 抽出に失敗しました")


def main() -> int:
    args = parse_args()
    if args.self_test:
        return run_self_test()
    if not args.files:
        return fail("invalid_input", "1 件以上のファイルパスが必要です")

    paths = [Path(value).expanduser().resolve() for value in args.files]
    invalid = [str(path) for path in paths if not path.is_file()]
    if invalid:
        return fail(
            "invalid_input",
            "存在しないか通常ファイルではありません: " + ", ".join(invalid),
        )
    if args.preflight:
        return 0

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        return fail(
            "setup_required",
            "Playwright が未インストールです。scripts/run.sh を使用してください。",
        )

    root = runtime_root()
    profile_dir = root / "profile"
    profile_dir.mkdir(parents=True, exist_ok=True)
    context = None
    page = None

    try:
        with sync_playwright() as playwright:
            context = playwright.chromium.launch_persistent_context(
                str(profile_dir),
                headless=False,
                no_viewport=True,
            )
            page = context.pages[0] if context.pages else context.new_page()
            page.goto(ISSUE_URL, wait_until="domcontentloaded", timeout=30000)
            body = wait_for_body(page, args.login_timeout)
            if body is None:
                diagnostic = save_diagnostic(page, "login-timeout")
                context.close()
                return fail(
                    "login_timeout",
                    "GitHub の Issue 作成画面を確認できませんでした",
                    diagnostic=diagnostic,
                )

            results = []
            for path in paths:
                before = body.input_value()
                try:
                    set_file(page, body, str(path))
                except Exception as exc:
                    diagnostic = save_diagnostic(page, "ui-changed")
                    context.close()
                    return fail("ui_changed", str(exc), diagnostic=diagnostic)

                try:
                    after = wait_for_upload(body, before, args.upload_timeout)
                except TimeoutError as exc:
                    diagnostic = save_diagnostic(page, "upload-timeout")
                    context.close()
                    return fail("upload_timeout", str(exc), diagnostic=diagnostic)

                markdown = added_markdown(before, after)
                urls = ATTACHMENT_URL_RE.findall(markdown)
                if not urls:
                    diagnostic = save_diagnostic(page, "url-missing")
                    context.close()
                    return fail(
                        "upload_failed",
                        f"{path.name} の添付 URL を取得できませんでした",
                        diagnostic=diagnostic,
                    )
                results.append(
                    {
                        "file": path.name,
                        "url": urls[0],
                        "markdown": markdown,
                    }
                )

            context.close()
    except Exception as exc:
        diagnostic = save_diagnostic(page, "unexpected") if page is not None else None
        if context is not None:
            try:
                context.close()
            except Exception:
                pass
        return fail("unexpected_error", str(exc), diagnostic=diagnostic)

    if args.output_format == "direct_url":
        for result in results:
            result.pop("markdown", None)
    else:
        for result in results:
            result.pop("url", None)

    output(
        {
            "ok": True,
            "status": "completed",
            "results": results,
            "issue_created": False,
        }
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
