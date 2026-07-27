package rearth.ae2helpers.util;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.pedroksl.advanced_ae.common.entities.AdvPatternProviderEntity;
import net.pedroksl.advanced_ae.common.parts.AdvPatternProviderPart;
import org.jetbrains.annotations.Nullable;

// Only referenced when the "advanced_ae" mod is loaded (guarded in ImportCardItem), so it is safe to
// hard-reference Advanced AE classes here.
public final class AdvancedAeCardCompat {

    private AdvancedAeCardCompat() {
    }

    @Nullable
    public static IUpgradeInventory getUpgradeInventory(BlockEntity te, Vec3 clickLocation) {
        if (te instanceof AdvPatternProviderEntity provider && provider.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
            return upgradeHost.ae2helpers$getUpgradeInventory();
        }
        if (te instanceof CableBusBlockEntity be
              && be.selectPartWorld(clickLocation).part instanceof AdvPatternProviderPart part
              && part.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
            return upgradeHost.ae2helpers$getUpgradeInventory();
        }
        return null;
    }
}
