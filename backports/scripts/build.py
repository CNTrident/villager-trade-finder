"""Build/retest each requested version; retain logs and verified release outputs."""
import argparse
import concurrent.futures
import hashlib
import json
import os
import subprocess
import sys
import threading
import zipfile
from pathlib import Path
from generate import generate, ROOT

parser=argparse.ArgumentParser()
parser.add_argument('versions',nargs='*')
parser.add_argument('--jobs',type=int,default=2)
args=parser.parse_args()
matrix=json.loads((ROOT/'matrix.json').read_text(encoding='utf8'))
versions=args.versions or [x['minecraft'] for x in matrix]
logs=ROOT/'logs';logs.mkdir(exist_ok=True)
dist=ROOT/'dist';dist.mkdir(exist_ok=True)
env=os.environ.copy()
bundled_java=ROOT.parent/'.tools/jdk25/jdk-25.0.3+9'
if bundled_java.exists():env['JAVA_HOME']=str(bundled_java)
gradle=ROOT.parent/'.tools/gradle-9.4.0/bin/gradle.bat'
if not gradle.exists():gradle=ROOT.parent/('gradlew.bat' if os.name=='nt' else 'gradlew')
report_path=ROOT/'build-report.json'
report=json.loads(report_path.read_text(encoding='utf8')) if report_path.exists() else {}
report_lock=threading.Lock()

def build_one(mc):
    generate(mc)
    # A distinct Gradle project/cache per game version makes parallel Loom remaps
    # independent. Only immutable dependency downloads are shared by Gradle.
    workspace=ROOT/'.workspaces'/mc
    workspace.mkdir(parents=True,exist_ok=True)
    build=(ROOT/'build.gradle').read_text(encoding='utf8')
    build=build.replace("file('matrix.json')", "file('../../matrix.json')")
    build=build.replace('file("build/${mc}")','file("../../build/${mc}")')
    build=build.replace('file("versions/${mc}")','file("../../versions/${mc}")')
    build=build.replace('? "versions/${mc}" : "generated/${mc}"','? "../../versions/${mc}" : "../../generated/${mc}"')
    build=build.replace("from('../LICENSE')","from('../../../LICENSE')")
    (workspace/'build.gradle').write_text(build,encoding='utf8')
    (workspace/'settings.gradle').write_text((ROOT/'settings.gradle').read_text(encoding='utf8').replace(
        "rootProject.name = 'villager-trade-finder-backports'", "rootProject.name = 'villager-trade-finder-"+mc+"'"),encoding='utf8')
    (workspace/'gradle.properties').write_text((ROOT/'gradle.properties').read_text(encoding='utf8'),encoding='utf8')
    log=logs/f'{mc}.log'
    print('BUILD START',mc,flush=True)
    with log.open('w',encoding='utf8') as output:
        result=subprocess.run([str(gradle),'build',f'-Pmc={mc}','-Pgenerated=true','--console=plain','--daemon'],cwd=workspace,env=env,
                              stdout=output,stderr=subprocess.STDOUT)
    status={'exit_code':result.returncode,'log':str(log.relative_to(ROOT))}
    if result.returncode==0:
        jar=ROOT/'build'/mc/'libs'/f'villager_trade_finder-fabric-{mc}-1.8.0.jar'
        payload=jar.read_bytes()
        with zipfile.ZipFile(jar) as package:
            metadata=json.loads(package.read('fabric.mod.json'))
            assert metadata['depends']['minecraft']==mc
            assert metadata['version']=='1.8.0'
            assert 'easyautocycler.refmap.json' in package.namelist(),'Missing obfuscated mixin refmap'
            assert any(p.endswith('/PotionNameResolver.class') for p in package.namelist())
            assert not any(p.endswith('/BackportChecks.class') for p in package.namelist())
            java_major=int.from_bytes(package.read('com/uncraftbar/easyautocycler/AutomationManager.class')[6:8],'big')
            expected=next(x['java'] for x in matrix if x['minecraft']==mc)+44
            assert java_major==expected,(mc,java_major,expected)
        (dist/jar.name).write_bytes(payload)
        status.update(status='verified',jar=jar.name,sha256=hashlib.sha256(payload).hexdigest(),bytes=len(payload))
        print('BUILD VERIFIED',mc,status['sha256'],flush=True)
    else:
        status['status']='failed'
        print('BUILD FAILED',mc,'see',log,flush=True)
    source_hash=hashlib.sha256()
    for path in sorted((ROOT/'generated'/mc).rglob('*')):
        if path.is_file():
            source_hash.update(path.relative_to(ROOT/'generated'/mc).as_posix().encode()+b'\0'+path.read_bytes())
    status['source_sha256']=source_hash.hexdigest()
    with report_lock:
        report[mc]=status
        report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    return status

with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as pool:
    list(pool.map(build_one,versions))
print('COMPLETE',sum(x['status']=='verified' for x in report.values()),'verified of',len(report),flush=True)
sys.exit(0 if all(report[x]['status']=='verified' for x in versions) else 1)
