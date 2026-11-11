## Pending import reliability and simpler card settings

- Notify AE2 when the card imports results so "lock until result" providers unlock after the full primary output returns.
- Track full pattern outputs independently of CPU request amounts; keep unfinished imports alive while a CPU awaits them, then expire them after 90 seconds without activity.
- Shorten card tooltips and wrap them at up to 300 pixels instead of vanilla's 170 pixels.
- Restart inactivity timeouts after loading and keep the 10-second surplus-draining timeout after the full output arrives.
- Remove CPU-sync cleanup and its configuration option; existing cards retain their import mode and machine face.
- Explain the remaining import filter and machine output face settings with concrete examples.
- Reject automatic crafting-grid fills when the terminal is disconnected or its grid node is inactive.

## Ghost and card screen regressions

- Retain the locator sent by AE2's network menu decoder on client menus, restoring per-terminal ghost lookup.
- Clear pending ghosts through both crafting-grid clear-button callbacks while retaining them across ordinary screen changes.
- Query matching server-side CPU jobs once per second while waiting; expire inactive ghosts after 10 seconds, with 30 seconds for unobserved startup requests.
- Reset cleanup timers when network status is unavailable and ignore status replies from older recipe generations or menus.
- Remove the duplicate background draw from the import card configuration screen to prevent the blur crash.
- Add real menu-decoder, cleanup and packet regression tests, plus an optional client renderer smoke test.

## Import card fixes from [PR #8](https://github.com/Rearth/ae2helpers/pull/8)

- Support undirected pattern provider blocks by checking every target side with cached import strategies.
- Share one operation budget across sides and rotate the first side to avoid starving adjacent machines.
- Track outputs at AE2's successful-pattern hook, including results returned through the provider itself.
- Preserve pending results before CPU registration and across world reloads.
- Drain surplus of tracked output types until each has been idle for 200 ticks, without capping amounts to CPU requests.
- Persist pending counts and fulfilled filters still draining surplus; restart idle grace periods after loading.
- Update tooltips and the guide, add regression tests, and isolate the game test run directory.

## Pending crafting ghosts

- Preserve pending ingredients per terminal and dimension across screen changes until disconnecting.
- Allow left- or right-clicking a ghost to remove it without cancelling the AE2 crafting job.
- Show the ingredient name, pending status, and click hint when hovering a ghost.
- Replace pending slots only for the terminal receiving a new recipe transfer.

## Minecraft 26.1.2 migration

- Update to Java 25, NeoForge 26.1.2.126, ModDevGradle 2.0.148, and AE2 26.1.13-beta.
- Remove obsolete Parchment mappings, the 1.21.1 EMI dependency, and legacy IntelliJ project files.
- Port GUI extraction, tooltips, recipes, item definitions, networking, and provider persistence.
- Restrict result-only imports to outstanding result amounts and wake providers when their card changes.
- Support configuring cards in either hand and validate crafting transfers against the current menu and crafting grid.
- Retry pending ingredient transfers until the server fills the slot; clear old requests on recipe changes.
- Keep client mixins off dedicated servers and avoid overlapping standard third-party upgrade panels.
- Add optional Minecraft integration tests and update CI to Java 25.

## Previous changes

- Allow card to be sneak-clicked into pattern providers.
- Add compatibility with me soul card mod.
- Add PT_BR localization (@PrincessStellar).
- Fix class loading issues on dedicated servers.
