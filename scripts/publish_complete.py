"""Publish a new supplementary release; existing 1.8.0 remains unchanged."""
import concurrent.futures
import hashlib
import json
import subprocess
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = 'https://api.github.com/repos/CNTrident/villager-trade-finder'
TAG = 'v1.8.0-complete'
credential = subprocess.run(['git', 'credential', 'fill'], input='protocol=https\nhost=github.com\n\n',
                            text=True, capture_output=True, check=True, cwd=ROOT)
fields = dict(line.split('=', 1) for line in credential.stdout.splitlines() if '=' in line)
headers = {'Authorization': 'Bearer ' + fields['password'], 'Accept': 'application/vnd.github+json',
           'User-Agent': 'VillagerTradeFinder-release', 'X-GitHub-Api-Version': '2022-11-28'}
def api(url, method='GET', body=None):
    payload = body if isinstance(body, bytes) else None if body is None else json.dumps(body, ensure_ascii=False).encode('utf8')
    request_headers = {**headers, 'Content-Type': 'application/octet-stream' if isinstance(body, bytes) else 'application/json'}
    with urllib.request.urlopen(urllib.request.Request(url, data=payload, headers=request_headers, method=method), timeout=120) as response:
        data = response.read()
        return json.loads(data) if data else None

user = api('https://api.github.com/user')
assert user['login'] == 'CNTrident' and user['id'] == 95963147
sha = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
assert api(BASE + '/commits/main')['sha'] == sha
tag_sha = subprocess.check_output(['git', 'rev-parse', TAG + '^{}'], cwd=ROOT, text=True).strip()
assert tag_sha == sha
release_fields = {'tag_name': TAG, 'target_commitish': sha,
                  'name': 'Villager Trade Finder 1.8.0 Complete - 23 Minecraft versions',
                  'body': (ROOT / 'release-notes/1.8.0-complete.md').read_text(encoding='utf8')}
try:
    release = api(BASE + '/releases/tags/' + TAG)
except urllib.error.HTTPError as error:
    if error.code != 404: raise
    release = api(BASE + '/releases', 'POST', {**release_fields, 'draft': True, 'prerelease': False})
assert release['author']['login'] == 'CNTrident' and release['draft'], 'Do not modify an existing published release'
release = api(BASE + '/releases/' + str(release['id']), 'PATCH', release_fields)
existing = {asset['name']: asset for asset in release['assets']}
upload = release['upload_url'].split('{', 1)[0]
files = sorted((ROOT / 'build/release-complete/dist').iterdir())
assert len(files) == 49
def upload_one(path):
    data = path.read_bytes()
    asset = existing.get(path.name)
    if asset is None:
        asset = api(upload + '?name=' + urllib.parse.quote(path.name), 'POST', data)
    assert asset['size'] == len(data)
    assert asset['digest'] == 'sha256:' + hashlib.sha256(data).hexdigest()
    print('ASSET VERIFIED', path.name, flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    list(pool.map(upload_one, files))
release = api(BASE + '/releases/' + str(release['id']), 'PATCH',
              {**release_fields, 'draft': False, 'prerelease': False, 'make_latest': 'true'})
assert not release['draft'] and release['tag_name'] == TAG
assert len(release['assets']) == 49 and release['author']['login'] == 'CNTrident'
print('PUBLISHED', release['html_url'], 'commit', sha, flush=True)
