package rearth.ae2helpers.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// Must extend Button to satisfy addToLeftToolbar signature
public class AutoInsertButton extends IconButton {
    
    public AutoInsertButton(OnPress onPress) {
        super(onPress);
    }
    
    @Override
    public List<Component> getTooltipMessage() {
        var enabled = AutoCraftingWatcher.INSTANCE.isAutoInsertEnabled();
        
        var title = Component.translatable("ae2helpers.crafthelper.title");
        
        var status = enabled ? Component.translatable("ae2helpers.crafthelper.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("ae2helpers.crafthelper.disabled").withStyle(ChatFormatting.RED);
        
        var tooltip = Component.translatable("ae2helpers.crafthelper.tooltip").withStyle(ChatFormatting.GRAY);
        
        return List.of(title, status, tooltip);
    }
    
    @Override
    public @NotNull Component getMessage() {
        
        var enabled = AutoCraftingWatcher.INSTANCE.isAutoInsertEnabled();
        return enabled ? Component.translatable("ae2helpers.crafthelper.enabled") : Component.translatable("ae2helpers.crafthelper.disabled");
    }
    
    @Override
    protected Icon getIcon() {
        var enabled = AutoCraftingWatcher.INSTANCE.isAutoInsertEnabled();
        return enabled ? Icon.ACCESS_WRITE : Icon.ACCESS_READ;
    }
}
