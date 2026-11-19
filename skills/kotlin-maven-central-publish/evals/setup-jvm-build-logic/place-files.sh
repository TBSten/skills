#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
git init -q
git config user.name "Fuga Taro"
git config user.email "fuga@example.com"
git remote add origin git@github.com:fuga/tiny-csv.git
