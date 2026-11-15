#!/bin/sh
# Checks scripts/release-notes.sh against a sample changelog.
set -eu

here=$(cd "$(dirname "$0")" && pwd)
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT
export CHANGELOG="$dir/CHANGELOG.md"
cat > "$CHANGELOG" <<'SAMPLE'
# Changelog

## [Unreleased]

- next

## [1.0.0-rc.1]

- candidate

## [1.0.0]

- first

### Fixed

- second

## [0.9.0]

- old
SAMPLE

failures=0
expect() { # description, expected, actual
  if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected [$2] got [$3]"; failures=$((failures + 1)); fi
}

expect "a version's own section, to the next heading" \
  "$(printf -- '- first\n\n### Fixed\n\n- second')" "$("$here/release-notes.sh" 1.0.0)"
expect "a leading v is accepted" "- old" "$("$here/release-notes.sh" v0.9.0)"
expect "a pre-release with its own section uses it" "- candidate" "$("$here/release-notes.sh" 1.0.0-rc.1)"
expect "a pre-release without a section falls back to Unreleased" "- next" "$("$here/release-notes.sh" 1.1.0-rc.1)"

if "$here/release-notes.sh" 1.1.0 > /dev/null 2>&1; then
  echo "FAIL a stable version without a section must fail"; failures=$((failures + 1))
else
  echo "ok   a stable version without a section fails"
fi

[ "$failures" -eq 0 ]
