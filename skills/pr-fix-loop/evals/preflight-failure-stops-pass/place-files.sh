#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
git init -q -b feat/tuple5
git add -A
git -c user.name=eval -c user.email=eval@example.com commit -qm "feat: tuple5"
git remote add origin https://github.com/example-org/kotlin-tuple-ext.git
# ユーザーの作業中の変更 (未コミット)
printf '\n// WIP: Tuple5\n' >> src/main/kotlin/Tuple.kt
