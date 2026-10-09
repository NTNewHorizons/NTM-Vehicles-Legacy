#!/usr/bin/env bash
set -euo pipefail

tag=${VERSION:?VERSION must contain the release tag}
[[ "$tag" =~ ^v?[0-9]+\.[0-9]+\.[0-9]+-1\.7\.10$ ]]
jar="build/libs/ntm_vehicles-$tag.jar"
test -f "$jar"

# Do not delete an existing release. Failed uploads leave a new release in draft.
if ! gh release view "$tag" >/dev/null 2>&1; then
    gh release create "$tag" --verify-tag --draft --generate-notes
fi
gh release upload "$tag" "$jar" --clobber
download=$(mktemp -d)
trap 'rm -f "$download/$(basename "$jar")"; rmdir "$download"' EXIT
gh release download "$tag" --pattern "$(basename "$jar")" --dir "$download"
cmp "$jar" "$download/$(basename "$jar")"
gh release edit "$tag" --draft=false
gh release view "$tag" --json isDraft --jq '.isDraft' | grep -qx false
