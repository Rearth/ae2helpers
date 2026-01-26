package rearth.ae2helpers.mixin.crafter;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.client.AutoCraftingWatcher;
import rearth.ae2helpers.client.AutoInsertButton;

@Mixin(MEStorageScreen.class)
public abstract class MEStorageScreenMixin<C extends MEStorageMenu> {
    
    @Inject(method = "containerTick", at = @At("RETURN"))
    private void onContainerTick(CallbackInfo ci) {
        MEStorageScreen<?> screen = (MEStorageScreen<?>) (Object) this;
        AutoCraftingWatcher.INSTANCE.onTick(screen);
    }
    
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        System.out.println("Screen removed");
        AutoCraftingWatcher.INSTANCE.onScreenRemoved();
    }
    
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void onRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AutoCraftingWatcher.INSTANCE.renderGhosts(guiGraphics, slot);
    }
    
    @Inject(method = "<init>(Lappeng/menu/me/common/MEStorageMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;Lappeng/client/gui/style/ScreenStyle;)V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        
        var screen = (MEStorageScreen<?>) (Object) this;
        if (screen.getMenu() instanceof CraftingTermMenu) {
            ((AEBaseScreenAccessor) this).invokeAddToLeftToolbar(new AutoInsertButton(this::ae2helpers$onToggleAutoInsert));
        }
    }
    
    @Unique
    private void ae2helpers$onToggleAutoInsert(Button button) {
        AutoCraftingWatcher.INSTANCE.toggleAutoInsert();
    }
}