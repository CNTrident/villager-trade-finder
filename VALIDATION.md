# Villager Trade Finder validation

## 1.9.3 · All 24 Minecraft versions

Validated on 2026-11-20. The [version manifest](release-manifests/1.9.3.json) lists 24 Minecraft versions and the SHA-256 of each installer and source JAR.

- All 24 Gradle builds passed, including each version’s matching and offer-state checks. The matcher and offer-state checks cover repeated quotes in different rounds, stale numbered replies, incorrect menu IDs, missing confirmation, and both orders of initial and locked data.
- On Minecraft 26.3, three integrated-server runs found the same Mending targets after 60, 17, and 58 requests. Summed time from starting automation to observing the hit fell from 15.212 s in 1.9.2 to 6.861 s in 1.9.3. The observed initial offer packet count fell from 405 to 135. The test also checked rapid restart, no additional request after success, and the trade after reopening.
- The same 26.3 installer ran on both sides of a separate Fabric server connection over loopback TCP. The client completed 154 requests with 154 initial offer responses. The Mending trade remained after the UI reopened and no additional refresh followed the hit.
- The 26.2 real-handler regression passed with and without VisibleTraders. It covers both the compatibility path and the numbered path across initial, later, and all scopes. The test verifies that a stale numbered reply cannot replace the displayed offer, and that an initial reply cannot stand in for missing locked data.
- A 26.3 integrated-server run with VisibleTraders compared each later-trade request, server refresh, complete future payload, and client filter evaluation. Both the numbered and compatibility paths checked all 113 rounds and kept the matching Mending offer at the end. The 113 rounds took 6.765 s with numbered replies and 12.468 s with the compatibility path.
- The 19 legacy installers passed archive-entry comparison against 1.9.2 with the sole content change in the Fabric version field. All five rebuilt 26.x installer archives matched the previously verified installer contents. The published 26.3 installer retains the exact bytes used by the runtime tests.

The gameplay tests used isolated 26.3 worlds and the packaged JAR. The TCP test used loopback. The 26.3 compatibility audit used the integrated server with numbered replies disabled in the test harness. The separate 26.3 TCP compatibility attempt ended before automation because of a native Windows exit. WAN timing and legacy-version gameplay were outside these measurements.

## 1.9.2 · All 24 Minecraft versions

Validated on 2026-11-20. The [version manifest](release-manifests/1.9.2.json) lists 24 Minecraft versions and the SHA-256 of every installer and source JAR.

- All 24 Gradle builds passed, including the matching, configuration, and offer-response checks. The shared response checks cover duplicate packets in a burst, a burst split across client ticks, rapid catch-up ticks, incorrect menu IDs, and the ordering of initial and locked offers.
- A real Fabric 26.3 client and integrated server ran the final installer with Trade Cycling 1.0.23+26.3 and Fabric API 0.163.0+26.3. Three Mending runs made 135 refresh requests and received 405 offer packets. The client sent zero additional requests after a hit. The villager kept the matching trade while the menu remained open and after reopening it.
- A 26.3 start, stop, and restart in the same client tick left exactly one request in flight. The restarted run accepted its settled reply and retained the matching bookshelf trade.
- The same 26.3 installer also passed on a separate Fabric dedicated server over loopback TCP. Only Trade Cycling, Fabric API, and a fixture that spawns the test villager were on the server. The client made 66 refresh requests, received 198 offer packets, sent zero requests after the Mending hit, and retained the trade after reopening.
- Fabric/Mixin checks on 26.2 passed with and without VisibleTraders on the client and dedicated server. The real packet handlers passed the initial, locked, and all-offer scopes, including a three-packet initial burst, the locked-offer clear marker, both packet orders, and the same-menu restart. The dedicated server excluded the client-only installer.

The 26.3 gameplay checks used temporary singleplayer and dedicated-server worlds. The TCP test used loopback, with no WAN latency simulation. The other versions were built and checked automatically. The local 26.3 installer is byte-identical to the installer tested in the game.

Temporary diagnostics used the system clock to test the 50 ms quiet window without slowing or intercepting the game's refresh requests. The packet format does not provide a request identifier.

The earlier 1.9.1 results remain in the history for [v1.9.1](https://github.com/CNTrident/villager-trade-finder/tree/v1.9.1).
