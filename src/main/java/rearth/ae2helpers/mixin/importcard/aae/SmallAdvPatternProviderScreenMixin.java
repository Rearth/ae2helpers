package rearth.ae2helpers.mixin.importcard.aae;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.pedroksl.advanced_ae.client.gui.SmallAdvPatternProviderScreen;
import net.pedroksl.advanced_ae.gui.advpatternprovider.SmallAdvPatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.client.AdvProviderUpgradePanel;

@Mixin(SmallAdvPatternProviderScreen.class)
public abstract class SmallAdvPatternProviderScreenMixin extends AEBaseScreen<SmallAdvPatternProviderMenu> {

    public SmallAdvPatternProviderScreenMixin(SmallAdvPatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2helpers$initUpgradePanel(SmallAdvPatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        AdvProviderUpgradePanel.install(style, this.widgets, menu);
    }
}
