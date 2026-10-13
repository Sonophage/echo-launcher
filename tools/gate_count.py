"""Counts the gate's results from the test XML (tools/gate.sh) and checks every module with tests wrote some.

Run from the repo root: python3 -I tools/gate_count.py <commit>. Prints the gate: line, writes it to
build/gate.txt with the commit, and exits 1 when a module that has src/test wrote no results.
"""
import glob
import sys
import xml.etree.ElementTree as E


def main(commit):
    files = [f for f in glob.glob('**/build/test-results/**/TEST-*.xml', recursive=True) if not f.startswith('.claude')]
    t = fl = er = sk = 0
    ran = set()
    for f in files:
        r = E.parse(f).getroot()
        t += int(r.get('tests', 0)); fl += int(r.get('failures', 0))
        er += int(r.get('errors', 0)); sk += int(r.get('skipped', 0))
        ran.add(f.split('/build/')[0])
    expected = {d[:-len('/src/test')] for d in glob.glob('**/src/test', recursive=True) if not d.startswith('.claude')}
    missing = sorted(expected - ran)
    line = f"gate: {t} tests, {fl} failures, {er} errors, {sk} skipped, from {len(files)} files in {len(ran)} modules"
    print(line)
    with open('build/gate.txt', 'w') as out:
        out.write(f"{line} at {commit}\n" + (f"missing: {', '.join(missing)}\n" if missing else ''))
    if missing:
        print(f"gate: no test results from {', '.join(missing)}", file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else '?'))
