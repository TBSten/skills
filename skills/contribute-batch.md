# contribute-batch

A Claude Code skill for triaging multiple pieces of project knowledge into skills / rules / prompts and contributing them to the [TBSten/skills](https://github.com/TBSten/skills) repository in a single pull request.

## Install

```sh
gh skill install tbsten/skills contribute-batch
```

## Overview

This skill acts as an orchestrator: it collects multiple pieces of knowledge from the current project, triages each one into skill / rule / prompt (or skips it), creates the artifacts in parallel with subagents, and bundles everything into one pull request to the TBSten/skills repository. Use contribute-skill / contribute-rule / contribute-prompt instead when you only need a single artifact of a known type.

## Usage

After installing the skill, ask Claude to contribute a batch of knowledge from your current project:

```
知見をまとめて contribute して: このプロジェクトのセットアップ手順と運用規約
```

```
contribute batch: CLAUDE.md と .claude/rules/ の知見を仕分けして PR にまとめたい
```

## What it does

1. **Collects knowledge** from the current project (CLAUDE.md, rules, skills, codebase)
2. **Triages** each piece into skill / rule / prompt (or skip) and confirms the plan with you
3. **Prepares a workspace** via the bundled `scripts/setup-workspace.sh` (clone + working branch, idempotent)
4. **Creates artifacts in parallel** with subagents (max 5), each following the repository's contribute guides
5. **Integrates** the results: updates the README tables, runs self-reviews, commits in meaningful units, and creates **one PR**

## Requirements

- `git` installed
- `gh` CLI installed and authenticated (`gh auth login`)
- Write access to the TBSten/skills repository (or a fork)
