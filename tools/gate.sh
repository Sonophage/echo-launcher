#!/usr/bin/env bash
# The gate (CLAUDE.md): runs the full check and counts the results from the test XML, never from
# Gradle's summary. Old results are deleted first, so a module that did not run counts as missing,
# not as passed. Extra arguments go to Gradle. Exits non-zero when Gradle or any test fails, when a
# module with tests wrote no results, or when another gate is already running in this checkout.
# The result line is also written to build/gate.txt with the commit it ran on.
set -u
cd "$(dirname "$0")/.."

# Two gates in one checkout delete each other's results, so the second one stops here.
mkdir -p build
exec 9>build/.gate.lock
flock -n 9 || { echo "gate: another gate is running in this checkout; wait for it" >&2; exit 1; }

# A worktree has no copy of the Discord SDK aar (it is not committed); take the main checkout's.
aar=discord/discord-native/libs/discord_partner_sdk.aar
main=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
if [ ! -f "$aar" ] && [ -f "$main/$aar" ]; then mkdir -p "$(dirname "$aar")"; cp "$main/$aar" "$aar"; fi

find . -path ./.claude -prune -o -path '*/build/test-results/*' -name 'TEST-*.xml' -print0 | xargs -0 rm -f

./gradlew checkStructure testDebugUnitTest :core:core-archive:test :core:core-navigation:test \
  :core:theme-kit:test :studio:test lintDebug --continue "$@"
gradle=$?

python3 -I tools/gate_count.py "$(git rev-parse --short HEAD)"
count=$?

echo "gradle exit: $gradle"
[ $gradle -ne 0 ] && exit $gradle
exit $count
