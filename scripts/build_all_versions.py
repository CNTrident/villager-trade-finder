"""Build the maintained 24-version matrix, keeping each version's validation log."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--only', help='Comma-separated Minecraft versions')
parser.add_argument('--online', action='store_true')
args = parser.parse_args()
selected = set(args.only.split(',')) if args.only else None
version = next(x.split('=', 1)[1] for x in (ROOT / 'gradle.properties').read_text().splitlines()
               if x.startswith('mod_version='))
output = ROOT / 'build' / ('matrix-' + version)
output.mkdir(parents=True, exist_ok=True)
lock = threading.Lock()
report = []

def build_group(rows, backport):
    for entry in rows:
        mc = entry['minecraft']
        if selected and mc not in selected:
            continue
        command = ['cmd', '/c', 'gradlew.bat'] if os.name == 'nt' else ['./gradlew']
        if backport:
            command += ['-p', 'backports', 'build', '-Pmc=' + mc]
        else:
            command += ['build', '-Pminecraft_version=' + mc,
                        '-Pfabric_api_version=' + entry['fabric_api'],
                        '-Ptrade_cycling_file_id=' + entry['trade_cycling_file_id']]
        command += ['--console=plain', '--max-workers=2']
        if not args.online:
            command.append('--offline')
        started = time.monotonic()
        log = output / (mc + '.log')
        with log.open('w', encoding='utf-8') as stream:
            result = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
        jar = (ROOT / 'backports/build' / mc / 'libs' if backport else ROOT / 'build/libs') \
              / f'villager_trade_finder-fabric-{mc}-{version}.jar'
        row = {'minecraft': mc, 'version': version, 'passed': result.returncode == 0 and jar.exists(),
               'exit_code': result.returncode, 'seconds': round(time.monotonic() - started, 1),
               'jar': str(jar.relative_to(ROOT)), 'log': str(log.relative_to(ROOT))}
        with lock:
            report.append(row)
            (output / 'build-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
            print(('PASS' if row['passed'] else 'FAIL'), mc, row['seconds'], 'seconds', flush=True)
        if not row['passed']:
            print(log.read_text(encoding='utf-8', errors='replace')[-3500:], flush=True)

with ThreadPoolExecutor(max_workers=2) as pool:
    futures = [pool.submit(build_group, json.loads((ROOT / 'backports/matrix.json').read_text()), True),
               pool.submit(build_group, json.loads((ROOT / 'mc26-matrix.json').read_text()), False)]
    for future in futures:
        future.result()
raise SystemExit(0 if all(row['passed'] for row in report) else 1)
