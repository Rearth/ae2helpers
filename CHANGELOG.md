## 1.0.2
- Add: Pattern Provider Redstone Card — the provider emits a redstone signal while one of its patterns is being crafted. Works on the block and the cable part (part emits from its attached side). Right-click the card in hand to configure: mode (while crafting / inverted / pulse on start & finish with configurable length), weak/strong signal, and which block face emits. Supported on AE2, ExtendedAE, ExpandedAE, Advanced AE and AppliedCreate providers.
- Add: Redstone Link Card — sneak-right-click a pattern provider to link it, then place the card in an import bus. When that bus imports (e.g. at the end of a Create chain), it keeps the linked provider's redstone signal alive. Lets the redstone card work when results return via a remote import bus instead of the import card or the provider's own return path.
- Fix: import card now pulls a machine's full overproduction — AE2 finishes a craft as soon as the request is delivered, so a pattern that yields more than requested left surplus behind; the card now keeps importing until the machine actually stops yielding.
- Fix: crafting results no longer get stuck in the machine — the pending result was dropped as soon as the crafting service reported 0, which happened before the crafting CPU had registered the job (and again on world load), stranding the output
- Fix: import card now works on pattern provider blocks left in the default "push to all sides" mode (previously it only worked when a single push direction was set, e.g. the cable part form)
- Add: Advanced AE pattern provider support (regular + small, block + part)
- Add: redstone card signal strength — configure the emitted power level (1-15) with a slider, so comparators / LittleBigRedstone can tell different sources apart
- Add: the redstone signal now lingers for 3s after crafting stops, so back-to-back crafts don't rapidly toggle it (each toggle is a neighbour update — this avoids TPS churn with many machines)
- Change: the import card's "Import Side" now selects which side of the provider to import from, independent of the push direction — pull results from a different side than the provider pushes to ("Auto" keeps importing from the push side(s))
- Fix: redstone signal could freeze on while a machine kept yielding results (and wouldn't reset when the redstone card was pulled but the import card stayed) — the emission is now recomputed every tick and on card changes

## 1.0.1
- Allow card to be sneak-clicked into pattern providers
- Add compatibility with me soul card mod
- Add PT_BR localization (@PrincessStellar)
- Fix class loading issues on dedicated servers
