# github-get-attachment-url

Upload local files to GitHub and get their `user-attachments` URLs — without creating an issue.

> [!WARNING]
> **Archived (2026-09).** GitHub CLI v2.99.0 added a repeatable `--attach` flag (`gh issue|pr create/edit/comment --attach <file>`), which covers this skill's primary use case natively — prefer it. This skill remains only for what `--attach` does not do: uploading documents/archives (PDF, zip, …) and getting a `user-attachments` URL without posting anything.

## Install

```sh
gh skill install tbsten/skills github-get-attachment-url
```

## Overview

This skill turns local files (images, videos, documents, archives, or anything GitHub accepts as an attachment) into publicly reachable GitHub-hosted URLs. It does this by driving a bundled, deterministic Python + Playwright runner that opens a GitHub **issue draft**, attaches the files, and reads back the generated `user-attachments` URL or Markdown. **The issue is never submitted** — only the draft's upload side effect is used.

The model does **not** improvise browser automation. It only runs the bundled runner, so results are reproducible and the workflow does not drift with page changes the way ad-hoc automation does.

## When to use

- "Upload this file to GitHub and give me the URL"
- "I need an attachment URL for this image / video / PDF"
- "Turn this local screenshot into a GitHub-hosted link"
- "Give me the Markdown for embedding this file"
- "Get a user-attachments URL without opening an issue"

## How it works

1. `scripts/run.sh` locates Python 3.11+ and, on first run, installs Playwright `1.61.0` + Chromium into a dedicated per-user cache (`$XDG_CACHE_HOME/github-get-attachment-url`). Installation only happens when `--allow-install` is passed, so the first run reports `setup_required` and asks for confirmation.
2. `scripts/upload.py` launches a visible Chromium against `https://github.com/TBSten/actions-test/issues/new`, waits up to 5 minutes for a manual GitHub sign-in (persisted in a local profile for later runs), attaches each file to the issue draft, and waits for GitHub to finish uploading.
3. Only the JSON printed to stdout is interpreted — never the on-screen browser state.

## Usage

```sh
"$SKILL_ROOT/scripts/run.sh" --format direct_url "/absolute/path/to/file"
```

- `--format direct_url` (default) returns the raw attachment URL; `--format markdown` returns the Markdown snippet.
- Pass one or more absolute file paths, each as its own quoted argument.
- Add `--allow-install` (as the first argument) once, when the runner reports `setup_required`.

## Output (stdout JSON)

| status | Meaning |
|---|---|
| `ok=true` (`status=completed`) | `results[]` holds `{ file, url }` or `{ file, markdown }`. `issue_created` is always `false`. |
| `setup_required` | First-time install of Playwright + Chromium is needed. Re-run with `--allow-install`. |
| `python_required` | Python 3.11+ is not available. |
| `login_timeout` | Sign in to GitHub in the opened browser, then re-run. |
| `ui_changed` / `upload_timeout` / `upload_failed` | The GitHub UI or upload failed; a `diagnostic` screenshot path may be included. |

## Prerequisites

- Python 3.11 or newer
- A GitHub account you can sign in to interactively (a real browser window opens on first use / when the session expires)
- Network access to install Playwright + Chromium on first run

## Safety

- Uploading a file sends it to an external service. Confirm the target files with the user before running unless they have clearly asked to upload them.
- Never upload files that may contain secrets, credentials, PII, or financial/medical/legal/HR data without explicit approval.
- The runner never creates an issue. Include `issue_created: false` in the completion report.

## Self-test

The runner ships a dependency-free self-test that verifies the Markdown-diff and URL-extraction logic:

```sh
sh scripts/run.sh --self-test   # -> {"ok": true, "status": "self_test_passed"}
```
