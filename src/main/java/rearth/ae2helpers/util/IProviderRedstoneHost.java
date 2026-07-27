package rearth.ae2helpers.util;

// Implemented by the pattern provider logic (via mixin) so the block/part can query whether the
// redstone card is installed and one of this provider's patterns is currently being crafted.
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public interface IProviderRedstoneHost {
    boolean ae2helpers$isEmittingRedstone();

    // true when emitting AND the installed redstone card is configured for a strong signal
    boolean ae2helpers$isEmittingStrongRedstone();

    // the block face the signal is restricted to, or null for all sides
    @Nullable Direction ae2helpers$getRedstoneSide();

    // the configured redstone power level (1-15) to emit while active
    int ae2helpers$getRedstoneSignalStrength();
}
