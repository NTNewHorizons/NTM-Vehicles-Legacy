#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
dependency="$root/.dependencies/IVL"
source=${IVL_SOURCE:-https://github.com/THOMASS47/IVL.git}
revision=d32bf237d2eb741ce11f86285a7bbe6157722be1
core_revision=cd9cfb8fe74dbcc426eb830f14822adf7402261f
patch="$root/patches/ivl-custom-hit-once.patch"
stamp="$dependency/.bridge-patch-sha256"
if command -v sha256sum >/dev/null 2>&1; then
    checksum=$(sha256sum "$patch" | cut -d ' ' -f 1)
elif command -v shasum >/dev/null 2>&1; then
    checksum=$(shasum -a 256 "$patch" | cut -d ' ' -f 1)
else
    echo "Dependency preparation requires sha256sum or shasum." >&2
    exit 1
fi
jar="$dependency/build/libs/immersivevehicleslegacy-0.1.0-ntmv2-dev.jar"

export GIT_TERMINAL_PROMPT=0
if [[ -n ${IVL_READ_TOKEN:-} ]]; then
    # Keep credentials out of URLs, Git configuration, and command-line arguments.
    export GIT_ASKPASS="$root/scripts/git-askpass.sh"
fi
if [[ ! -e "$dependency/.git" ]]; then
    # Gradle creates output directories before Exec starts; initialize in place.
    git init "$dependency"
    git -C "$dependency" remote add origin "$source"
fi
if ! git -C "$dependency" rev-parse --verify HEAD >/dev/null 2>&1; then
    if ! git -C "$dependency" fetch --depth 1 "$source" "$revision"; then
        echo "Cannot fetch pinned IVL source. Supply IVL_SOURCE with an authorized mirror/local checkout," >&2
        echo "authenticate Git for private IVL access (IVL_READ_TOKEN for HTTPS), or use -PivlJar=/path/to/patched-dev.jar." >&2
        exit 1
    fi
    git -C "$dependency" checkout --detach "$revision"
fi
[[ $(git -C "$dependency" rev-parse HEAD) == "$revision" ]] || {
    echo "Unexpected IVL revision in $dependency; use a fresh checkout." >&2
    exit 1
}
if [[ ! -f "$dependency/MinecraftTransportSimulator/mccore/src/main/java/minecrafttransportsimulator/entities/instances/EntityBullet.java" ]]; then
    git clone --no-checkout https://github.com/DonBruce64/MinecraftTransportSimulator.git \
        "$dependency/MinecraftTransportSimulator"
    git -C "$dependency/MinecraftTransportSimulator" checkout --detach "$core_revision"
fi
[[ $(git -C "$dependency/MinecraftTransportSimulator" rev-parse HEAD) == "$core_revision" ]]
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
