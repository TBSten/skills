#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
g() { git -c user.name=dev -c user.email=dev@example.com "$@"; }
git init -q -b main
g add -A
g commit -q -m "init"
