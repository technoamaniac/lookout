#!/bin/sh
# Prints the CHANGELOG.md section for a version, for use as the body of a GitHub release.
# Usage: scripts/release-notes.sh 1.2.3   (a leading "v" is accepted)
# A pre-release (1.2.3-rc.1) falls back to [Unreleased] when it has no section of its own; a stable
# version must have one, so a release cannot go out without its changelog entry.
set -eu

CHANGELOG="${CHANGELOG:-CHANGELOG.md}"
version="${1:?usage: release-notes.sh <version>}"
version="${version#v}"

# Prints the body of the "## [heading]" section, without the heading itself
section() {
  awk -v heading="## [$1]" '
    index($0, heading) == 1 { found = 1; next }
    found && /^## / { exit }
    found { print }
  ' "$CHANGELOG"
}

# Drops blank lines at both ends, so the notes start and end on text
trim() {
  awk 'NF { for (i = 0; i < blank; i++) print ""; blank = 0; print; started = 1; next }
       started { blank++ }'
}

notes=$(section "$version" | trim)
case "$version" in
  *-*) [ -n "$notes" ] || notes=$(section "Unreleased" | trim) ;;
esac

if [ -z "$notes" ]; then
  echo "No entry for $version in $CHANGELOG (add a '## [$version]' section)" >&2
  exit 1
fi
printf '%s\n' "$notes"
