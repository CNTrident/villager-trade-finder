"""Consolidate checked 1.8.0 binaries and newly built 26.1/26.1.1 binaries."""
import argparse
import hashlib
import json
import shutil
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--baseline-dist', type=Path, required=True)
parser.add_argument('--modern-libs', type=Path, required=True)
args = parser.parse_args()
DIST = ROOT / 'build/release-complete/dist'
DIST.mkdir(parents=True, exist_ok=True)
if any(DIST.iterdir()):
    raise RuntimeError('Output is not empty; refusing to overwrite packaged assets')

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

rows = json.loads((args.baseline_dist / 'version-manifest.json').read_text(encoding='utf8'))
assert len(rows) == 21
for row in rows:
    jar = args.baseline_dist / row['jar']
    assert digest(jar) == row['sha256']
    for name in (row['jar'], row['sources']):
        shutil.copy2(args.baseline_dist / name, DIST / name)

def metadata(project):
    request = urllib.request.Request('https://api.modrinth.com/v2/project/' + project + '/version',
                                     headers={'User-Agent': 'VillagerTradeFinder-release'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)

trade = next(v for v in metadata('trade-cycling') if v['version_number'] == 'fabric-1.0.21+26.1.2')
visible = next(v for v in metadata('visible-traders') if v['version_number'] == '2.4.0')
def dependency(version, slug):
    return {'version': version['version_number'], 'game_versions': version['game_versions'],
            'page': 'https://modrinth.com/mod/' + slug + '/version/' + version['id'],
            'download': next(f['url'] for f in version['files'] if f['primary'])}

for entry in json.loads((ROOT / 'mc26-matrix.json').read_text(encoding='utf8')):
    mc = entry['minecraft']
    if mc not in ('26.1', '26.1.1'): continue
    jar = args.modern_libs / f'villager_trade_finder-fabric-{mc}-1.8.0.jar'
    with zipfile.ZipFile(jar) as archive:
        assert archive.testzip() is None
        info = json.loads(archive.read('fabric.mod.json'))
        assert info['version'] == '1.8.0' and info['depends']['minecraft'] == mc
        bytecode = archive.read('com/uncraftbar/easyautocycler/AutomationManager.class')
        assert int.from_bytes(bytecode[6:8], 'big') == 69
        assert not any(n.endswith('/PotionArrowChecks.class') for n in archive.namelist())
    shutil.copy2(jar, DIST / jar.name)
    sources = DIST / f'villager_trade_finder-fabric-{mc}-1.8.0-sources.jar'
    with zipfile.ZipFile(sources, 'w', zipfile.ZIP_DEFLATED) as archive:
        for source in (ROOT / 'src/main/java', ROOT / entry['compatibility_source']):
            for path in sorted(source.rglob('*.java')):
                archive.writestr(path.relative_to(source).as_posix(), path.read_bytes())
        archive.writestr('LICENSE', (ROOT / 'LICENSE').read_bytes())
        archive.writestr('SOURCE-NOT-INSTALLABLE.txt', 'Source code only; do not install this JAR as a mod.\n')
    rows.append({'minecraft': mc, 'mod_version': '1.8.0', 'java': 25,
                 'fabric_api': entry['fabric_api'], 'jar': jar.name, 'sha256': digest(jar),
                 'bytes': jar.stat().st_size, 'sources': sources.name,
                 'source_directory': 'src/main/java + ' + entry['compatibility_source'],
                 'build_arguments': ['-Pminecraft_version=' + mc, '-Pfabric_api_version=' + entry['fabric_api'],
                                     '-Ptrade_cycling_file_id=' + entry['trade_cycling_file_id']],
                 'trade_cycling': dependency(trade, 'trade-cycling'),
                 'visible_traders': dependency(visible, 'visible-traders'),
                 'checks': 'compile, remap, potion-arrow/bilingual/configuration regression checks; no in-game testing'})
assert len(rows) == len({row['minecraft'] for row in rows}) == 23
rows.sort(key=lambda row: tuple(map(int, row['minecraft'].split('.'))))
manifest = json.dumps(rows, ensure_ascii=False, indent=2).encode('utf8')
(DIST / 'version-manifest.json').write_bytes(manifest)

instructions = '''Villager Trade Finder 1.8.0 安装包合集（23 个版本）

包含 1.20～1.21.11 的 19 个正式版本，以及 26.1、26.1.1、26.1.2、26.2。
只选与你游戏版本完全对应的一份 JAR 放入客户端 mods，不要全部一起安装。
本 ZIP 不含 sources 源码 JAR，不包含前置模组，ZIP 本身不要放入 mods。

另行安装对应游戏版本的 Fabric Loader、Fabric API 和 Trade Cycling。
多人服务器也需要 Trade Cycling；本模组本身只需安装在客户端。
后续/全部交易需要客户端、服务器安装对应版本的 VisibleTraders。
26.1～26.1.2 对应 VisibleTraders 2.4.x，26.2 对应 2.5.x。
Java：1.20～1.20.4 为 Java 17；1.20.5～1.21.11 为 Java 21；26.x 为 Java 25。
按 C 编辑职业目标，R 开始或停止。只对尚未交易锁定的村民刷新。

1.20、1.20.3、1.21.2 暂未在 Modrinth 找到单独标注的 Trade Cycling 成品，
请核对官方前置兼容性，不要混装其他版本依赖。各版本依赖地址见 version-manifest.json。
已通过本地构建和自动检查，未完成逐版本游戏内实测。
MIT 协议和原作者、修改者版权声明见 LICENSE；文件校验值见 SHA256SUMS.txt。
来源：https://github.com/CNTrident/villager-trade-finder/releases/tag/v1.8.0-complete
'''.encode('utf-8-sig')
contents = {row['jar']: (DIST / row['jar']).read_bytes() for row in rows}
contents.update({'version-manifest.json': manifest, '安装说明.txt': instructions,
                 'LICENSE': (ROOT / 'LICENSE').read_bytes()})
contents['SHA256SUMS.txt'] = ''.join(hashlib.sha256(data).hexdigest() + '  ' + name + '\n'
                                  for name, data in sorted(contents.items())).encode('utf8')
bundle = DIST / 'villager_trade_finder-1.8.0-all-installers.zip'
with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as archive:
    for name, data in sorted(contents.items()): archive.writestr(name, data)
with zipfile.ZipFile(bundle) as archive:
    assert archive.testzip() is None
    assert sum(n.endswith('.jar') for n in archive.namelist()) == 23
    assert not any('-sources' in n for n in archive.namelist())
(DIST / 'SHA256SUMS.txt').write_text(''.join(digest(path) + '  ' + path.name + '\n'
    for path in sorted(DIST.iterdir()) if path.name != 'SHA256SUMS.txt'), encoding='utf8')
assert len(list(DIST.iterdir())) == 49
print('PACKAGED 23 versions, 49 release assets:', DIST, flush=True)
