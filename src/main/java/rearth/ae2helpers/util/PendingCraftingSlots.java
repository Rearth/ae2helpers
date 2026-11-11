package rearth.ae2helpers.util;

import appeng.menu.locator.MenuHostLocator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Session-local pending recipes, addressed by AE2's terminal location and dimension. */
public final class PendingCraftingSlots {
    public record Terminal(ResourceKey<Level> dimension, MenuHostLocator locator) {}

    public static final class PendingRecipe {
        private final Map<Integer, Ingredient> slots = new HashMap<>();
        private final int revision;
        private final Map<Integer, Long> inactiveSince = new HashMap<>();
        private final Set<Integer> observedCrafting = new HashSet<>();
        private int retryDelay = 15;
        private int statusDelay;

        private PendingRecipe(int revision) {
            this.revision = revision;
        }

        public int revision() {
            return revision;
        }

        public boolean shouldQueryStatus() {
            if (statusDelay > 0) {
                statusDelay--;
                return false;
            }
            statusDelay = 20;
            return true;
        }

        public void updateCraftingStatus(boolean known, int activeSlots, long gameTime) {
            if (!known) {
                inactiveSince.clear();
                return;
            }
            slots.keySet().removeIf(slot -> {
                if ((activeSlots & (1 << slot)) != 0) {
                    observedCrafting.add(slot);
                    inactiveSince.remove(slot);
                    return false;
                }
                long since = inactiveSince.computeIfAbsent(slot, ignored -> gameTime);
                // Allow a longer startup grace period when a craft has not been observed yet.
                long grace = observedCrafting.contains(slot) ? 200 : 600;
                return gameTime - since >= grace;
            });
            inactiveSince.keySet().retainAll(slots.keySet());
            observedCrafting.retainAll(slots.keySet());
        }

        public Map<Integer, Ingredient> slots() {
            return slots;
        }

        public boolean readyToCheck() {
            if (retryDelay > 0) {
                retryDelay--;
                return false;
            }
            return true;
        }

        public void delayRetry() {
            retryDelay = 10;
        }
    }

    private final Map<Terminal, PendingRecipe> terminals = new HashMap<>();
    private int nextRevision;

    public PendingRecipe get(Terminal terminal) {
        return terminals.get(terminal);
    }

    public void replace(Terminal terminal, Map<Integer, Ingredient> recipe, Set<Integer> waitingSlots) {
        clear(terminal);
        if (terminal == null) return;
        var pending = new PendingRecipe(++nextRevision);
        for (int slot : waitingSlots) {
            var ingredient = recipe.get(slot);
            if (slot >= 0 && slot < 9 && ingredient != null) {
                pending.slots.put(slot, ingredient);
            }
        }
        if (!pending.slots.isEmpty()) terminals.put(terminal, pending);
    }

    public boolean remove(Terminal terminal, int slot) {
        var pending = get(terminal);
        if (pending == null || pending.slots.remove(slot) == null) return false;
        pending.inactiveSince.remove(slot);
        pending.observedCrafting.remove(slot);
        if (pending.slots.isEmpty()) clear(terminal);
        return true;
    }

    public void clear(Terminal terminal) {
        terminals.remove(terminal);
    }

    public void clearAll() {
        terminals.clear();
    }
}
