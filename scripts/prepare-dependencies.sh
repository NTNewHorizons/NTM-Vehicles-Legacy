#!/usr/bin/env bash
set -euo pipefail

# Clones the pinned IVL source, applies the CUSTOM hit patch, and builds the dev jar.
# IVL is private: configure normal Git authentication (SSH or credential helper).
# IVL_SOURCE may point at an authorized mirror; the pinned commit still applies.
root=$(cd "$(dirname "$0")/.." && pwd)
dependency="$root/.dependencies/IVL"
source=${IVL_SOURCE:-https://github.com/THOMASS47/IVL.git}
revision=d32bf237d2eb741ce11f86285a7bbe6157722be1
patch="$root/patches/ivl-custom-hit-once.patch"
stamp="$dependency/.bridge-patch-sha256"
checksum=$(sha256sum "$patch" | cut -d ' ' -f 1)
jar="$dependency/build/libs/immersivevehicleslegacy-0.1.0-ntmv2-dev.jar"

export GIT_TERMINAL_PROMPT=0
if [[ ! -e "$dependency/.git" ]]; then
    git init "$dependency"
    git -C "$dependency" remote add origin "$source"
fi
if ! git -C "$dependency" rev-parse --verify HEAD >/dev/null 2>&1; then
    git -C "$dependency" fetch --depth 1 "$source" "$revision"
    git -C "$dependency" checkout --detach "$revision"
fi
[[ $(git -C "$dependency" rev-parse HEAD) == "$revision" ]] || {
    echo "Unexpected IVL revision in $dependency; use a fresh checkout." >&2
    exit 1
}
if git -C "$dependency" apply --reverse --check "$patch" 2>/dev/null; then
    if [[ -f "$jar" && -f "$stamp" && $(cat "$stamp") == "$checksum" ]]; then
        exit 0
    fi
else
    git -C "$dependency" apply --check "$patch"
    git -C "$dependency" apply "$patch"
fi
VERSION=0.1.0-ntmv2 bash "$dependency/gradlew" -p "$dependency" --no-daemon assemble
test -f "$jar"
printf '%s\n' "$checksum" > "$stamp"
