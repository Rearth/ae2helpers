package rearth.ae2helpers.util;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Tracks outputs from accepted patterns independently of CPU demand. */
public final class CraftingResultTracker {
    public static final long SURPLUS_IDLE_TIMEOUT = 20 * 10;
    public static final long PENDING_IDLE_TIMEOUT = 20 * 90;
    private static final String NBT_KEY = "ae2helpers_expected_results";

    private static final class PendingResult {
        long remaining;
        long lastActivity = -1;
        boolean requestedLastCheck;
    }

    private final Map<AEKey, PendingResult> results = new HashMap<>();

    public Set<AEKey> keys() {
        return results.keySet();
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public long remaining(AEKey key) {
        var result = results.get(key);
        return result == null ? 0 : result.remaining;
    }

    public void expect(AEKey key, long amount, long gameTime) {
        if (amount <= 0) return;
        var result = results.computeIfAbsent(key, ignored -> new PendingResult());
        result.remaining = amount > Long.MAX_VALUE - result.remaining ? Long.MAX_VALUE : result.remaining + amount;
        result.lastActivity = gameTime;
    }

    public boolean imported(AEKey key, long amount, long gameTime) {
        var result = results.get(key);
        if (result == null || amount <= 0) return false;
        result.remaining = Math.max(0, result.remaining - amount);
        result.lastActivity = gameTime;
        // Retain fulfilled keys until the machine has been quiet, allowing surplus to drain.
        return true;
    }

    public boolean cleanup(long gameTime, Predicate<AEKey> isRequested) {
        boolean changed = false;
        var iterator = results.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var result = entry.getValue();
            // Start a fresh grace period after loading instead of counting offline time as idle.
            if (result.lastActivity < 0) result.lastActivity = gameTime;
            boolean requested = result.remaining > 0 && isRequested.test(entry.getKey());
            // Keep slow crafts alive, then grant the full idle timeout when CPU demand disappears.
            if (result.remaining > 0 && (requested || result.requestedLastCheck)) result.lastActivity = gameTime;
            result.requestedLastCheck = requested;
            if (requested) continue;
            long timeout = result.remaining == 0 ? SURPLUS_IDLE_TIMEOUT : PENDING_IDLE_TIMEOUT;
            if (gameTime - result.lastActivity >= timeout) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        results.clear();
    }

    public void write(ValueOutput output) {
        var list = output.childrenList(NBT_KEY);
        results.forEach((key, result) -> {
            var child = list.addChild();
            GenericStack.writeTag(child, new GenericStack(key, result.remaining));
        });
    }

    public void read(ValueInput input) {
        clear();
        for (var child : input.childrenListOrEmpty(NBT_KEY)) {
            var stack = GenericStack.readTag(child);
            if (stack == null || stack.amount() < 0) continue;
            var result = new PendingResult();
            result.remaining = stack.amount();
            results.put(stack.what(), result);
        }
    }
}
