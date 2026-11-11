package rearth.ae2helpers.mixin.crafter;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuHostLocator;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MenuTypeBuilder.class)
public abstract class MenuTypeBuilderMixin {
    @Inject(method = "fromNetwork", at = @At("RETURN"))
    private void ae2helpers$rememberClientLocator(CallbackInfoReturnable<AEBaseMenu> cir,
                                                 @Local MenuHostLocator locator) {
        // AE2 sends this locator to the client, but only stores it on the server menu.
        cir.getReturnValue().setLocator(locator);
    }
}
