#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
git init -q
git add -A
git -c user.name=eval -c user.email=eval@example.com commit -qm "initial"
