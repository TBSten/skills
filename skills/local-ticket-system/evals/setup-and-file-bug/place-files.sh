#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
# .gitignore is stored as gitignore so it does not affect the skills repo itself.
mv gitignore .gitignore
