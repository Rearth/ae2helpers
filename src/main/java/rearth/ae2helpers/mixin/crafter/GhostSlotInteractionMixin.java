package rearth.ae2helpers.mixin.crafter;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.ae2helpers.client.AutoCraftingWatcher;

@Mixin(AEBaseScreen.class)
public abstract class GhostSlotInteractionMixin {
    @Unique private int ae2helpers$ghostClickButton = -1;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ae2helpers$cancelGhost(MouseButtonEvent event, boolean doubleClick,
                                       CallbackInfoReturnable<Boolean> cir) {
        ae2helpers$ghostClickButton = -1;
        if ((event.button() == 0 || event.button() == 1)
                && (Object) this instanceof MEStorageScreen<?> screen
                && AutoCraftingWatcher.INSTANCE.cancelGhost(screen, event.x(), event.y())) {
            ae2helpers$ghostClickButton = event.button();
            cir.setReturnValue(true);
        }
    }

    // Consume the whole click, including its release, so cancelling while carrying
    // an item cannot place that item into the newly cleared ghost slot.
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void ae2helpers$consumeGhostRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ae2helpers$ghostClickButton == event.button()) {
            ae2helpers$ghostClickButton = -1;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void ae2helpers$consumeGhostDrag(MouseButtonEvent event, double deltaX, double deltaY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (ae2helpers$ghostClickButton == event.button()) cir.setReturnValue(true);
    }
}
