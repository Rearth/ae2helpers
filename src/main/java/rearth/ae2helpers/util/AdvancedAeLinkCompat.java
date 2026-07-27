package rearth.ae2helpers.util;

import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import org.jetbrains.annotations.Nullable;

// Only referenced when "advanced_ae" is loaded (guarded in the callers).
public final class AdvancedAeLinkCompat {

    private AdvancedAeLinkCompat() {
    }

    @Nullable
    public static ProviderLink makeLink(BlockEntity be, Level level, Vec3 hit) {
        if (be instanceof AdvPatternProviderLogicHost) {
            return new ProviderLink(GlobalPos.of(level.dimension(), be.getBlockPos()), null);
        }
        if (be instanceof CableBusBlockEntity cb) {
            var selected = cb.selectPartWorld(hit);
            if (selected.part instanceof AdvPatternProviderLogicHost) {
                return new ProviderLink(GlobalPos.of(level.dimension(), be.getBlockPos()), selected.side);
            }
        }
        return null;
    }

    @Nullable
    public static Object resolveLogic(BlockEntity be, @Nullable Direction side) {
        if (side == null && be instanceof AdvPatternProviderLogicHost host) {
            return host.getLogic();
        }
        if (side != null && be instanceof CableBusBlockEntity cb && cb.getPart(side) instanceof AdvPatternProviderLogicHost host) {
            return host.getLogic();
        }
        return null;
    }
}
