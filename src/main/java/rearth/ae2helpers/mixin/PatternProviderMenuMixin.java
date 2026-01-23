package rearth.ae2helpers.mixin;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;

@Mixin(PatternProviderMenu.class)
public abstract class PatternProviderMenuMixin extends AEBaseMenu {
    
    @Shadow
    @Final
    protected PatternProviderLogic logic;
    
    public PatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }
    
    @Inject(
      method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/patternprovider/PatternProviderLogicHost;)V",
      at = @At("TAIL")
    )
    private void initUpgrades(MenuType<?> menuType, int id, Inventory playerInventory, PatternProviderLogicHost host, CallbackInfo ci) {
        if (this.logic instanceof IPatternProviderUpgradeHost upgradeHost) {
            
            
            ae2helpers$createUpgradeSlots(upgradeHost.ae2helpers$getUpgradeInventory());
            
            // could be this but that breaks with extendedae
            // this.setupUpgrades(upgradeHost.ae2helpers$getUpgradeInventory());
            
            
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