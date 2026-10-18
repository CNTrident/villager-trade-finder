"""Package the checked 26.3 port alongside the 23 unchanged published binaries."""
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
DIST = ROOT / 'build/release-mc26.3/dist'
DIST.mkdir(parents=True, exist_ok=True)
assert not any(DIST.iterdir()), 'Refusing to overwrite an existing distribution'
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
expected = dict((name, digest) for digest, name in
                (line.split('  ', 1) for line in (args.baseline_dist / 'SHA256SUMS.txt').read_text().splitlines()))
rows = json.loads((args.baseline_dist / 'version-manifest.json').read_text(encoding='utf8'))
assert len(rows) == 23
for row in rows:
    for name in (row['jar'], row['sources']):
        source = args.baseline_dist / name
        assert sha(source) == expected[name]
        if name == row['jar']: assert sha(source) == row['sha256']
        shutil.copy2(source, DIST / name)
jar = args.modern_libs / 'villager_trade_finder-fabric-26.3-1.8.0.jar'
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    metadata = json.loads(archive.read('fabric.mod.json'))
    assert metadata['version'] == '1.8.0' and metadata['depends']['minecraft'] == '26.3'
    assert metadata['depends']['fabricloader'] == '>=0.19.3'
    assert int.from_bytes(archive.read('com/uncraftbar/easyautocycler/AutomationManager.class')[6:8], 'big') == 69
    assert 'com/uncraftbar/easyautocycler/compat/MinecraftApiCompat.class' in archive.namelist()
    assert not any(n.endswith('/PotionArrowChecks.class') for n in archive.namelist())
shutil.copy2(jar, DIST / jar.name)
sources = DIST / 'villager_trade_finder-fabric-26.3-1.8.0-sources.jar'
with zipfile.ZipFile(sources, 'w', zipfile.ZIP_DEFLATED) as archive:
    for source in (ROOT / 'src/main/java', ROOT / 'src/compat/26_3/java'):
        for path in sorted(source.rglob('*.java')):
            archive.writestr(path.relative_to(source).as_posix(), path.read_bytes())
    archive.writestr('LICENSE', (ROOT / 'LICENSE').read_bytes())
    archive.writestr('SOURCE-NOT-INSTALLABLE.txt', 'Source code only; do not install this JAR as a mod.\n')
def dependency(version_id, slug):
    request = urllib.request.Request('https://api.modrinth.com/v2/version/' + version_id,
                                     headers={'User-Agent': 'VillagerTradeFinder-release'})
    with urllib.request.urlopen(request, timeout=60) as response: version = json.load(response)
    assert '26.3' in version['game_versions']
    return {'version': version['version_number'], 'game_versions': version['game_versions'],
            'page': 'https://modrinth.com/mod/' + slug + '/version/' + version_id,
            'download': next(f['url'] for f in version['files'] if f['primary'])}
rows.append({'minecraft': '26.3', 'mod_version': '1.8.0', 'java': 25, 'fabric_loader': '>=0.19.3',
             'fabric_api': '0.163.0+26.3', 'jar': jar.name, 'sha256': sha(jar), 'bytes': jar.stat().st_size,
             'sources': sources.name, 'source_directory': 'src/main/java + src/compat/26_3/java',
             'build_arguments': ['-Pminecraft_version=26.3', '-Pfabric_loader_version=0.19.3',
                                 '-Pfabric_api_version=0.163.0+26.3', '-Ptrade_cycling_file_id=9155650'],
             'trade_cycling': dependency('FKbMeHEw', 'trade-cycling'),
             'visible_traders': dependency('TojbcxPs', 'visible-traders'),
             'checks': 'compile; SDL key codes; trade-list API; Trade Cycling API; potion/bilingual/config regression; no in-game testing'})
assert len(rows) == len({row['minecraft'] for row in rows}) == 24
manifest = json.dumps(rows, ensure_ascii=False, indent=2).encode('utf8')
(DIST / 'version-manifest.json').write_bytes(manifest)
instructions = '''Villager Trade Finder 1.8.0：24 个游戏版本安装包合集

包含 1.20～1.21.11 的 19 个正式版本，以及 26.1、26.1.1、26.1.2、26.2、26.3。
只选择与游戏版本完全对应的一份 JAR 放入客户端 mods，不能全部一起安装。
本 ZIP 不含源码 JAR 或前置 Mod；不要把 ZIP 直接放进 mods。

需要：对应游戏版本的 Fabric Loader、Fabric API、Trade Cycling；多人服务器也需要 Trade Cycling。
26.3 要求 Java 25、Fabric Loader 0.19.3 或更新版本；本次使用 Fabric API 0.163.0+26.3、Trade Cycling 1.0.23+26.3。
后续/全部交易需要客户端与服务器另装 VisibleTraders；26.3 使用 2.5.1 或兼容新版。
26.1～26.1.2 对应 VisibleTraders 2.4.x，26.2 对应 2.5.x。
Java：1.20～1.20.4 为 17；1.20.5～1.21.11 为 21；26.x 为 25。
打开未锁定职业村民的交易界面，按 C 编辑目标，R 开始或停止。

前置下载与源码位置见 version-manifest.json。1.20、1.20.3、1.21.2 的 Trade Cycling 前置兼容性仍需核对。
本地构建与自动回归检查已通过，未完成逐版本游戏内实测。
SHA256SUMS.txt 可检查文件；MIT 许可与原作者、修改者版权声明见 LICENSE。
发布：https://github.com/CNTrident/villager-trade-finder/releases/tag/v1.8.0-mc26.3
'''.encode('utf-8-sig')
contents = {row['jar']: (DIST / row['jar']).read_bytes() for row in rows}
contents.update({'version-manifest.json': manifest, '安装说明.txt': instructions,
                 'LICENSE': (ROOT / 'LICENSE').read_bytes()})
contents['SHA256SUMS.txt'] = ''.join(hashlib.sha256(data).hexdigest() + '  ' + name + '\n'
                                  for name, data in sorted(contents.items())).encode('utf8')
with zipfile.ZipFile(DIST / 'villager_trade_finder-1.8.0-all-24-installers.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for name, data in sorted(contents.items()): archive.writestr(name, data)
(DIST / 'SHA256SUMS.txt').write_text(''.join(sha(path) + '  ' + path.name + '\n'
    for path in sorted(DIST.iterdir()) if path.name != 'SHA256SUMS.txt'), encoding='utf8')
assert len(list(DIST.iterdir())) == 51
print('PACKAGED 24 versions / 51 assets:', DIST, flush=True)
