package rearth.ae2helpers.mixin;

import appeng.api.upgrades.Upgrades;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.parts.crafting.PatternProviderPart;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = PatternProviderScreen.class)
public abstract class PatternProviderScreenMixin extends AEBaseScreen<PatternProviderMenu> {
    
    public PatternProviderScreenMixin(PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void initUpgradePanel(PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        
        // could be this but that breaks with extendedae
//        this.widgets.add("upgrades", new UpgradesPanel(
//          menu.getSlots(SlotSemantics.UPGRADE),
//          this::ae2helpers$getCompatibleUpgrades
//        ));
        
        var existingStyle = style.getWidget("upgrades");
        
        // inject new style, similar to existing upgrades
        if (style instanceof ScreenStyleAccessor accessor) {
            
            WidgetStyle upgradeStyle = existingStyle;
            if (ModList.get().isLoaded("appflux")) {
                upgradeStyle = new WidgetStyle();
                upgradeStyle.setLeft(existingStyle.getLeft());
                upgradeStyle.setRight(existingStyle.getRight());
                upgradeStyle.setTop(existingStyle.getTop() + existingStyle.getHeight() + 32);
                upgradeStyle.setHeight(existingStyle.getHeight());
                upgradeStyle.setWidth(existingStyle.getWidth());
                upgradeStyle.setHideEdge(existingStyle.isHideEdge());
            }
            
            accessor.ae2helpers$getWidgets().put("importupgrades", upgradeStyle);
        }
        
        this.widgets.add("importupgrades", new UpgradesPanel(
          menu.getSlots(ae2helpers.IMPORT_UPGRADE),
          this::ae2helpers$getCompatibleUpgrades
        ));
        
    }
    
    @Unique
    private List<Component> ae2helpers$getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        
        IPatternProviderUpgradeHost upgradeHost;
        if (this.getMenu().getTarget() instanceof PatternProviderPart part && part.getLogic() instanceof IPatternProviderUpgradeHost upgradeableObject) {
            upgradeHost = upgradeableObject;
        } else if (this.getMenu().getTarget() instanceof PatternProviderBlockEntity part && part.getLogic() instanceof IPatternProviderUpgradeHost upgradeableObject) {
            upgradeHost = upgradeableObject;
        } else {
            return list;
        }
        
        var inventory = upgradeHost.ae2helpers$getUpgradeInventory();
        list.addAll(Upgrades.getTooltipLinesForMachine(inventory.getUpgradableItem()));
        
        return list;
    }
}