package rearth.ae2helpers.mixin.redstone;

import appeng.api.parts.IPart;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.AEBasePart;
import org.spongepowered.asm.mixin.Mixin;
import rearth.ae2helpers.util.IProviderRedstoneHost;

// Shared across all AE2-based pattern provider parts (AE2, ExtendedAE, ExpandedAE), which all extend
// AEBasePart and implement PatternProviderLogicHost. Emits from the part's mounted side only.
@Mixin(AEBasePart.class)
public abstract class PatternProviderRedstonePartMixin implements IPart {

    @Override
    public int isProvidingWeakPower() {
        if (this instanceof PatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone
              && redstone.ae2helpers$isEmittingRedstone()) {
            return 15;
        }
        return 0;
    }

    @Override
    public int isProvidingStrongPower() {
        if (this instanceof PatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone
              && redstone.ae2helpers$isEmittingStrongRedstone()) {
            return 15;
        }
        return 0;
    }
}
