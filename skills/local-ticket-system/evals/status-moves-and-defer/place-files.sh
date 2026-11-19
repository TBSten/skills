#!/usr/bin/env bash
set -eu
src="$(dirname "$0")/files"
cp -R "$src/." .
# .local/ and .gitignore are ignored by the skills repo itself, so they are stored under seed/ and renamed here.
mkdir -p .local
mv seed/ticket .local/ticket
mkdir -p .local/ticket/done .local/ticket/closed .local/ticket/archived .local/ticket/deferred
mv seed/gitignore .gitignore
rmdir seed
