package rearth.ae2helpers.mixin.crafter;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AEBaseScreen.class)
public interface AEBaseScreenAccessor {
    
    @Invoker("addToLeftToolbar")
    <B extends Button> B invokeAddToLeftToolbar(B button);
    
    
}