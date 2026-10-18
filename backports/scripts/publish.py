"""Publish verified assets using the user's in-memory Git credential. No token files."""
import concurrent.futures
import hashlib
import json
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REPO='CNTrident/villager-trade-finder'
BASE='https://api.github.com/repos/'+REPO
credential=subprocess.run(['git','credential','fill'],input='protocol=https\nhost=github.com\n\n',
                          text=True,capture_output=True,check=True,cwd=ROOT)
fields=dict(line.split('=',1) for line in credential.stdout.splitlines() if '=' in line)
token=fields.get('password')
if not token:raise RuntimeError('No existing GitHub credential; nothing was published')
headers={'Authorization':'Bearer '+token,'Accept':'application/vnd.github+json',
         'X-GitHub-Api-Version':'2022-11-28','User-Agent':'CNTrident/VillagerTradeFinder-release'}

def api(url,method='GET',body=None,raw=False):
    if body is None:payload=None
    elif isinstance(body,bytes):payload=body
    else:payload=json.dumps(body,ensure_ascii=False).encode('utf8')
    request_headers=dict(headers)
    request_headers['Content-Type']='application/octet-stream' if isinstance(body,bytes) else 'application/json'
    request=urllib.request.Request(url,data=payload,headers=request_headers,method=method)
    with urllib.request.urlopen(request,timeout=120) as response:
        data=response.read()
        return data if raw else json.loads(data) if data else None

user=api('https://api.github.com/user')
assert user['login']=='CNTrident' and user['id']==95963147,'Unexpected authenticated account'
sha=subprocess.check_output(['git','rev-parse','HEAD'],text=True,cwd=ROOT).strip()
remote=api(BASE+'/commits/main')
assert remote['sha']==sha,'Local commit is not on remote main'
body=(ROOT/'release-notes.md').read_text(encoding='utf8')
release_fields={'tag_name':'v1.8.0','target_commitish':sha,
                'name':'Villager Trade Finder 1.8.0 - Minecraft 1.20-1.21.11 + 26.x',
                'body':body}
try:release=api(BASE+'/releases/tags/v1.8.0')
except urllib.error.HTTPError as error:
    if error.code!=404:raise
    release=api(BASE+'/releases','POST',{**release_fields,'draft':True,'prerelease':False})
assert release['author']['login']=='CNTrident'
if not release['draft']:raise RuntimeError('v1.8.0 is already published; refusing to replace a published release')
release=api(BASE+'/releases/'+str(release['id']),'PATCH',release_fields)
existing={a['name']:a for a in release['assets']}
assets=[p for p in sorted((ROOT/'dist').iterdir()) if p.is_file()]
assert len([p for p in assets if p.name.endswith('.jar') and not p.name.endswith('-sources.jar')])>=19
upload=release['upload_url'].split('{',1)[0]

def upload_asset(path):
    payload=path.read_bytes();digest='sha256:'+hashlib.sha256(payload).hexdigest()
    if path.name in existing:
        asset=existing[path.name]
    else:
        for attempt in range(3):
            try:
                asset=api(upload+'?name='+urllib.parse.quote(path.name),'POST',payload)
                break
            except urllib.error.HTTPError as error:
                if error.code not in (502,503,504) or attempt==2:raise
                time.sleep(2)
    assert asset['size']==len(payload),(path.name,'size mismatch')
    assert asset.get('digest')==digest,(path.name,'digest mismatch')
    print('ASSET VERIFIED',path.name,flush=True)
    return asset

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    uploaded=list(pool.map(upload_asset,assets))
release=api(BASE+'/releases/'+str(release['id']),'PATCH',{**release_fields,'draft':False,'prerelease':False,'make_latest':'true'})
published=api(BASE+'/releases/tags/v1.8.0')
assert not published['draft'] and published['author']['login']=='CNTrident'
assert len(published['assets'])==len(assets)
print('PUBLISHED',published['html_url'],'assets',len(assets),'commit',sha,flush=True)
