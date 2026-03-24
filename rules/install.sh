#!/bin/bash
set -euo pipefail

REPO="tbsten/skills"
BRANCH="main"
BASE_URL="https://raw.githubusercontent.com/${REPO}/${BRANCH}"
API_URL="https://api.github.com/repos/${REPO}/contents"

usage() {
  echo "Usage: curl -fsSL ${BASE_URL}/rules/install.sh | bash -s -- <rule-name> [install-name]"
  echo ""
  echo "Installs a rule into .claude/rules/ and downloads reference files."
  echo "If install-name is specified, the rule is saved as .claude/rules/<install-name>.md"
  exit 1
}

rule_name=""
install_name=""

for arg in "$@"; do
  case "$arg" in
    as=*)
      install_name="${arg#as=}"
      ;;
    *)
      rule_name="$arg"
      ;;
  esac
done

if [ -z "$rule_name" ]; then
  echo "Error: rule name is required."
  usage
fi

install_name="${install_name:-$rule_name}"
rule_dir="rules/${rule_name}"

echo "Installing rule: ${rule_name} as ${install_name} ..."

# Download RULE.md -> .claude/rules/<install-name>.md
mkdir -p .claude/rules
echo "  -> .claude/rules/${install_name}.md"
curl -fsSL "${BASE_URL}/${rule_dir}/RULE.md" -o ".claude/rules/${install_name}.md"

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

echo "Done! Rule '${install_name}' installed."
