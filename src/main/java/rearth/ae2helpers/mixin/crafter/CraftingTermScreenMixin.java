package rearth.ae2helpers.mixin.crafter;

import appeng.api.config.ActionItems;
import appeng.client.gui.me.items.CraftingTermScreen;
import appeng.client.gui.widgets.ActionButton;
import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rearth.ae2helpers.client.AutoCraftingWatcher;

import java.util.function.Consumer;

@Mixin(CraftingTermScreen.class)
public abstract class CraftingTermScreenMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW",
            target = "(Lappeng/api/config/ActionItems;Ljava/util/function/Consumer;)Lappeng/client/gui/widgets/ActionButton;"))
    private ActionButton ae2helpers$clearPendingSlots(ActionItems action, Consumer<ActionItems> callback,
                                                     Operation<ActionButton> original,
                                                     @Local(argsOnly = true) CraftingTermMenu menu) {
        return original.call(action, (Consumer<ActionItems>) clicked -> {
            AutoCraftingWatcher.INSTANCE.clear(menu);
            callback.accept(clicked);
        });
    }
}
