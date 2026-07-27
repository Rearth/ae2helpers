package rearth.ae2helpers.util;

import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

// Resolves a redstone link to the target provider logic and pings it when the linked import bus imports.
public final class RedstoneLinkNotifier {

    private RedstoneLinkNotifier() {
    }

    public static void notify(ServerLevel busLevel, ProviderLink link) {
        if (!busLevel.dimension().equals(link.pos().dimension())) return;

        var pos = link.blockPos();
        if (!busLevel.isLoaded(pos)) return;

        var be = busLevel.getBlockEntity(pos);
        var logic = ae2helpers$resolveLogic(be, link.side());
        if (logic instanceof ILinkedImportTarget target) {
            target.ae2helpers$onLinkedImport();
        }
    }

    @Nullable
    private static Object ae2helpers$resolveLogic(BlockEntity be, @Nullable Direction side) {
        if (side == null && be instanceof PatternProviderLogicHost host) {
            return host.getLogic();
        }
        if (side != null && be instanceof CableBusBlockEntity cb && cb.getPart(side) instanceof PatternProviderLogicHost host) {
            return host.getLogic();
        }
        if (be != null && ModList.get().isLoaded("advanced_ae")) {
            return AdvancedAeLinkCompat.resolveLogic(be, side);
        }
        return null;
    }
}
