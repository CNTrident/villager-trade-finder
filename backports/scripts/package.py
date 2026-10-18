"""Freeze each built source tree, create source JARs and a verified download bundle."""
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
matrix=json.loads((ROOT/'matrix.json').read_text(encoding='utf8'))
report=json.loads((ROOT/'build-report.json').read_text(encoding='utf8'))
dist=ROOT/'dist'
assert len(matrix)==19
license=(ROOT.parent/'LICENSE').read_bytes()
summary=[]
for entry in matrix:
    mc=entry['minecraft']
    result=report[mc]
    assert result['status']=='verified',f'Not verified: {mc}'
    jar=dist/result['jar']
    assert hashlib.sha256(jar.read_bytes()).hexdigest()==result['sha256']
    source=ROOT/'generated'/mc
    source_hash=hashlib.sha256()
    for path in sorted(source.rglob('*')):
        if path.is_file():source_hash.update(path.relative_to(source).as_posix().encode()+b'\0'+path.read_bytes())
    assert result.get('source_sha256')==source_hash.hexdigest(),f'Source changed after build: {mc}'
    frozen=ROOT/'versions'/mc
    for folder in ('java','resources','tests'):
        shutil.copytree(source/folder,frozen/folder,dirs_exist_ok=True)
    source_jar=dist/f'villager_trade_finder-fabric-{mc}-1.8.0-sources.jar'
    with zipfile.ZipFile(source_jar,'w',zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((source/'java').rglob('*.java')):
            archive.writestr(path.relative_to(source/'java').as_posix(),path.read_bytes())
        archive.writestr('LICENSE',license)
        archive.writestr('SOURCE-NOT-INSTALLABLE.txt','Source code only; install the matching JAR without the -sources suffix.\n')
    def dependency(value):
        if value is None:return None
        return {'version':value['version_number'],'game_versions':value['game_versions'],
                'page':'https://modrinth.com/mod/'+('trade-cycling' if value['project_id']=='qpPoAL6m' else 'visible-traders')+'/version/'+value['id'],
                'download':next(f['url'] for f in value['files'] if f['primary'])}
    row={'minecraft':mc,'mod_version':'1.8.0','java':entry['java'],'fabric_api':entry['fabric_api'],
         'jar':jar.name,'sha256':result['sha256'],'bytes':result['bytes'],
         'sources':source_jar.name,'source_directory':f'backports/versions/{mc}',
         'trade_cycling':dependency(entry['trade_cycling']),
         'visible_traders':dependency(entry['visible_traders']),
         'checks':'compile, remap, headless matching/configuration regression checks; no in-game testing'}
    (frozen/'version.json').write_text(json.dumps(row,ensure_ascii=False,indent=2),encoding='utf8')
    summary.append(row)
legacy_summary=list(summary)
# The current-version binaries were already built and checked with this same
# unchanged 1.8.0 main source. Keep them available in the consolidated release.
known={
    '26.1.2':'c88e438279d86634c468fd17ec230bf48108691523c3f767a41cbaec5dc6a8c7',
    '26.2':'ac01c9e8c3c8a661028e806daa010d20074a632160c2a5a66a37d39398b8d480',
}
trade_versions=json.loads((ROOT/'metadata/trade-cycling.json').read_text(encoding='utf8'))
visible_versions=json.loads((ROOT/'metadata/visible-traders.json').read_text(encoding='utf8'))
for mc,expected in known.items():
    jar=ROOT.parent/'build/libs'/f'villager_trade_finder-fabric-{mc}-1.8.0.jar'
    assert hashlib.sha256(jar.read_bytes()).hexdigest()==expected,('Changed baseline binary',mc)
    with zipfile.ZipFile(jar) as archive:
        metadata=json.loads(archive.read('fabric.mod.json'))
        assert metadata['depends']['minecraft']==mc and metadata['version']=='1.8.0'
    shutil.copy2(jar,dist/jar.name)
    source_jar=dist/f'villager_trade_finder-fabric-{mc}-1.8.0-sources.jar'
    with zipfile.ZipFile(source_jar,'w',zipfile.ZIP_DEFLATED) as archive:
        roots=[ROOT.parent/'src/main/java',ROOT.parent/('src/compat/26_1_2/java' if mc=='26.1.2' else 'src/compat/26_2/java')]
        for source in roots:
            for path in sorted(source.rglob('*.java')):archive.writestr(path.relative_to(source).as_posix(),path.read_bytes())
        archive.writestr('LICENSE',license)
        archive.writestr('SOURCE-NOT-INSTALLABLE.txt','Source code only; not an installable mod.\n')
    trade=next((v for v in trade_versions if 'fabric' in v['loaders'] and mc in v['game_versions'] and v['version_number'].endswith('-1.0.21')),None)
    visible=next(v for v in visible_versions if 'fabric' in v['loaders'] and mc in v['game_versions'])
    summary.append({'minecraft':mc,'mod_version':'1.8.0','java':25,'fabric_api':'0.146.0+26.1.2' if mc=='26.1.2' else '0.158.0+26.2',
                    'jar':jar.name,'sha256':expected,'bytes':jar.stat().st_size,'sources':source_jar.name,
                    'source_directory':'src/main/java + src/compat/'+('26_1_2' if mc=='26.1.2' else '26_2'),
                    'trade_cycling':dependency(trade),'visible_traders':dependency(visible),
                    'checks':'previously completed 1.8.0 build and potion regression checks; no in-game testing'})
(ROOT/'release-matrix.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8')
shutil.copy2(ROOT/'release-matrix.json',dist/'version-manifest.json')
with zipfile.ZipFile(dist/'villager_trade_finder-1.8.0-mc1.20-1.21.11-installers.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for row in legacy_summary:archive.write(dist/row['jar'],row['jar'])
    archive.write(dist/'version-manifest.json','version-manifest.json')
    archive.writestr('INSTALL.txt','Choose ONE jar matching your exact Minecraft version. Requires Fabric API and server/client Trade Cycling. Future trades need matching VisibleTraders. Sources jars are not installable.\n')
    archive.writestr('LICENSE',license)
checksums=[]
for path in sorted(dist.iterdir()):
    if path.is_file() and path.name!='SHA256SUMS.txt':
        checksums.append(hashlib.sha256(path.read_bytes()).hexdigest()+'  '+path.name)
(dist/'SHA256SUMS.txt').write_text('\n'.join(checksums)+'\n',encoding='utf8')
print('Packaged',len(summary),'verified versions and',len(checksums),'files')
