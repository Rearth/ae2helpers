package rearth.ae2helpers.mixin.redstone;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.parts.automation.ImportBusPart;
import appeng.parts.automation.UpgradeablePart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.RedstoneLinkNotifier;

// When an import bus that holds a linked redstone link card actually imports, ping the linked provider so
// its redstone signal stays alive while the result trickles back (e.g. end of a Create chain).
@Mixin(ImportBusPart.class)
public abstract class ImportBusLinkMixin extends UpgradeablePart {

    public ImportBusLinkMixin(IPartItem<?> partItem) {
        super(partItem);
    }

    @Inject(method = "doBusWork", at = @At("RETURN"))
    private void ae2helpers$notifyLink(IGrid grid, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;

        var stack = ae2helpers$findLinkCard();
        if (stack == null) return;
        var link = stack.get(ae2helpers.REDSTONE_LINK.get());
        if (link == null) return;

        var be = this.getBlockEntity();
        if (be != null && be.getLevel() instanceof ServerLevel serverLevel) {
            RedstoneLinkNotifier.notify(serverLevel, link);
        }
    }

    @Unique
    private ItemStack ae2helpers$findLinkCard() {
        var upgrades = this.getUpgrades();
        for (int i = 0; i < upgrades.size(); i++) {
            var stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ae2helpers.REDSTONE_LINK_CARD.get())) return stack;
        }
        return null;
    }
}
