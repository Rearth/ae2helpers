# AE2 Helpers

An Applied Energistics 2 addon for Minecraft **26.1.2**, NeoForge **26.1.2.126 or newer**, and Java **25**. This build targets AE2 **26.1.13-beta** on both client and server.

## Features

- **Automatic crafting ingredient insertion:** Request missing recipe ingredients through AE2's recipe transfer integration, then return to the crafting terminal. Pending ingredients appear as ghosts and fill as items become available. Ghosts survive closing or switching screens and are remembered separately for each terminal and dimension until disconnecting. Both crafting-grid clear buttons remove pending ghosts for that terminal. Once the matching CPU job stops, unfilled ghosts expire after a 10-second grace period; requests never observed running get 30 seconds for startup. A filled slot removes its ghost immediately. Hover a ghost for its status; left- or right-click it to stop auto-filling that slot without cancelling the AE2 crafting job. The toolbar button toggles this feature. Transfers retry until the server confirms a filled slot and are bound to the current menu ID.
- **Crafting Result Import Card:** Adds a dedicated slot to pattern providers, including provider parts. Sneak-click a provider with the card to install it, or insert it in the provider menu. Right-click a held card to configure it; both hands are supported.
- **Result-only import:** Tracks outputs of successful pattern pushes and imports those output types, including surplus. Fulfilled output types stop being checked after 10 seconds without imports. Optional CPU synchronization retires previously observed jobs after an idle grace period; it never caps imports to the CPU request count. Import-all mode also accepts untracked items.
- **Import side selection:** Uses the adjacent machine's facing side by default, with an optional side override. Full-block providers support the default all-sides mode as well as a selected push direction. Target sides share a transfer budget and are checked in a rotating order.

The card uses a separate upgrade inventory so other addons can keep their own provider upgrades. Its panel leaves room for upgrades exposed through AE2's standard upgrade slots. Third-party addon combinations still need in-game compatibility testing.

The import-card fixes are adapted from [PR #8](https://github.com/Rearth/ae2helpers/pull/8) for this 26.1.2 branch.

## Development

Open `build.gradle` as a Gradle project in IntelliJ and select JDK 25 for the Gradle JVM. Legacy `.iml`, `.ipr`, and `.iws` files are unnecessary and ignored; Gradle supplies the project model and run configurations.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

The jar is written to `build/libs/`. On Linux/macOS, use `./gradlew` instead.

Integration tests run inside Minecraft with the real AE2 implementation:

```powershell
.\gradlew.bat runGameTestServer -PgameTests
```

The test server uses a separate `run-gametest/` directory so it can run alongside the development client. Tests cover terminal reopening, per-terminal/dimension isolation and ghost cancellation, real network-menu locator decoding, stale ghost cleanup and status packets, configured-card and pending-result persistence, card drops, packet codecs, surplus draining, shared transfer budgets, all-sides providers, delayed batch outputs across reloads, CPU-registration and idle-cleanup edge cases, import-all behavior, and processing results imported by a provider through AE2's tick manager. Test sources and fixtures in `src/gametest/` are included only with `-PgameTests`; omit that property when building a release jar.

An optional client smoke test opens the card screens with background blur enabled and checks that the terminal UI mixins apply:

```powershell
.\gradlew.bat runClientSmoke -PclientTests
```

It runs in `run-clienttest/` and exits automatically after rendering both hand variants. The title-screen test uses default settings because item component registries are bound only after joining a world. Test sources in `src/clienttest/` are excluded from normal builds.

Local dependency source checkouts in `source_ref/` are ignored and are not build dependencies. JEI and Jade are included as development runtime dependencies for in-game testing. Their versions are configured in `gradle.properties`; neither is required by the published mod.
