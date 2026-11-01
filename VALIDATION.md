# Villager Trade Finder 1.9.1 validation

Validated on 2026-11-01. The maintained matrix contains 24 Minecraft versions: 1.20 through 1.21.11, plus 26.1, 26.1.1, 26.1.2, 26.2 and 26.3.

- All 24 Gradle builds passed, including the existing matching, configuration, potion, bilingual candidate and offer-response checks appropriate to each version.
- The packaged 26.2 mod passed real Fabric Loader / Mixin checks for client and dedicated-server environments, with and without VisibleTraders. The client-only mod was excluded on the dedicated server.
- New 26.2 checks exercised the transformed interaction handler, custom and empty merchant titles, misleading profession titles, synchronized entity professions, excluded professions, configuration return, menu replacement, expired interaction, disconnect and world changes.
- The complete-response regression used a custom-named toolsmith and real vanilla / VisibleTraders handlers in three packet orders. A delayed Unbreaking III match stopped without another refresh request.
- All 43 class files in the delivered 26.2 JAR are identical to those loaded by the packaged regression checks.
- Every installer, source archive and installation bundle has a SHA-256 checksum. The version manifest records the per-version installer and source hashes.

These are automated build and headless runtime checks. No manual test in a real singleplayer save or multiplayer world was performed. The new entity-association runtime scenarios were executed on 26.2; the other maintained versions passed compilation, remapping and their existing regressions.

The documentation and publishing revision does not change the installer, source-archive or bundle bytes. Historical releases remain available.
