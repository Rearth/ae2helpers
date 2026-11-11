---
navigation:
  parent: ae2helpers/index.md
  title: Automatic Recipe Item Insertion
  icon: minecraft:crafting_table
---

ae2helpers introduces a "Ghost Item" feature to the Crafting Terminal, allowing you to queue up a recipe even if the ingredients aren't ready yet.

When you attempt to move a recipe (via JEI/EMI/REI) into the Crafting Terminal but lack the necessary items:

1.  **Request the Autocraft:** Hold `CTRL` + Click the recipe transfer button (standard AE2 behavior) to request the missing items.
2.  **Wait & Watch:** Instead of leaving the slots empty, **Ghost Items** will appear in the grid with a small loading spinner.
3.  **Switch Screens Freely:** Pending slots are remembered for each terminal and dimension until you disconnect. Return to the same terminal to resume auto-filling.
4.  **Auto-Fill:** The moment your Crafting CPUs finish the job and the items enter the network, they are **automatically inserted** into the correct slots on the grid.

Hover a ghost to see its ingredient name and pending status. Left- or right-click it to remove that ghost and stop waiting for that slot. The underlying AE2 crafting job continues.

A ghost disappears as soon as its slot receives a real item. If the ingredient's CPU job finishes or is cancelled while the item is no longer available (for example, another craft consumed it), the unfilled ghost expires after a 10-second grace period. Requests never observed running receive a 30-second startup grace period. The server checks matching CPU output types; it cannot tell which consumer took a finished item. An unavailable network pauses this cleanup.

Both crafting-grid clear buttons also remove pending ghosts for the current terminal. Merely closing the terminal keeps them pending.

## Controls
This feature is enabled by default. If you wish to disable it:
*   Look for the **Import Access Icon** on the left toolbar of the Crafting Terminal.
*   Clicking this button toggles the Auto-Insert feature on or off.

***

## Who should use this?
Without this feature, crafting complex items requires you to open a recipe, request an autocraft, wait for it to finish, 
find the recipe again, and click move *again*.

With **ae2helpers**, you simply request the craft and wait in the terminal. 
The grid fills itself as soon as the items are ready, meaning you don't have to keep checking if the items are available.