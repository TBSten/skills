# contribute-prompt

A Claude Code skill for contributing project knowledge to the [TBSten/skills](https://github.com/TBSten/skills) repository as a one-shot prompt.

## Install

```sh
gh skill install tbsten/skills contribute-prompt
```

## Overview

This skill automates packaging project-specific knowledge into a one-shot prompt — a Markdown instruction file distributed via a raw URL that users paste into Claude Code, with no skill installation required — and creating a pull request to the TBSten/skills repository. Best suited for setup and scaffolding steps that run once to completion.

## Usage

After installing the skill, ask Claude to contribute knowledge from your current project:

```
知見をプロンプトとして登録して: この CI セットアップ手順
```

```
contribute prompt: このスキャフォールド手順をワンショットプロンプトにしたい
```

## What it does

1. **Collects knowledge** from the current project (CLAUDE.md, rules, skills, codebase)
2. **Organizes** the knowledge into a self-contained one-shot prompt
3. **Clones** TBSten/skills to a temp directory
4. **Creates** `prompts/<name>/PROMPT.md`, detail docs, and README rows following the repository's conventions
5. **Creates a PR** against TBSten/skills and validates its title/body format

## Requirements

- `git` installed
- `gh` CLI installed and authenticated (`gh auth login`)
- Write access to the TBSten/skills repository (or a fork)
