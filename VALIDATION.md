# Villager Trade Finder 1.9.2 validation

Validated on 2026-11-20. The [version manifest](release-manifests/1.9.2.json) lists 24 Minecraft versions and the SHA-256 of every installer and source JAR.

- All 24 Gradle builds passed, including the matching, configuration, and offer-response checks. The shared response checks cover duplicate packets in a burst, a burst split across client ticks, rapid catch-up ticks, incorrect menu IDs, and the ordering of initial and locked offers.
- A real Fabric 26.3 client and integrated server ran the final installer with Trade Cycling 1.0.23+26.3 and Fabric API 0.163.0+26.3. Three Mending runs made 135 refresh requests and received 405 offer packets. The client sent zero additional requests after a hit. The villager kept the matching trade while the menu remained open and after reopening it.
- A 26.3 start, stop, and restart in the same client tick left exactly one request in flight. The restarted run accepted its settled reply and retained the matching bookshelf trade.
- The same 26.3 installer also passed on a separate Fabric dedicated server over loopback TCP. Only Trade Cycling, Fabric API, and a fixture that spawns the test villager were on the server. The client made 66 refresh requests, received 198 offer packets, sent zero requests after the Mending hit, and retained the trade after reopening.
- Fabric/Mixin checks on 26.2 passed with and without VisibleTraders on the client and dedicated server. The real packet handlers passed the initial, locked, and all-offer scopes, including a three-packet initial burst, the locked-offer clear marker, both packet orders, and the same-menu restart. The dedicated server excluded the client-only installer.

The 26.3 gameplay checks used temporary singleplayer and dedicated-server worlds. The TCP test used loopback, with no WAN latency simulation. The other versions were built and checked automatically. The local 26.3 installer is byte-identical to the installer tested in the game.

Temporary diagnostics used the system clock to test the 50 ms quiet window without slowing or intercepting the game's refresh requests. The packet format does not provide a request identifier.

The earlier 1.9.1 results remain in the history for [v1.9.1](https://github.com/CNTrident/villager-trade-finder/tree/v1.9.1).
