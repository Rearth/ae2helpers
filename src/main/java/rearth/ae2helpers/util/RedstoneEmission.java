package rearth.ae2helpers.util;

import net.minecraft.core.Direction;

// Shared block getSignal/getDirectSignal logic: honors the emission state and the configured side.
public final class RedstoneEmission {

    private RedstoneEmission() {
    }

    public static int weak(IProviderRedstoneHost host, Direction queryDirection) {
        return host.ae2helpers$isEmittingRedstone() && matchesSide(host, queryDirection) ? host.ae2helpers$getRedstoneSignalStrength() : 0;
    }

    public static int strong(IProviderRedstoneHost host, Direction queryDirection) {
        return host.ae2helpers$isEmittingStrongRedstone() && matchesSide(host, queryDirection) ? host.ae2helpers$getRedstoneSignalStrength() : 0;
    }

    private static boolean matchesSide(IProviderRedstoneHost host, Direction queryDirection) {
        var side = host.ae2helpers$getRedstoneSide();
        return side == null || queryDirection.getOpposite() == side;
    }
}
