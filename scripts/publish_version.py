"""Publish a verified version without changing earlier releases or tags."""
import argparse
import concurrent.futures
import hashlib
import json
import subprocess
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--tag', required=True)
parser.add_argument('--notes', type=Path, required=True)
parser.add_argument('--dist', type=Path, required=True)
parser.add_argument('--name', required=True)
args = parser.parse_args()
BASE = 'https://api.github.com/repos/CNTrident/villager-trade-finder'
credentials = subprocess.run(['git', 'credential', 'fill'], input='protocol=https\nhost=github.com\n\n',
                             capture_output=True, text=True, check=True, cwd=ROOT)
fields = dict(line.split('=', 1) for line in credentials.stdout.splitlines() if '=' in line)
headers = {'Authorization': 'Bearer ' + fields['password'], 'User-Agent': 'VillagerTradeFinder-release',
           'Accept': 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28'}
def api(url, method='GET', body=None):
    data = body if isinstance(body, bytes) else None if body is None else json.dumps(body, ensure_ascii=False).encode('utf8')
    request_headers = {**headers, 'Content-Type': 'application/octet-stream' if isinstance(body, bytes) else 'application/json'}
    with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=request_headers, method=method), timeout=120) as response:
        payload = response.read()
        return json.loads(payload) if payload else None
user = api('https://api.github.com/user')
assert user['login'] == 'CNTrident' and user['id'] == 95963147
sha = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
assert api(BASE + '/commits/main')['sha'] == sha
assert subprocess.check_output(['git', 'rev-parse', args.tag + '^{}'], cwd=ROOT, text=True).strip() == sha
release_fields = {'tag_name': args.tag, 'target_commitish': sha, 'name': args.name,
                  'body': args.notes.read_text(encoding='utf8')}
try: release = api(BASE + '/releases/tags/' + urllib.parse.quote(args.tag, safe=''))
except urllib.error.HTTPError as error:
    if error.code != 404: raise
    release = api(BASE + '/releases', 'POST', {**release_fields, 'draft': True, 'prerelease': False})
assert release['draft'] and release['author']['login'] == 'CNTrident', 'Do not overwrite a published release'
release = api(BASE + '/releases/' + str(release['id']), 'PATCH', release_fields)
existing = {a['name']: a for a in release['assets']}
files = sorted(p for p in args.dist.iterdir() if p.is_file())
rows = json.loads((args.dist / 'version-manifest.json').read_text(encoding='utf8'))
assert len(files) == len(rows) * 2 + 3
for row in rows:
    assert hashlib.sha256((args.dist / row['jar']).read_bytes()).hexdigest() == row['sha256']
upload = release['upload_url'].split('{', 1)[0]
def upload_one(path):
    payload = path.read_bytes()
    asset = existing.get(path.name)
    if asset is None: asset = api(upload + '?name=' + urllib.parse.quote(path.name), 'POST', payload)
    assert asset['size'] == len(payload)
    assert asset['digest'] == 'sha256:' + hashlib.sha256(payload).hexdigest()
    print('VERIFIED', path.name, flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    list(pool.map(upload_one, files))
release = api(BASE + '/releases/' + str(release['id']), 'PATCH',
              {**release_fields, 'draft': False, 'prerelease': False, 'make_latest': 'true'})
assert release['tag_name'] == args.tag and not release['draft']
assert release['author']['login'] == 'CNTrident' and len(release['assets']) == len(files)
print('PUBLISHED', release['html_url'], 'commit', sha, flush=True)
