package rearth.ae2helpers.mixin.redstone.aae;

import appeng.api.parts.IPart;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import net.pedroksl.advanced_ae.common.parts.AdvPatternProviderPart;
import net.pedroksl.advanced_ae.common.parts.SmallAdvPatternProviderPart;
import org.spongepowered.asm.mixin.Mixin;
import rearth.ae2helpers.util.IProviderRedstoneHost;

@Mixin({AdvPatternProviderPart.class, SmallAdvPatternProviderPart.class})
public abstract class AdvPatternProviderRedstonePartMixin implements IPart {

    @Override
    public int isProvidingWeakPower() {
        if (this instanceof AdvPatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone
              && redstone.ae2helpers$isEmittingRedstone()) {
            return 15;
        }
        return 0;
    }

    @Override
    public int isProvidingStrongPower() {
        if (this instanceof AdvPatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone
              && redstone.ae2helpers$isEmittingStrongRedstone()) {
            return 15;
        }
        return 0;
    }
}
