package rearth.ae2helpers.mixin.importcard.aae;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;

// SmallAdvPatternProviderMenu extends this menu, so injecting the shared constructor covers both.
@Mixin(AdvPatternProviderMenu.class)
public abstract class AdvPatternProviderMenuMixin extends AEBaseMenu {

    @Shadow @Final protected AdvPatternProviderLogic logic;

    public AdvPatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(
      method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;)V",
      at = @At("TAIL")
    )
    private void ae2helpers$initUpgrades(MenuType<?> menuType, int id, Inventory playerInventory, AdvPatternProviderLogicHost host, CallbackInfo ci) {
        if (this.logic instanceof IPatternProviderUpgradeHost upgradeHost) {
            ae2helpers$createUpgradeSlots(upgradeHost.ae2helpers$getUpgradeInventory());
        }
    }

    @Unique
    protected final void ae2helpers$createUpgradeSlots(IUpgradeInventory upgrades) {
        for (int i = 0; i < upgrades.size(); i++) {
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, i);
            slot.setNotDraggable();
            this.addSlot(slot, ae2helpers.IMPORT_UPGRADE);
        }
    }
}
