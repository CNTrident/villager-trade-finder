"""Resolve exact released game/dependency versions from official metadata."""
import json
import re
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "metadata"
CACHE.mkdir(parents=True, exist_ok=True)

def fetch(url, name):
    path = CACHE / name
    if not path.exists():
        request = urllib.request.Request(url, headers={"User-Agent": "CNTrident/VillagerTradeFinder backport build"})
        with urllib.request.urlopen(request, timeout=90) as response:
            path.write_bytes(response.read())
    return path

def data(url, name):
    return json.loads(fetch(url, name).read_text(encoding="utf-8"))

manifest = data("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", "minecraft.json")
trade = data("https://api.modrinth.com/v2/project/trade-cycling/version", "trade-cycling.json")
visible = data("https://api.modrinth.com/v2/project/visible-traders/version", "visible-traders.json")
api_xml = ET.parse(fetch("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml", "fabric-api.xml"))
apis = [node.text for node in api_xml.findall(".//version")]
versions = [v for v in manifest["versions"] if v["type"] == "release"
            and re.fullmatch(r"1\.(20(?:\.[1-6])?|21(?:\.(?:[1-9]|10|11))?)", v["id"])]
versions.sort(key=lambda v: tuple(map(int, v["id"].split("."))))
matrix = []
def slim(value):
    if value is None:return None
    return {"id":value["id"],"version_number":value["version_number"],"game_versions":value["game_versions"],
            "project_id":value["project_id"],"files":[f for f in value["files"] if f["primary"]]}
for release in versions:
    mc = release["id"]
    api = [v for v in apis if v.endswith("+" + mc)][-1]
    game = data(release["url"], f"minecraft-{mc}.json")
    dep = next((v for v in trade if "fabric" in v["loaders"] and mc in v["game_versions"]), None)
    vt = next((v for v in visible if "fabric" in v["loaders"] and mc in v["game_versions"]), None)
    matrix.append({"minecraft": mc, "fabric_api": api, "java": game["javaVersion"]["majorVersion"],
                   "trade_cycling": slim(dep), "visible_traders": slim(vt), "manifest_url": release["url"]})
    fetch(game["downloads"]["client_mappings"]["url"], f"mappings-{mc}.txt")
    print(mc, api, game["javaVersion"]["majorVersion"], dep["version_number"] if dep else "no Modrinth release",
          vt["version_number"] if vt else "no VisibleTraders release", flush=True)
(ROOT / "matrix.json").write_text(json.dumps(matrix, ensure_ascii=False, indent=2), encoding="utf-8")
