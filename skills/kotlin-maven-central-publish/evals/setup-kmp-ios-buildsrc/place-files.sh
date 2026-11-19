#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
git init -q
git config user.name "hoge-dev"
git config user.email "hoge-dev@example.com"
git remote add origin https://github.com/hoge-dev/kvstore.git
