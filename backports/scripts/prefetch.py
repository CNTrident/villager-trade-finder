"""Warm only missing future-version Mojang downloads; verify official SHA-1 first."""
import concurrent.futures
import hashlib
import json
import os
import tempfile
import urllib.request
import time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CACHE=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/fabric-loom'
versions=['1.20.5','1.20.6','1.21','1.21.1','1.21.2','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7','1.21.8','1.21.9','1.21.10']

def download(task):
    mc,side=task
    info=json.loads((ROOT/'metadata'/f'minecraft-{mc}.json').read_text(encoding='utf8'))['downloads'][side]
    dest=CACHE/mc/f'minecraft-{side}.jar'
    if dest.exists():return
    dest.parent.mkdir(parents=True,exist_ok=True)
    for attempt in range(3):
        try:
            with urllib.request.urlopen(info['url'],timeout=120) as response:body=response.read()
            break
        except Exception:
            if attempt==2:raise
            time.sleep(2)
    assert len(body)==info['size'] and hashlib.sha1(body).hexdigest()==info['sha1'],(mc,side,'invalid Mojang download')
    with tempfile.NamedTemporaryFile(dir=dest.parent,prefix='verified-prefetch-',suffix='.tmp',delete=False) as temporary:
        temporary.write(body)
        name=Path(temporary.name)
    try:
        # Creating a hard link is atomic and never overwrites a Gradle-owned file.
        try:os.link(name,dest)
        except FileExistsError:pass
    finally:name.unlink()
    print('PREFETCH VERIFIED',mc,side,flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    futures={pool.submit(download,task):task for task in [(v,s) for v in versions for s in ('client','server')]}
    for future in concurrent.futures.as_completed(futures):
        try:future.result()
        except Exception as error:print('PREFETCH DEFERRED TO GRADLE',futures[future],type(error).__name__,flush=True)
