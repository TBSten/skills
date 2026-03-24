#!/bin/bash
set -euo pipefail

REPO="tbsten/skills"
BRANCH="main"
BASE_URL="https://raw.githubusercontent.com/${REPO}/${BRANCH}"
API_URL="https://api.github.com/repos/${REPO}/contents"

usage() {
  echo "Usage: curl -fsSL ${BASE_URL}/rules/install.sh | bash -s -- <rule-name>"
  echo ""
  echo "Installs a rule into .claude/rules/ and downloads reference files."
  exit 1
}

rule_name="${1:-}"
if [ -z "$rule_name" ]; then
  echo "Error: rule name is required."
  usage
fi

rule_dir="rules/${rule_name}"

echo "Installing rule: ${rule_name} ..."

# Download RULE.md -> .claude/rules/<rule-name>.md
mkdir -p .claude/rules
echo "  -> .claude/rules/${rule_name}.md"
curl -fsSL "${BASE_URL}/${rule_dir}/RULE.md" -o ".claude/rules/${rule_name}.md"

# Recursively list and download files from a GitHub directory
download_dir() {
  local api_path="$1"
  local entries
  entries=$(curl -fsSL "${API_URL}/${api_path}")

  echo "$entries" | grep '"path"\|"type"' | paste - - | while read -r line; do
    local path type
    path=$(echo "$line" | sed 's/.*"path": *"//;s/".*//')
    type=$(echo "$line" | sed 's/.*"type": *"//;s/".*//')

    if [ "$type" = "dir" ]; then
      download_dir "$path"
    else
      local filename
      filename=$(basename "$path")
      if [ "$filename" = "RULE.md" ]; then
        continue
      fi

      local relative_path="${path#${rule_dir}/}"
      local dest_dir
      dest_dir=$(dirname "$relative_path")
      if [ "$dest_dir" != "." ]; then
        mkdir -p "$dest_dir"
      fi

      echo "  -> ${relative_path}"
      curl -fsSL "${BASE_URL}/${path}" -o "$relative_path"
    fi
  done
}

# Download reference files recursively (everything except RULE.md)
download_dir "$rule_dir"

echo "Done! Rule '${rule_name}' installed."
