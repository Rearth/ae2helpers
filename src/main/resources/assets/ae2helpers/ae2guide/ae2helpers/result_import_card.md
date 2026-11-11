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

*   **Import Only Crafting Results:**
    *   **On (Default):** Pulls output types from crafts started by this provider and leaves unrelated items in the machine. It waits for the full output amount in each pattern, even if the CPU finishes earlier. Once that amount has arrived, it collects extra matching output until none has arrived for 10 seconds.
    *   **Off:** Pulls everything the machine allows, including ingredients. Use this when you want the provider to act like an Import Bus.
    *   Long-running processes stay pending while a crafting CPU is waiting for their output. Once no CPU needs that output type, it receives a fresh 90-second inactivity timeout. New pattern pushes or successful imports restart the timer. This stops abandoned imports after cancellation without timing out active CPU requests.
    *   Pending counts survive world reloads, with a fresh timeout after loading; offline time does not count. Cancelling a CPU job does not immediately clear them, because a machine may still be processing its inputs. Remove and reinsert the card to reset pending imports immediately.

*   **Machine Output Face:**
    *   **Auto:** Extracts from the machine face touching the Pattern Provider.
    *   **Specific Face (Up, Down, North, etc.):** Extracts through that face of each targeted machine. For example, choose Down for a furnace's output slot. This setting does not change which neighbours the provider targets.


The card uses an "exponential backoff" algorithm. If the machine is slow, the card checks less frequently to save server performance. It wakes up immediately when a new craft begins.

### Provider directions
*   **Provider blocks and parts:** Both are supported. A full block in the default "push to all sides" mode checks every target side; a directed provider checks its selected side. All sides share one transfer budget and are checked in a rotating order.
