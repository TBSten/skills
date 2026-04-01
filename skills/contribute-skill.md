# contribute-skill

A Claude Code skill for contributing project knowledge to the [TBSten/skills](https://github.com/TBSten/skills) repository.

## Install

```sh
npx skills add tbsten/skills \
  --skill contribute-skill
```

## Overview

This skill automates the process of packaging project-specific knowledge, patterns, and workflows into a reusable Claude Code skill and creating a pull request to the TBSten/skills repository.

## Usage

After installing the skill, ask Claude to contribute knowledge from your current project:

```
知見をスキルリポジトリに登録して: Kotlin Coroutines のエラーハンドリングパターン
```

```
contribute skill: このプロジェクトのテスト戦略をスキル化したい
```

## What it does

1. **Collects knowledge** from the current project (CLAUDE.md, rules, skills, codebase)
2. **Organizes** the knowledge into a structured skill format
3. **Clones** TBSten/skills to a temp directory
4. **Creates** skill files following the repository's conventions
5. **Creates a PR** against TBSten/skills with all required files

## Requirements

- `git` installed
- `gh` CLI installed and authenticated (`gh auth login`)
- Write access to the TBSten/skills repository (or a fork)
