#!/usr/bin/env bash
set -eu
src="$(dirname "$0")/files"
cp -R "$src/." .
mv seed/gitignore .gitignore
# Build a small git repo: main + stacked feat/login -> feat/login-ui, and feat/payment.
g() { git -c user.name=dev -c user.email=dev@example.com "$@"; }
git init -q -b main
g add README.md .gitignore
g commit -q -m "init"
git checkout -q -b feat/login
echo "login" > login.txt; g add login.txt; g commit -q -m "feat: login api"
git checkout -q -b feat/login-ui
echo "login ui" > login-ui.txt; g add login-ui.txt; g commit -q -m "feat: login screen"
git checkout -q main
git checkout -q -b feat/payment
echo "payment" > payment.txt; g add payment.txt; g commit -q -m "wip: payment"
git checkout -q main
# Previous status-board snapshot (only in some cases). Stored under seed/ because .local/ is ignored by the skills repo.
if [ -d seed/status-board ]; then
  mkdir -p .local/status-board
  mv seed/status-board/* .local/status-board/
  rmdir seed/status-board
fi
rmdir seed
