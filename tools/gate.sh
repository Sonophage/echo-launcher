#!/usr/bin/env bash
# The gate (CLAUDE.md): runs the full check and counts the results from the test XML, never from
# Gradle's summary. Old results are deleted first, so a module that did not run counts as missing,
# not as passed. Extra arguments go to Gradle. Exits non-zero when Gradle or any test fails.
set -u
cd "$(dirname "$0")/.."

find . -path ./.claude -prune -o -path '*/build/test-results/*' -name 'TEST-*.xml' -print0 | xargs -0 rm -f

./gradlew checkStructure testDebugUnitTest :core:core-archive:test :core:core-navigation:test \
  :core:theme-kit:test :studio:test lintDebug --continue "$@"
gradle=$?

python3 -I - <<'EOF'
import glob, xml.etree.ElementTree as E
files = [f for f in glob.glob('**/build/test-results/**/TEST-*.xml', recursive=True) if not f.startswith('.claude')]
t = fl = er = sk = 0
modules = set()
for f in files:
    r = E.parse(f).getroot()
    t += int(r.get('tests', 0)); fl += int(r.get('failures', 0)); er += int(r.get('errors', 0)); sk += int(r.get('skipped', 0))
    modules.add(f.split('/build/')[0])
print(f"gate: {t} tests, {fl} failures, {er} errors, {sk} skipped, from {len(files)} files in {len(modules)} modules")
EOF

echo "gradle exit: $gradle"
exit $gradle
