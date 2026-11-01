# Villager Trade Finder maintenance

This repository is a Fabric client mod for the 24 Minecraft versions in mc26-matrix.json and backports/matrix.json. Build with Java 25; legacy artifacts target the matrix's Java 17 or 21 runtime. The mod integrates with Trade Cycling and optionally VisibleTraders. It has no server entrypoint or server mixins.

Use gradlew build for the default 26.2 build, gradlew -p backports build -Pmc=<version> for a maintained legacy version, and python scripts/build_all_versions.py for all 24. Each build runs real item/filter regression checks and packet-state checks. The opt-in 26.2 smoke harness runs actual packet handlers without opening a world or window.

One refresh must finish generation, receive its complete required response, and evaluate targets before the next request. VisibleTraders Optional.empty is a clearing/loading marker, not completion. Modern versions need both initial and complete locked offers; legacy versions receive both scopes in one merchant packet. Stop on a match before sending any further refresh. The safety limit is 1000000. Keep vanilla server scheduling unchanged.

Preserve profession-specific targets, AND/OR, ON/OFF/AUTO, exact enchantment sets and levels, base prices and potion criteria. Keep user-facing strings in the existing English/Chinese language resources. Keep version declarations, docs and artifact names synchronized. Do not commit caches, worlds, credentials, local logs or unrelated workspace projects.
