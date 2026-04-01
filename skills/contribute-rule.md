# contribute-rule

A Claude Code skill for contributing project knowledge to the [TBSten/skills](https://github.com/TBSten/skills) repository as a rule.

## Install

```sh
npx skills add tbsten/skills \
  --skill contribute-rule
```

## Overview

This skill automates the process of packaging project-specific conventions, best practices, and guidelines into a reusable Claude Code rule and creating a pull request to the TBSten/skills repository.

## Usage

After installing the skill, ask Claude to contribute knowledge from your current project as a rule:

```
この規約をルールとして登録して: Kotlin のコーディング規約
```

```
contribute rule: このプロジェクトのコミットメッセージルール
```

## What it does

1. **Collects knowledge** from the current project (CLAUDE.md, rules, codebase)
2. **Organizes** the knowledge into a rule format (RULE.md)
3. **Clones** TBSten/skills to a temp directory
4. **Creates** rule files following the repository's conventions
5. **Creates a PR** against TBSten/skills with all required files

## Requirements

- `git` installed
- `gh` CLI installed and authenticated (`gh auth login`)
- Write access to the TBSten/skills repository (or a fork)
