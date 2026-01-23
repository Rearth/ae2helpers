package rearth.ae2helpers.mixin;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ScreenStyle.class)
public interface ScreenStyleAccessor {
    
    @Accessor("widgets")
    Map<String, WidgetStyle> ae2helpers$getWidgets();
}