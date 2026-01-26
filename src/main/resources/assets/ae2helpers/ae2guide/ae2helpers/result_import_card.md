---
navigation:
  parent: ae2helpers/index.md
  title: Crafting Result Import Card
  icon: ae2helpers:result_import_card
categories:
- result import card
item_ids:
- ae2helpers:result_import_card
---

The **Crafting Result Import Card** is a specialized upgrade for the **Pattern Provider** that simplifies automation 
setups by removing the need for separate Import Buses (on blocks/machines that can't auto-output items, like a vanilla furnace).

This card allows the **Pattern Provider itself** to pull the finished items back from the adjacent machine, effectively 
combining the "Push" and "Pull" logic into a single block.

An upgrade slot has been added to the **Pattern Provider** where this card can be inserted.

# Configuration
Right-click the card item to open its settings menu:

*   **Import Crafting Results Only:**
    *   **Enabled (Default):** The card is "Smart". It tracks exactly what ingredients were pushed and calculates the expected output. It will *only* import those specific result items. It ignores other items in the machine.
    *   **Disabled:** The card acts like a standard Import Bus. It will aggressively pull *any* item from the target machine into the network.
    *   Note that enabling this is much more performance-friendly, as it doesn't need to permanently try and import from the machine.

*   **Sync with Crafting CPU:**
    *   **Enabled (Default):** The card communicates with the Crafting CPU managing the job. If you cancel the crafting job at a terminal, the card stops waiting for the results. This prevents the provider from getting stuck "waiting" for items that will never arrive.

*   **Import Side:**
    *   **Auto:** Imports from the side of the machine that is touching the Pattern Provider.
    *   **Specific Side (Up, Down, North, etc.):** Forces the provider to pull from a specific side of the adjacent machine. Useful for machines that only output to specific sides (e.g., Sided Furnaces).


The card uses an "exponential backoff" algorithm. If the machine is slow, the card checks less frequently to save server performance. It wakes up immediately when a new craft begins.

### Limitations
*   **Sided Providers Only:** This can can work with both the full and the "panel" / "part" version of the pattern provider. However, the full-block provider needs to point to a specific side (can be done with a wrench).