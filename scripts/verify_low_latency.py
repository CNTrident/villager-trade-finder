"""Run the packaged 26.2 mod through real Fabric/Mixin, without a world or game window.

First: gradlew.bat writeSmokeClasspath -PlowLatencySmoke
Then: python scripts/verify_low_latency.py --java PATH_TO_JAVA --visible-traders PATH_TO_JAR
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--java', default='java')
parser.add_argument('--visible-traders', type=Path)
args = parser.parse_args()
cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches'

def dependency(group, artifact, version):
    jars = list((cache / 'modules-2/files-2.1' / group / artifact / version).glob('*/*.jar'))
    return next(p for p in jars if not p.name.endswith(('-sources.jar', '-javadoc.jar')))

version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))

classpath = [Path(p) for p in json.loads((ROOT / 'build/smoke-classpath.json').read_text())]
classpath = [p for p in classpath if p.is_file()
             and '/remapped_mods/' not in p.as_posix()
             and '/minecraftMaven/' not in p.as_posix()
             and p.name != 'mappings.jar']
classpath.append(cache / 'fabric-loom/26.2/minecraft-merged.jar')
mods = [ROOT / f'build/libs/villager_trade_finder-fabric-26.2-{version}.jar',
        ROOT / f'build/smoke-artifacts/villager_trade_finder-fabric-26.2-{version}-smoke.jar',
        dependency('net.fabricmc.fabric-api', 'fabric-api', '0.158.0+26.2'),
        dependency('curse.maven', 'trader-cycling-570431', '8271477')]
run_id = uuid.uuid4().hex[:8]
for visible in ([False, True] if args.visible_traders else [False]):
    for side in ['server', 'client']:
        work = ROOT / 'build' / ('packaged-smoke-' + side + ('-visible' if visible else '-plain') + '-' + run_id)
        work.mkdir(parents=True, exist_ok=False)
        (work / 'mods').mkdir()
        for source in mods + ([args.visible_traders.resolve()] if visible else []):
            shutil.copy2(source, work / 'mods' / source.name)
        main = 'net.fabricmc.loader.impl.launch.knot.Knot' + side.capitalize()
        argfile = work / 'java.args'
        argfile.write_text('-Dfabric.development=false\n-classpath\n"'
                           + os.pathsep.join(p.as_posix() for p in classpath) + '"\n' + main + '\n')
        with (work / 'console.log').open('w', encoding='utf-8') as output:
            completed = subprocess.run([args.java, '@' + str(argfile)], cwd=work,
                                       stdout=output, stderr=subprocess.STDOUT, timeout=90)
        if completed.returncode != 0:
            print((work / 'console.log').read_text(encoding='utf-8')[-16000:])
            raise SystemExit(completed.returncode)
        print((work / 'smoke-result.txt').read_text().strip(), flush=True)
